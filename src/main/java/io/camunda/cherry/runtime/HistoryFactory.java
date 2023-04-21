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
import org.springframework.stereotype.Service;

import java.time.Instant;
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
   * @param periodStatistic Period of statistic
   * @return statistic object
   */
  public Statistic getStatistic(String runnerType,
                                Instant instantNow,
                                HistoryPerformance.PeriodStatistic periodStatistic) {
    Statistic statistic = new Statistic();

    HistoryPerformance.IntervalRule intervalRule = historyPerformance.getIntervalRuleByPeriod(periodStatistic);
    Instant dateThreshold = instantNow.minusMillis(
        intervalRule.intervalInMinutes * intervalRule.numberOfIntervals * 60L * 1000L);

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
                                                       Instant instantNow,
                                                       HistoryPerformance.PeriodStatistic periodStatistic) {
    return historyPerformance.getPerformance(runnerType, instantNow, periodStatistic);
  }

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  get information                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * save the execution statistics
   *
   * @param executionTime instant of the execution
   * @param typeExecutor  type of executor
   * @param runnerType    name of runner
   * @param status        status of execution
   * @param error         if the execution get an error, provide it
   * @param durationInMs  duration of this execution
   */
  public void saveExecution(Instant executionTime,
                            RunnerExecutionEntity.TypeExecutor typeExecutor,
                            String runnerType,
                            ExecutionStatusEnum status,
                            ConnectorException error,
                            long durationInMs) {
    try {
      RunnerExecutionEntity runnerExecutionEntity = new RunnerExecutionEntity();
      runnerExecutionEntity.typeExecutor = typeExecutor;

      runnerExecutionEntity.executionMs = durationInMs;
      runnerExecutionEntity.executionTime = executionTime;
      runnerExecutionEntity.runnerType = runnerType;
      runnerExecutionEntity.status = status;
      if (error != null) {
        runnerExecutionEntity.errorCode = error.getErrorCode();
        runnerExecutionEntity.errorExplanation = error.getMessage();
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
  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Save                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  public static class Interval {
    /**
     * Name is something like 16:00 / 16:15
     */
    public String slot;
    public long executions = 0;
    public long sumOfExecutionTime = 0;
    public long executionsSucceeded = 0;
    public long executionsFailed = 0;
    public long executionsBpmnErrors = 0;
    public long picTimeInMs = 0;
    public long averageTimeInMs = 0;

    public Interval(String slot) {
      this.slot = slot;
    }

  }

}
