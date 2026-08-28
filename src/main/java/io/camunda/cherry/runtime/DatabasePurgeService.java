package io.camunda.cherry.runtime;

import io.camunda.cherry.db.repository.OperationRepository;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import io.camunda.cherry.db.repository.TopicCountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DatabasePurgeService {

    Logger logger = LoggerFactory.getLogger(DatabasePurgeService.class.getName());

    @Value("${cherry.database.purgeRetentionDays:30}")
    int purgeRetentionDays;

    private final RunnerExecutionRepository runnerExecutionRepository;
    private final OperationRepository operationRepository;
    private final TopicCountRepository topicCountRepository;

    public DatabasePurgeService(RunnerExecutionRepository runnerExecutionRepository,
                                OperationRepository operationRepository,
                                TopicCountRepository topicCountRepository) {
        this.runnerExecutionRepository = runnerExecutionRepository;
        this.operationRepository = operationRepository;
        this.topicCountRepository = topicCountRepository;

    }

    @EventListener(ApplicationReadyEvent.class)
    public void purgeAllRecords() {
        logger.info("----- DatabasePurge: wiping all execution history on startup");
        runnerExecutionRepository.deleteAllInBatch();
        operationRepository.deleteAllInBatch();
        topicCountRepository.deleteAllInBatch();
        logger.info("----- DatabasePurge: execution history cleared");
    }

    @Transactional
    @Scheduled(initialDelayString = "PT1H", fixedDelayString = "PT1H")
    public void purgeOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(purgeRetentionDays);
        logger.info("----- DatabasePurge: removing records older than {} days (cutoff={})", purgeRetentionDays, cutoff);
        int execDeleted = runnerExecutionRepository.deleteByExecutionTimeBefore(cutoff);
        int opDeleted = operationRepository.deleteByExecutionTimeBefore(cutoff);
        int tcDeleted = topicCountRepository.deleteByExecutionTimeBefore(cutoff);
        logger.info("----- DatabasePurge: deleted executions={} operations={} topicCounts={}", execDeleted, opDeleted, tcDeleted);
    }
}
