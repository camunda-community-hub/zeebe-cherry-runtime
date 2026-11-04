/* ******************************************************************** */
/*                                                                      */
/*  RunnerMonitor                                                        */
/*                                                                      */
/*  Collects every X minutes the pending job count (topicCount) for     */
/*  all runners managed by Cherry and saves a snapshot to the DB.       */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.supervisor;

import io.camunda.cherry.db.StorageService;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.zeebe.OrchestrationAPI;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Configuration
public class RunnerMonitor {

    private final RunnerFactory runnerFactory;
    private final JobRunnerFactory jobRunnerFactory;
    private final OrchestrationAPI orchestrationAPI;
    private final HistoryFactory historyFactory;
    private final Map<String, Long> previousCounts = new LinkedHashMap<>();
    Logger logger = LoggerFactory.getLogger(RunnerMonitor.class.getName());
    @Value("${cherry.runner.refreshTopicsInMinutes:2}")
    private Integer refreshTopicsInMinutes;
    @Value("${cherry.runner.refreshCollectAllTopics:true}")
    private Boolean collectAllTopics;
    private ThreadPoolTaskScheduler scheduler;

    RunnerMonitor(RunnerFactory runnerFactory,
                  JobRunnerFactory jobRunnerFactory,
                  OrchestrationAPI orchestrationAPI,
                  HistoryFactory historyFactory) {
        this.runnerFactory = runnerFactory;
        this.jobRunnerFactory = jobRunnerFactory;
        this.orchestrationAPI = orchestrationAPI;
        this.historyFactory = historyFactory;
    }

    @PostConstruct
    public void init() {
        logger.info("RunnerMonitor starting, refresh every {} mn", refreshTopicsInMinutes);
        try {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("runner-monitor-");
            scheduler.initialize();
            refreshListTopics();
            scheduleNext();
        } catch (Exception e) {
            logger.error("RunnerMonitor.init failed: {}", e.getMessage(), e);
        }
    }

    public long getJobCount(String runnerType) {
        return orchestrationAPI.getJobCount(runnerType);
    }

    private void refreshListTopics() {
        if (collectAllTopics) {

            Map<String, Long> allJobCounts = orchestrationAPI.getAllJobCount();
            logger.info("Collecting topic counts found {} types", allJobCounts.size());
            for (Map.Entry<String, Long> entry : allJobCounts.entrySet()) {
                historyFactory.saveTopicCount(entry.getKey(), entry.getValue());
            }
            return;
        }

        if (!jobRunnerFactory.isStarted()) {
            logger.info("runnerMonitor : initialization still in progress");
            return;
        }

        List<AbstractRunner> runnerList = runnerFactory.getAllRunners(new StorageService.Filter());

        if (runnerList.isEmpty()) {
            logger.warn("RunnerMonitor - no runners registered yet, skipping topic count");
            return;
        }

        Map<String, Long> changedTypes = new LinkedHashMap<>();

        for (AbstractRunner runner : runnerList) {
            String runnerType = runner.getType();
            try {
                long count = orchestrationAPI.getJobCount(runnerType);
                historyFactory.saveTopicCount(runnerType, count);

                Long previous = previousCounts.get(runnerType);
                if (previous == null || previous != count) {
                    changedTypes.put(runnerType, count);
                    previousCounts.put(runnerType, count);
                }
            } catch (Exception e) {
                logger.error("RunnerMonitor - jobType[{}] failed: {}", runnerType, e.getMessage());
            }
        }

        if (!changedTypes.isEmpty()) {
            StringBuilder summary = new StringBuilder("RunnerMonitor - jobs waiting changed: ");
            changedTypes.forEach((type, count) -> summary.append(type).append("=").append(count).append(", "));
            summary.setLength(summary.length() - 2);
            logger.info(summary.toString());
        }
    }

    private void scheduleNext() {
        scheduler.schedule(
                () -> {
                    refreshListTopics();
                    scheduleNext();
                },
                Instant.now().plusSeconds(refreshTopicsInMinutes * 60));
    }
}
