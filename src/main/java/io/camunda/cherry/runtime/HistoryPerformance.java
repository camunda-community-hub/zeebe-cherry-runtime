/* ******************************************************************** */
/*                                                                      */
/*  CherryHistoryPerformance                                            */
/*                                                                      */
/*  From the history, return performance calculation                    */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryPerformance {

  public static final String SLOT_FORMATTER = "%03dD%02d:%02d";
  @Autowired
  RunnerExecutionRepository runnerExecutionRepository;
  Logger logger = LoggerFactory.getLogger(HistoryPerformance.class.getName());

  public Instant getInstantThresholdFromPeriod(Instant instantNow, HistoryPerformance.PeriodStatistic periodStatistic) {
    HistoryPerformance.IntervalRule intervalRule = getIntervalRuleByPeriod(periodStatistic);

    return instantNow.minusMillis(intervalRule.intervalInMinutes * intervalRule.numberOfIntervals * 60 * 1000);
  }

  /**
   * Get performance for a runnerType. Return a record per 15 minutes. Do not ask 1 year !
   *
   * @param runnerType      type of runner
   * @param instantNow      Reference time
   * @param periodStatistic period of statistics, to calculate the thresold and interval
   * @return performance object
   */
  public Performance getPerformance(String runnerType, Instant instantNow, PeriodStatistic periodStatistic) {
    Performance performance = new Performance();
    Map<String, HistoryFactory.Interval> mapInterval = new LinkedHashMap<>();

    Instant dateThreshold = getInstantByPeriod(instantNow, periodStatistic);
    IntervalRule intervalRule = getIntervalRuleByPeriod(periodStatistic);

    //--- populate all the map
    LocalDateTime indexTime = LocalDateTime.ofInstant(dateThreshold, ZoneOffset.UTC);

    // according the period, we determine theses parameters:
    // - the number of interval (example, FOURHOUR => 24 interval every 10 mn), 1Y =>365 interval every 1 day)
    // - the time to move from one intervalle to the next one (FOURHOUR: + 10 mn)
    // - the rule to round a time, to find the intervalle

    // We want to keep at the end 24*4 interval

    for (int index = 0; index <= intervalRule.numberOfIntervals; index++) {
      String slotString = intervalRule.getSlotFromDate(indexTime);
      mapInterval.put(slotString, new HistoryFactory.Interval(slotString));
      indexTime = indexTime.plusMinutes(intervalRule.intervalInMinutes);
    }

    //---  now we can fetch and explode data
    List<RunnerExecutionEntity> listExecutions = runnerExecutionRepository.selectRunnerRecords(runnerType,
        dateThreshold);
    for (RunnerExecutionEntity runnerExecutionEntity : listExecutions) {
      Instant executionTime = runnerExecutionEntity.executionTime;
      LocalDateTime slotTime = LocalDateTime.ofInstant(executionTime, ZoneOffset.UTC);
      String slotString = intervalRule.getSlotFromDate(slotTime);
      HistoryFactory.Interval interval = mapInterval.get(slotString);
      if (interval == null) {
        // this must not arrive
        logger.error("Interval is not populated [" + slotString + "]");
        continue;
      }
      interval.executions++;
      interval.sumOfExecutionTime += runnerExecutionEntity.executionMs;
      switch (runnerExecutionEntity.status) {
      case SUCCESS -> interval.executionsSucceeded++;
      case FAIL -> interval.executionsFailed++;
      case BPMNERROR -> interval.executionsBpmnErrors++;
      }
      if (runnerExecutionEntity.executionMs > interval.picTimeInMs)
        interval.picTimeInMs = runnerExecutionEntity.executionMs;
    }

    // build the list and calculate average
    long sumTotalExecutionTimeInMs = 0;
    long sumTotalExecutions = 0;
    for (HistoryFactory.Interval interval : mapInterval.values()) {
      if (interval.executions > 0)
        interval.averageTimeInMs = interval.sumOfExecutionTime / interval.executions;
      performance.listIntervals.add(interval);

      sumTotalExecutionTimeInMs += interval.sumOfExecutionTime;
      sumTotalExecutions += interval.executions;
      if (interval.picTimeInMs > performance.picTimeInMs)
        performance.picTimeInMs = interval.picTimeInMs;
    }

    // global values
    if (sumTotalExecutions > 0)
      performance.averageTimeInMs = sumTotalExecutionTimeInMs / sumTotalExecutions;

    return performance;
  }

  public IntervalRule getIntervalRuleByPeriod(PeriodStatistic periodStatistic) {
    IntervalRule intervalRule = new IntervalRule();
    intervalRule.periodStatistic = periodStatistic;

    switch (periodStatistic) {
    case FOURHOUR -> {
      intervalRule.numberOfIntervals = 24;
      intervalRule.intervalInMinutes = 10;
    }
    case ONEDAY -> {
      intervalRule.numberOfIntervals = 144;
      intervalRule.intervalInMinutes = 10;
    }
    case ONEWEEK -> {
      intervalRule.numberOfIntervals = 7 * 4;
      intervalRule.intervalInMinutes = 7 * 24 / (7 * 4) * 60;
    }
    case ONEMONTH -> {
      intervalRule.numberOfIntervals = 30;
      intervalRule.intervalInMinutes = 24 * 60;
    }
    case ONEYEAR -> {
      intervalRule.numberOfIntervals = 365;
      intervalRule.intervalInMinutes = 24 * 60;
    }
    }
    return intervalRule;
  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Interval calculation                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  public Instant getInstantByPeriod(Instant reference, PeriodStatistic period) {
    switch (period) {
    case FOURHOUR -> {
      return getInstantByDelay(reference, 4);
    }
    case ONEDAY -> {
      return getInstantByDelay(reference, 24);
    }
    case ONEWEEK -> {
      return getInstantByDelay(reference, 7 * 24);
    }
    case ONEMONTH -> {
      return getInstantByDelay(reference, 30 * 24);
    }
    case ONEYEAR -> {
      return getInstantByDelay(reference, 365 * 24);
    }
    }
    return getInstantByDelay(reference, 4);
  }

  public Instant getInstantByDelay(Instant reference, int delayStatInHour) {
    return reference.minusSeconds((long) delayStatInHour * 60 * 60);
  }

  public enum PeriodStatistic {FOURHOUR, ONEDAY, ONEWEEK, ONEMONTH, ONEYEAR}

  public static class Performance {
    public long picTimeInMs;
    public long executions;
    public long averageTimeInMs;
    public List<HistoryFactory.Interval> listIntervals = new ArrayList<>();
  }

  public class IntervalRule {
    public int numberOfIntervals;
    public int intervalInMinutes;
    PeriodStatistic periodStatistic;

    public String getSlotFromDate(LocalDateTime dateTime) {
      LocalDateTime result = dateTime;
      result = result.minusNanos(result.getNano());
      result = result.minusSeconds(result.getSecond());
      if (intervalInMinutes <= 60) {
        result = result.minusMinutes(result.getMinute() % intervalInMinutes);
      } else {
        // Round per hour
        result = result.minusMinutes(result.getMinute());
        int nbHours = intervalInMinutes / 60;
        result = result.minusHours(result.getHour() % nbHours);
      }

      return String.format(SLOT_FORMATTER, result.getDayOfYear(), result.getHour(), result.getMinute());

    }
  }

}
