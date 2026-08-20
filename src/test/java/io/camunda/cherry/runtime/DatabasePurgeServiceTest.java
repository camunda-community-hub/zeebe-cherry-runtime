package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.db.entity.TopicCountEntity;
import io.camunda.cherry.db.repository.OperationRepository;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import io.camunda.cherry.db.repository.TopicCountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(DatabasePurgeService.class)
@TestPropertySource(properties = "cherry.database.purgeRetentionDays=5")
public class DatabasePurgeServiceTest {

    @Autowired
    DatabasePurgeService databasePurgeService;

    @Autowired
    RunnerExecutionRepository runnerExecutionRepository;

    @Autowired
    OperationRepository operationRepository;

    @Autowired
    TopicCountRepository topicCountRepository;

    // --- purgeAllRecords (startup wipe) ---

    @Test
    public void startupWipeClearsEverythingRegardlessOfAge() {
        LocalDateTime now = LocalDateTime.now();
        saveExecution("runner-A", now.minusDays(6));
        saveExecution("runner-A", now.minusDays(1));
        saveOperation("runner-A", now.minusDays(6));
        saveOperation("runner-A", now.minusDays(1));
        saveTopicCount("runner-A", now.minusDays(6));
        saveTopicCount("runner-A", now.minusDays(1));

        databasePurgeService.purgeAllRecords();

        assertEquals(0, runnerExecutionRepository.count());
        assertEquals(0, operationRepository.count());
        assertEquals(0, topicCountRepository.count());
    }

    @Test
    public void startupWipeIsNopOnEmptyDatabase() {
        databasePurgeService.purgeAllRecords();

        assertEquals(0, runnerExecutionRepository.count());
        assertEquals(0, operationRepository.count());
        assertEquals(0, topicCountRepository.count());
    }

    // --- purgeOldRecords (scheduled time-based cleanup) ---

    @Test
    public void scheduledPurgeDeletesExpiredAndKeepsRecent() {
        LocalDateTime now = LocalDateTime.now();
        saveExecution("runner-A", now.minusDays(6)); // expired
        saveExecution("runner-A", now.minusDays(2)); // recent
        saveOperation("runner-A", now.minusDays(6));
        saveOperation("runner-A", now.minusDays(2));
        saveTopicCount("runner-A", now.minusDays(6));
        saveTopicCount("runner-A", now.minusDays(2));

        databasePurgeService.purgeOldRecords();

        assertEquals(1, runnerExecutionRepository.count());
        assertEquals(1, operationRepository.count());
        assertEquals(1, topicCountRepository.count());
    }

    @Test
    public void scheduledPurgeKeepsAllRecordsWhenNoneAreExpired() {
        LocalDateTime now = LocalDateTime.now();
        saveExecution("runner-A", now.minusDays(1));
        saveOperation("runner-A", now.minusDays(1));
        saveTopicCount("runner-A", now.minusDays(1));

        databasePurgeService.purgeOldRecords();

        assertEquals(1, runnerExecutionRepository.count());
        assertEquals(1, operationRepository.count());
        assertEquals(1, topicCountRepository.count());
    }

    private void saveExecution(String runnerType, LocalDateTime time) {
        RunnerExecutionEntity e = new RunnerExecutionEntity();
        e.runnerType = runnerType;
        e.executionTime = time;
        runnerExecutionRepository.save(e);
    }

    private void saveOperation(String runnerType, LocalDateTime time) {
        OperationEntity e = new OperationEntity();
        e.runnerType = runnerType;
        e.executionTime = time;
        e.operation = OperationEntity.Operation.STARTRUNNER;
        operationRepository.save(e);
    }

    private void saveTopicCount(String runnerType, LocalDateTime time) {
        TopicCountEntity e = new TopicCountEntity();
        e.runnerType = runnerType;
        e.executionTime = time;
        e.topicCount = 1L;
        topicCountRepository.save(e);
    }
}
