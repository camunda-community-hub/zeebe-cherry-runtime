package io.camunda.cherry.supervisor;


import io.camunda.cherry.runtime.HistoryPerformance;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RunnerThroughput {

    public Analysis checkPerformance(HistoryPerformance.Performance performance) {

        /*
        Check the list of interval. Intervales contains a date, so an the duration for the internval is Interval(n).executionTime - Interval(n-1).executionTime
        In the next, interval(n) is the LAST interval in the list
         */

        Analysis analysis = new Analysis();

        List<HistoryPerformance.Interval> listIntervals = performance.listIntervals;
        if (listIntervals == null || listIntervals.isEmpty()) {
            analysis.throughput = THROUGHPUT.NOTHOUGPUT;
            analysis.backlog = ORIENTATION.NOBACKLOG;
            analysis.execution = ORIENTATION.NOEXECUTION;
            analysis.failure = THROUGHPUT.NOFAILURE;
            analysis.errors = THROUGHPUT.NOERRORS;
            analysis.backLogInSeconds = 0;
            analysis.backLogHuman = formatDurationHuman(0);
            return analysis;
        }

        int size = listIntervals.size();
        HistoryPerformance.Interval intervalN = listIntervals.get(size - 1);
        HistoryPerformance.Interval intervalN1 = size >= 2 ? listIntervals.get(size - 2) : null;
        HistoryPerformance.Interval intervalN2 = size >= 3 ? listIntervals.get(size - 3) : null;

        // Sum executions/failures/BPMN errors over the last 5 intervals (same lookback window as
        // calculateBacklog's rate), to decide whether there was any activity at all worth tagging.
        int lookBack = Math.min(5, size);
        long sumExecutions = 0;
        long sumFailed = 0;
        long sumBpmnErrors = 0;
        for (int i = size - lookBack; i < size; i++) {
            HistoryPerformance.Interval interval = listIntervals.get(i);
            sumExecutions += interval.executions;
            sumFailed += interval.executionsFailed;
            sumBpmnErrors += interval.executionsBpmnErrors;
        }

        analysis.throughput = intervalN.executions == 0
                ? THROUGHPUT.NOTHOUGPUT
                : calculateThroughput(intervalN, intervalN1);
        analysis.backlog = intervalN.topicCount == 0
                ? ORIENTATION.NOBACKLOG
                : calculateOrientationBacklog(intervalN, intervalN1, intervalN2);
        analysis.execution = sumExecutions == 0
                ? ORIENTATION.NOEXECUTION
                : calculateOrientationExecution(intervalN, intervalN1, intervalN2);
        analysis.failure = sumFailed == 0 ? THROUGHPUT.NOFAILURE : calculateErrorLevel(sumFailed);
        analysis.errors = sumBpmnErrors == 0 ? THROUGHPUT.NOERRORS : calculateErrorLevel(sumBpmnErrors);
        calculateBacklog(analysis, listIntervals, intervalN, intervalN1);

        return analysis;
    }

    /**
     * backLogInSeconds/backLogHuman: time to clear interval(n).topicCount, using the best throughput
     * (max executionsSucceeded) seen over the last 5 intervals as the processing rate.
     */
    private void calculateBacklog(Analysis analysis, List<HistoryPerformance.Interval> listIntervals,
                                  HistoryPerformance.Interval intervalN, HistoryPerformance.Interval intervalN1) {
        long topicCount = intervalN.topicCount;
        if (topicCount <= 0) {
            analysis.backLogInSeconds = 0;
            analysis.backLogHuman = formatDurationHuman(0);
            return;
        }

        long durationInSeconds = intervalN1 != null
                ? Duration.between(intervalN1.executionTime, intervalN.executionTime).getSeconds()
                : 0;

        int size = listIntervals.size();
        int lookBack = Math.min(5, size);
        long maxExecutionsSucceeded = 0;
        for (int i = size - lookBack; i < size; i++) {
            maxExecutionsSucceeded = Math.max(maxExecutionsSucceeded, listIntervals.get(i).executionsSucceeded);
        }

        if (durationInSeconds <= 0 || maxExecutionsSucceeded <= 0) {
            // no measurable recent throughput: can't estimate when the backlog will clear
            analysis.backLogInSeconds = -1;
            analysis.backLogHuman = "Unknown";
            return;
        }

        double rate = (double) maxExecutionsSucceeded / durationInSeconds;
        analysis.backLogInSeconds = Math.round(topicCount / rate);
        analysis.backLogHuman = formatDurationHuman(analysis.backLogInSeconds);
    }

    /**
     * 7653 => "2 h 7 mn 33 s"
     */
    private String formatDurationHuman(long totalSeconds) {
        if (totalSeconds < 0) {
            return "Unknown";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append(" h ");
        }
        if (hours > 0 || minutes > 0) {
            result.append(minutes).append(" mn ");
        }
        result.append(seconds).append(" s");
        return result.toString();
    }

    /**
     * Number of executions per second on interval(n), using Interval(n).executionTime - Interval(n-1).executionTime
     * as the interval duration. Falls back to the raw execution count when there is no previous interval.
     */
    private THROUGHPUT calculateThroughput(HistoryPerformance.Interval intervalN, HistoryPerformance.Interval intervalN1) {
        double executionsPerSecond = intervalN.executions;
        if (intervalN1 != null) {
            long durationInSeconds = Duration.between(intervalN1.executionTime, intervalN.executionTime).getSeconds();
            if (durationInSeconds > 0) {
                executionsPerSecond = (double) intervalN.executions / durationInSeconds;
            }
        }
        if (executionsPerSecond < 100) {
            return THROUGHPUT.LOW;
        }
        if (executionsPerSecond <= 500) {
            return THROUGHPUT.MEDIUM;
        }
        return THROUGHPUT.HIGH;
    }

    /**
     * V3=interval(n).topicCount, V2=interval(n-1).topicCount, V1=interval(n-2).topicCount
     */
    private ORIENTATION calculateOrientationBacklog(HistoryPerformance.Interval intervalN,
                                                    HistoryPerformance.Interval intervalN1,
                                                    HistoryPerformance.Interval intervalN2) {
        if (intervalN1 == null || intervalN2 == null) {
            return ORIENTATION.STABLE;
        }
        return calculateOrientation(intervalN2.topicCount, intervalN1.topicCount, intervalN.topicCount);
    }

    /**
     * V3=interval(n).executions, V2=interval(n-1).executions, V1=interval(n-2).executions - trend of
     * the raw execution count (how many jobs ran) over the last 3 intervals, not its average duration.
     */
    private ORIENTATION calculateOrientationExecution(HistoryPerformance.Interval intervalN,
                                                      HistoryPerformance.Interval intervalN1,
                                                      HistoryPerformance.Interval intervalN2) {
        if (intervalN1 == null || intervalN2 == null) {
            return ORIENTATION.STABLE;
        }
        return calculateOrientation(intervalN2.executions, intervalN1.executions, intervalN.executions);
    }

    /**
     * Shared trend calculation for a value sampled on 3 consecutive intervals (V1 oldest, V3 newest).
     * +1000 (absolute) catches a big jump outright; for smaller numbers, +20 (absolute) catches
     * high-volume runners and +50% (relative) catches low-volume ones - e.g. 8 -> 29 -> 49 - where a
     * flat +20 never triggers. The "delta >= 5" floor keeps single-digit noise (e.g. 2 -> 3 -> 4, a
     * 100% jump but trivial in absolute terms) from being flagged.
     */
    private ORIENTATION calculateOrientation(long v1, long v2, long v3) {
        if (v3 > v1 + 1000) {
            return ORIENTATION.HIGHINCREASE;
        }
        if (v3 < v1 - 1000) {
            return ORIENTATION.HIGHDECREASE;
        }

        long delta = v3 - v1;
        boolean growthIsSignificant = v3 > v1 + 20 || (v1 > 0 && v3 >= v1 * 1.5 && delta >= 5);
        boolean dropIsSignificant = v3 < v1 - 20 || (v1 > 0 && v3 <= v1 * 0.67 && -delta >= 5);
        if (v3 > v2 && v2 > v1 && growthIsSignificant) {
            return ORIENTATION.INCREASE;
        }
        if (v3 < v2 && v2 < v1 && dropIsSignificant) {
            return ORIENTATION.DECREASE;
        }
        return ORIENTATION.STABLE;
    }

    /**
     * Shared scale for executionFailed/executionBpmnErrors on interval(n): 0 => LOW, <50 => MEDIUM, else HIGH
     */
    private THROUGHPUT calculateErrorLevel(long count) {
        if (count == 0) {
            return THROUGHPUT.LOW;
        }
        if (count < 50) {
            return THROUGHPUT.MEDIUM;
        }
        return THROUGHPUT.HIGH;
    }

    public enum THROUGHPUT {
        LOW, MEDIUM, HIGH,
        /**
         * intervalN had no executions at all - the frontend hides the throughput tag on this value.
         */
        NOTHOUGPUT,
        /**
         * No failures over the last 5 intervals - the frontend hides the failure tag on this value.
         */
        NOFAILURE,
        /**
         * No BPMN errors over the last 5 intervals - the frontend hides the errors tag on this value.
         */
        NOERRORS
    }

    public enum ORIENTATION {
        HIGHDECREASE, DECREASE, STABLE, INCREASE, HIGHINCREASE,
        /**
         * intervalN.topicCount is 0 - the frontend hides the backlog tag on this value.
         */
        NOBACKLOG,
        /**
         * No executions at all over the last 5 intervals - the frontend hides the execution tag on
         * this value.
         */
        NOEXECUTION
    }

    public static class Analysis {
        /**
         * Look the last interval(n), and check the field execution. Calculate the number of execution per seconds.
         * To understand the interval duration,
         * If executions < 100: LOW
         * between 100 - 500 : MEDIUM
         * Over 500: HIGH
         */
        public THROUGHPUT throughput;

        /**
         * Compare interval(n).topicCount : V3 and interval(n-1).topicCount=V2, and interval(n-2).topicCount=V1
         * if v3 > V1 + 1000: HIGHINCREASE
         * if (v3 < v1-1000 : HIGHDECREASE
         * <p>
         * If V3 > V2 > V1 and (V3 > V1 + 20 or V3 >= V1 * 1.5 with a delta of at least 5) : INCREASE.
         * if v3 < v2 < V1 and (v3 < v1 - 20 or v3 <= v1 * 0.67 with a delta of at least 5) : DECREASE
         * <p>
         * else STABLE
         */
        public ORIENTATION backlog;

        /**
         * Compare Interval(n).averageTimeInMs= V2 and Interval(n).averageTimeInMs=V1
         * if V2 > V1 * 20% : HIGHINCREASE
         * if V2 > V1 * 5% : INCREASE
         * if V2< V1 * 20% : HIGHDECREASE
         * if V2< V1 * 10% : DECREASE
         * else STABLE
         */
        public ORIENTATION execution;


        /**
         * on the last interval, check the interval(n).executionsFailed
         * if == 0 : LOW
         * if < 50 : MEDIUM
         * else HIGH
         */
        public THROUGHPUT failure;
        /**
         * on the last interval, check the interval(n).executionsBpmnErrors
         * if == 0 : LOW
         * if < 50 : MEDIUM
         * else HIGH
         */
        public THROUGHPUT errors;

        /**
         * The backlog is the time to complete the number of topic.
         * Example: topicCount=12300
         * Get the executionsSucceeded. ATTENTION: check the last 5 last interval (interval(n), interval(n-1) and get the max(executionsSucceeded)
         * In one interval of 10 mn, we can process 4800 => rate is 4800/10/60= 8 per s
         * To process the backlog, we need topicCount/rate = 2300/8=1537 s
         */
        public long backLogInSeconds;
        /**
         * Return a human value: example, 7653 s = 2 h 7 mn 33 s
         */
        public String backLogHuman;
    }
}
