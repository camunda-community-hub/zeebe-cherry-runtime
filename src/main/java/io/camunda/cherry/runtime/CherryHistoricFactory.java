/* ******************************************************************** */
/*                                                                      */
/*  CherryHistoriqueFactory                                                 */
/*                                                                      */
/*  Collect and return historic information                                           */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import io.camunda.cherry.definition.AbstractRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class CherryHistoricFactory {

  Logger logger = LoggerFactory.getLogger(CherryHistoricFactory.class.getName());

  @Autowired
  RunnerExecutionRepository runnerExecutionRepository;

  @Component
  public static class Statistic {
    public long executions;
    public long failed;
    public long succeeded;
  }

  private final Random rand = new Random(System.currentTimeMillis());

  public static class Interval {
    /**
     * Name is something like 16:00 / 16:15
     */
    public String slot;
    public long executions = 0;
    public long sumOfExecutionTime = 0;
    public long executionsSuccess = 0;
    public long executionsFaileds = 0;
    public long executionsBpmnErrors = 0;
    public long picTimeInMs = 0;
    public long averageTimeInMs = 0;

    public Interval(String slot) {
      this.slot= slot;
    }

  }

  @Component
  public static class Performance {
    public long picTimeInMs;
    public long executions;
    public long averageTimeInMs;
    public List<Interval> listIntervals = new ArrayList<>();
  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  get information                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * get main statistics for the runner type in the last <delayStatInHour> period
   * @param runnerType type of runner
   * @param delayStatInHour delay in hour for the period (now-delayinhour < now)
   * @return statistic object
   */
  public Statistic getStatistic(String runnerType, int delayStatInHour) {
    Statistic statistic = new Statistic();

    Instant dateThreshold = getInstantByDelay(delayStatInHour);
    List<Map<String, Object>> listStats = runnerExecutionRepository.selectStatusStats(runnerType, dateThreshold);
    for (Map<String, Object> record : listStats) {
      Long recordNumber = Long.valueOf(record.get("number").toString());
      if (record.get("status")!=null && AbstractRunner.ExecutionStatusEnum.SUCCESS.toString().equalsIgnoreCase(record.get("status").toString()))
        statistic.succeeded += recordNumber;
      else
        statistic.failed += recordNumber;
    }
    statistic.executions = statistic.failed + statistic.succeeded;
    return statistic;
  }

  /**
   * Get performance for a runnerType. Return a record per 15 minutes. Do not ask 1 year !
   * @param runnerType type of runner
   * @param delayStatInHour delay in hour for the period (now-delayinhour < now)
   * @return performance object
   */
  public Performance getPerformance(String runnerType, int delayStatInHour) {
    Performance performance = new Performance();

    Instant dateThreshold = getInstantByDelay(delayStatInHour);

    Map<String, Interval> mapInterval = new LinkedHashMap<>();
    //--- populate all the map
    LocalDateTime indexTime = LocalDateTime.ofInstant(dateThreshold, ZoneOffset.UTC);
    indexTime = indexTime.minusNanos(indexTime.getNano());
    indexTime = indexTime.minusSeconds(indexTime.getSecond());
    indexTime = indexTime.minusMinutes(indexTime.getMinute()  % 15);
    indexTime = indexTime.plusMinutes(15);
    dateThreshold = indexTime.toInstant(ZoneOffset.UTC);

    for (int index = 0; index <= 24 * 4; index++) {
      String slotString = String.format("%3dD%02d:%02d", indexTime.getDayOfYear(), indexTime.getHour(),
          indexTime.getMinute());
      mapInterval.put(slotString, new Interval(slotString));
      indexTime=indexTime.plusMinutes(15);
    }

    //---  now we can fetch and explode data
    List<RunnerExecutionEntity> listExecutions = runnerExecutionRepository.selectRunnerRecords(runnerType,
        dateThreshold);
    for (RunnerExecutionEntity runnerExecutionEntity : listExecutions) {
      Instant executionTime = runnerExecutionEntity.executionTime;
      LocalDateTime slotTime = LocalDateTime.ofInstant(executionTime, ZoneOffset.UTC);

      // find the slot from the execution time
      // back the the previous quater
      slotTime = slotTime.minusNanos(slotTime.getNano());
      slotTime = slotTime.minusSeconds(slotTime.getSecond());
      slotTime = slotTime.minusMinutes(slotTime.getMinute()% 15);
      String slotString = String.format("%3dD%02d:%02d", slotTime.getDayOfYear(), slotTime.getHour(),
          slotTime.getMinute());
      Interval interval = mapInterval.get(slotString);
      if (interval == null) {
        // this must not arrive
        logger.error("Interval is not populated [" + slotString + "]");
        continue;
      }
      interval.executions++;
      interval.sumOfExecutionTime += runnerExecutionEntity.executionMs;
      switch (runnerExecutionEntity.status) {
      case SUCCESS -> interval.executionsSuccess++;
      case FAIL -> interval.executionsFaileds++;
      case BPMNERROR -> interval.executionsBpmnErrors++;
      }
      if (runnerExecutionEntity.executionMs > interval.picTimeInMs)
        interval.picTimeInMs = runnerExecutionEntity.executionMs;
    }

    // build the list and calculate average
    long sumTotalExecutionTimeInMs = 0;
    long sumTotalExecutions = 0;
    for (Interval interval : mapInterval.values()) {
      if (interval.executions > 0)
        interval.averageTimeInMs = interval.sumOfExecutionTime / interval.executions;
      performance.listIntervals.add(interval);

      sumTotalExecutionTimeInMs += interval.sumOfExecutionTime;
      sumTotalExecutions += interval.executions;
      if (interval.picTimeInMs > performance.picTimeInMs)
        performance.picTimeInMs = interval.picTimeInMs;
    }

    // global values
    if (sumTotalExecutions>0)
      performance.averageTimeInMs = sumTotalExecutionTimeInMs / sumTotalExecutions;

    return performance;
  }

  public Performance getEnginePerformance(int delayStatInHour) {
    return getPerformance("", delayStatInHour);
  }

  public Statistic getEngineStatistic(int delayStatInHour) {
    return getStatistic("", delayStatInHour);
  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Save                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * save the execution statistics
   * @param executionTime instant of the execution
   * @param runnerType name of runner
   * @param status status of execution
   * @param durationInMs duration of this execution
   */
  public void saveExecution(Instant executionTime, String runnerType, AbstractRunner.ExecutionStatusEnum status, long durationInMs) {
    try {
      RunnerExecutionEntity runnerExecutionEntity = new RunnerExecutionEntity();
      runnerExecutionEntity.executionMs = durationInMs;
      runnerExecutionEntity.executionTime = executionTime;
      runnerExecutionEntity.runnerType = runnerType;
      runnerExecutionEntity.status = status;
      try {
        runnerExecutionEntity.status = status;
      }catch(Exception e)
      {}

      runnerExecutionRepository.save(runnerExecutionEntity);
    } catch (Exception e) {
      logger.error("CherryHistoricFactory.saeExcution: failed " + e.getMessage() + " " + e.getCause());
    }
  }

  private Instant getInstantByDelay(int delayStatInHour) {
    return Instant.now().minusSeconds(delayStatInHour * 60 * 60);
  }

}
