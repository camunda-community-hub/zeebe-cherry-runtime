/* ******************************************************************** */
/*                                                                      */
/*  CherryHistoricFactory                                                 */
/*                                                                      */
/*  Collect and return historic information                                           */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import io.camunda.connector.api.error.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static io.camunda.cherry.definition.AbstractRunner.ExecutionStatusEnum;

@Service
public class HistoryFactory {

  Logger logger = LoggerFactory.getLogger(HistoryFactory.class.getName());

  @Autowired
  RunnerExecutionRepository runnerExecutionRepository;

  @Autowired
  HistoryPerformance historyPerformance;

  /**
   * get main statistics for the runner type in the last <delayStatInHour> period
   *
   * @param runnerType      type of runner
   * @param dateNow         dateNow to get a correct synchronization
   * @param periodStatistic Period of statistic
   * @return statistic object
   */
  public Statistic getStatistic(String runnerType,
                                LocalDateTime dateNow,
                                HistoryPerformance.PeriodStatistic periodStatistic) {
    Statistic statistic = new Statistic();

    HistoryPerformance.IntervalRule intervalRule = historyPerformance.getIntervalRuleByPeriod(periodStatistic);
    LocalDateTime dateThreshold = LocalDateTime.now();
    dateThreshold = dateThreshold.minusMinutes(intervalRule.intervalInMinutes * intervalRule.numberOfIntervals);

    List<Map<String, Object>> listStats = runnerExecutionRepository.selectStatusStats(runnerType, dateThreshold);
    for (Map<String, Object> recordStats : listStats) {
      Long recordNumber = Long.valueOf(recordStats.get("number").toString());
      try {
        ExecutionStatusEnum status = ExecutionStatusEnum.valueOf(recordStats.get("status").toString());
        switch (status) {
        case SUCCESS:
          statistic.executionsSucceeded += recordNumber;
          break;
        case FAIL:
          statistic.executionsFailed += recordNumber;
          break;
        case BPMNERROR:
          statistic.executionsBpmnErrors += recordNumber;
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + status);
        }
      } catch (Exception e) {
        // should not arrived here
        statistic.executionsFailed += recordNumber;
      }

    }
    statistic.executions = statistic.executionsSucceeded + statistic.executionsFailed + statistic.executionsBpmnErrors;
    return statistic;
  }

  public HistoryPerformance.Performance getPerformance(String runnerType,
                                                       LocalDateTime dateNow,
                                                       HistoryPerformance.PeriodStatistic periodStatistic) {
    return historyPerformance.getPerformance(runnerType, dateNow, periodStatistic);
  }

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  get information                                          */
  /*                                                          */
  /* -------------------------------------------------------- */
  public List<RunnerExecutionEntity> getExecutions(String runnerType,
                                                   LocalDateTime dateNow,
                                                   LocalDateTime dateThreshold,
                                                   int pageNumberInt, int rowsPerPageInt) {


    return runnerExecutionRepository.selectRunnerRecords(runnerType,
        dateThreshold, PageRequest.of(pageNumberInt, rowsPerPageInt));

  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Save                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * save the execution statistics
   *
   * @param executionTime instant of the execution
   * @param typeExecutor  type of executor
   * @param runnerType    name of runner
   * @param status        status of execution
   * @param errorMessage         if the execution get an error, provide it
   * @param durationInMs  duration of this execution
   */
  public void saveExecution(Instant executionTime,
                            RunnerExecutionEntity.TypeExecutor typeExecutor,
                            String runnerType,
                            ExecutionStatusEnum status,
                            String errorCode,
                            String errorMessage,
                            long durationInMs) {
    try {
      RunnerExecutionEntity runnerExecutionEntity = new RunnerExecutionEntity();
      runnerExecutionEntity.typeExecutor = typeExecutor;

      runnerExecutionEntity.executionMs = durationInMs;
      // Save it at the UTC mode, so we can display in any time zone after
      runnerExecutionEntity.executionTime = LocalDateTime.ofInstant(executionTime, ZoneOffset.UTC);

      runnerExecutionEntity.runnerType = runnerType;
      runnerExecutionEntity.status = status;
      if (errorCode != null) {
        runnerExecutionEntity.errorCode = errorCode;
        runnerExecutionEntity.errorExplanation = errorMessage;
      }

      runnerExecutionRepository.save(runnerExecutionEntity);
    } catch (Exception e) {
      logger.error("CherryHistoricFactory.saveExcution: failed " + e.getMessage() + " " + e.getCause());
    }
  }

  public static class Statistic {
    public long executions;
    public long executionsFailed;
    public long executionsSucceeded;
    public long executionsBpmnErrors;
  }

}
