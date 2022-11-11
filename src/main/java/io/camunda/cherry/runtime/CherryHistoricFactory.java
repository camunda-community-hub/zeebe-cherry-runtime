/* ******************************************************************** */
/*                                                                      */
/*  CherryHistoriqueFactory                                                 */
/*                                                                      */
/*  Collect and return historic information                                           */
/* ******************************************************************** */
package io.camunda.cherry.runtime;


import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CherryHistoricFactory {

    public static class Statistic {
        public long executions;
        public long failed;
    }

    private final Random rand = new Random(System.currentTimeMillis());




    public static class Interval {
        /**
         * Name is something like 16:00 / 16:15
         */
        public String slot;
        public long executions;
        public long picTimeInMs;
        public long averageTimeInMs;

    }

    public static class Performance {
        public long picTimeInMs;
        public long executions;
        public long averageTimeInMs;
        public List<Interval> listIntervals = new ArrayList<>();
    }


    public Statistic getStatistic(String runnerName, int delayStatInHour) {
        Statistic statistic = new Statistic();
        statistic.executions = rand.nextLong(1000 * delayStatInHour);
        statistic.failed = rand.nextLong(10 * delayStatInHour);
        return statistic;
    }


    public Performance getPerformance(String runnerName, int delayStatInHour) {
        Performance performance = new Performance();
        ZonedDateTime referenceTime = ZonedDateTime.now();
        // back the the previous quater
        referenceTime = referenceTime.minusNanos(referenceTime.getNano());
        referenceTime = referenceTime.minusSeconds(referenceTime.getSecond());
        referenceTime = referenceTime.minusMinutes(referenceTime.getMinute() % 15);

        // back to the number of hour now
        referenceTime.minusHours(delayStatInHour);


        for (int i = 0; i < delayStatInHour * 4; i++) {
            Interval interval = new Interval();
            performance.listIntervals.add( interval );
            // java.time.format.datetimeformatter can't be found by Java - syntax error
            interval.slot = String.format("%2d:%2d", referenceTime.getHour(), referenceTime.getMinute());
            interval.averageTimeInMs = rand.nextLong(2000);
            interval.executions = rand.nextLong(100);
            interval.picTimeInMs = interval.averageTimeInMs + 10 + rand.nextLong(100);

            referenceTime = referenceTime.plusMinutes(15);

            // cumul
            performance.picTimeInMs = Math.max(performance.picTimeInMs, interval.picTimeInMs);
            performance.executions += interval.executions;
            performance.averageTimeInMs += interval.averageTimeInMs;
        }
        performance.averageTimeInMs = performance.averageTimeInMs /(delayStatInHour * 4);

        return performance;
    }


    public Performance getEnginePerformance( int delayStatInHour) {
        return getPerformance("",  delayStatInHour);
    }

    public Statistic getEngineStatistic(int delayStatInHour) {
        return getStatistic("", delayStatInHour);
    }

    }
