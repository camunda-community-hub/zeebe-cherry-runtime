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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryPerformance {

  public static final String SLOT_FORMATTER = "%03dD%02d:%02d";
  public static final String HUMAN_DATE_FORMATER = "yyyy-MM-dd HH:mm";
  @Autowired
  RunnerExecutionRepository runnerExecutionRepository;

  Logger logger = LoggerFactory.getLogger(HistoryPerformance.class.getName());

  public LocalDateTime getInstantThresholdFromPeriod(LocalDateTime dateNow,
                                                     HistoryPerformance.PeriodStatistic periodStatistic) {
    HistoryPerformance.IntervalRule intervalRule = getIntervalRuleByPeriod(periodStatistic);

    return dateNow.minusMinutes((long) intervalRule.intervalInMinutes * intervalRule.numberOfIntervals);
  }

  /**
   * Get performance for a runnerType. Return a record per 15 minutes. Do not ask 1 year !
   *
   * @param runnerType      type of runner
   * @param dateNow         Reference time
   * @param periodStatistic period of statistics, to calculate the thresold and interval
   * @return performance object
   */
  public Performance getPerformance(String runnerType, LocalDateTime dateNow, PeriodStatistic periodStatistic) {
    Performance performance = new Performance();
    Map<String, Interval> mapInterval = new LinkedHashMap<>();

    LocalDateTime dateThreshold = getInstantByPeriod(dateNow, periodStatistic);
    IntervalRule intervalRule = getIntervalRuleByPeriod(periodStatistic);

    // --- populate all the map
    LocalDateTime indexTime = dateThreshold;

    // according the period, we determine theses parameters:
    // - the number of interval (example, FOURHOUR => 24 interval every 10 mn), 1Y =>365 interval
    // every 1 day)
    // - the time to move from one intervalle to the next one (FOURHOUR: + 10 mn)
    // - the rule to round a time, to find the intervalle

    // We want to keep at the end 24*4 interval

    for (int index = 0; index <= intervalRule.numberOfIntervals; index++) {
      String slotString = intervalRule.getSlotFromDate(indexTime);
      mapInterval.put(slotString, new Interval(slotString, indexTime));
      indexTime = indexTime.plusMinutes(intervalRule.intervalInMinutes);
    }

    // ---  now we can fetch and explode data
    List<RunnerExecutionEntity> listExecutions = runnerExecutionRepository.selectRunnerRecords(runnerType,
        dateThreshold, PageRequest.of(0, 10000));
    for (RunnerExecutionEntity runnerExecutionEntity : listExecutions) {
      LocalDateTime slotTime = runnerExecutionEntity.executionTime;
      String slotString = intervalRule.getSlotFromDate(slotTime);
      Interval interval = mapInterval.get(slotString);
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
      if (runnerExecutionEntity.executionMs > interval.peakTimeInMs)
        interval.peakTimeInMs = runnerExecutionEntity.executionMs;
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
      if (interval.peakTimeInMs > performance.peakTimeInMs)
        performance.peakTimeInMs = interval.peakTimeInMs;
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

  public LocalDateTime getInstantByPeriod(LocalDateTime reference, PeriodStatistic period) {
    return switch (period) {
      case FOURHOUR -> getInstantByDelay(reference, 4);
      case ONEDAY -> getInstantByDelay(reference, 24);
      case ONEWEEK -> getInstantByDelay(reference, 7 * 24);
      case ONEMONTH -> getInstantByDelay(reference, 30 * 24);
      case ONEYEAR -> getInstantByDelay(reference, 365 * 24);
    };
  }

  public LocalDateTime getInstantByDelay(LocalDateTime reference, int delayStatInHour) {
    return reference.minusHours(delayStatInHour);
  }

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Class definitions                                         */
  /*                                                          */
  /* -------------------------------------------------------- */

  public enum PeriodStatistic {
    FOURHOUR, ONEDAY, ONEWEEK, ONEMONTH, ONEYEAR
  }

  public static class Performance {
    public long peakTimeInMs;
    public long executions;
    public long averageTimeInMs;
    public List<Interval> listIntervals = new ArrayList<>();
  }

  public static class Interval {
    /**
     * Name is something like 16:00 / 16:15
     */
    public String slot;

    public String humanTimeSlot;
    public long executions = 0;
    public long sumOfExecutionTime = 0;
    public long executionsSucceeded = 0;
    public long executionsFailed = 0;
    public long executionsBpmnErrors = 0;
    public long peakTimeInMs = 0;
    public long averageTimeInMs = 0;

    public Interval(String slot, LocalDateTime slotTime) {
      this.slot = slot;
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HUMAN_DATE_FORMATER);
      this.humanTimeSlot = formatter.format(slotTime);
    }
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
