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
            analysis.throughput = THROUGHPUT.LOW;
            analysis.backlog = ORIENTATION.STABLE;
            analysis.execution = ORIENTATION.STABLE;
            analysis.failure = THROUGHPUT.LOW;
            analysis.errors = THROUGHPUT.LOW;
            analysis.backLogInSeconds = 0;
            analysis.backLogHuman = formatDurationHuman(0);
            return analysis;
        }

        int size = listIntervals.size();
        HistoryPerformance.Interval intervalN = listIntervals.get(size - 1);
        HistoryPerformance.Interval intervalN1 = size >= 2 ? listIntervals.get(size - 2) : null;
        HistoryPerformance.Interval intervalN2 = size >= 3 ? listIntervals.get(size - 3) : null;

        analysis.throughput = calculateThroughput(intervalN, intervalN1);
        analysis.backlog = calculateOrientationBacklog(intervalN, intervalN1, intervalN2);
        analysis.execution = calculateOrientationExecution(intervalN, intervalN1);
        analysis.failure = calculateErrorLevel(intervalN.executionsFailed);
        analysis.errors = calculateErrorLevel(intervalN.executionsBpmnErrors);
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
        long v3 = intervalN.topicCount;
        long v2 = intervalN1.topicCount;
        long v1 = intervalN2.topicCount;

        if (v3 > v1 + 1000) {
            return ORIENTATION.HIGHINCREASE;
        }
        if (v3 < v1 - 1000) {
            return ORIENTATION.HIGHDECREASE;
        }
        if (v3 > v2 && v2 > v1 && v3 > v1 + 50) {
            return ORIENTATION.INCREASE;
        }
        if (v3 < v2 && v2 < v1 && v3 < v1 - 50) {
            return ORIENTATION.DECREASE;
        }
        return ORIENTATION.STABLE;
    }

    /**
     * V2=interval(n).averageTimeInMs, V1=interval(n-1).averageTimeInMs. Thresholds are expressed as a
     * percentage change of V1 (e.g. HIGHINCREASE when V2 rose by at least 20% over V1).
     */
    private ORIENTATION calculateOrientationExecution(HistoryPerformance.Interval intervalN,
                                                       HistoryPerformance.Interval intervalN1) {
        if (intervalN1 == null) {
            return ORIENTATION.STABLE;
        }
        long v2 = intervalN.averageTimeInMs;
        long v1 = intervalN1.averageTimeInMs;

        if (v2 > v1 * 1.20) {
            return ORIENTATION.HIGHINCREASE;
        }
        if (v2 > v1 * 1.05) {
            return ORIENTATION.INCREASE;
        }
        if (v2 < v1 * 0.80) {
            return ORIENTATION.HIGHDECREASE;
        }
        if (v2 < v1 * 0.90) {
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

    public static class Analysis {
        /**
         *
         * Look the last interval(n), and check the field execution. Calculate the number of execution per seconds.
         * To understand the interval duration,
         * If executions < 100: LOW
         * between 100 - 500 : MEDIUM
         * Over 500: HIGH
         */
        public THROUGHPUT throughput;

        /** Compare interval(n).topicCount : V3 and interval(n-1).topicCount=V2, and interval(n-2).topicCount=V1
         * if v3 > V1 + 1000: HIGHINCREASE
         * if (v3 < v1-1000 : HIGHDECREASE
         *
         * If V3 > V2 > V1 and V3 > V1 +50 INCREASE.
         * if v3 < v2 < V1 and v3 < v1-50 : DECREASE
         *
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
         *
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
         * 
         */
        public long backLogInSeconds;
        /**
         *  Return a human value: example, 7653 s = 2 h 7 mn 33 s
         */
        public String backLogHuman;
    }

    public enum THROUGHPUT {
        LOW, MEDIUM, HIGH
    }
    public enum ORIENTATION {
        HIGHDECREASE, DECREASE, STABLE, INCREASE, HIGHINCREASE
    }
}
