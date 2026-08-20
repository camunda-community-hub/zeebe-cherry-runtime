package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.TopicCountEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class TopicCountRepositoryTest {

    @Autowired
    TopicCountRepository topicCountRepository;

    @Test
    public void filtersByRunnerTypeAndDate() {
        LocalDateTime now = LocalDateTime.now();

        save("runner-A", now.minusMinutes(5), 10);
        save("runner-A", now.minusMinutes(30), 20);
        save("runner-A", now.minusHours(2), 99); // outside the 1-hour window
        save("runner-B", now.minusMinutes(5), 5); // different runner

        List<TopicCountEntity> results = topicCountRepository.selectRunnerRecords(
                "runner-A", now.minusHours(1), PageRequest.of(0, 100));

        assertEquals(2, results.size());
        results.forEach(r -> assertEquals("runner-A", r.runnerType));
    }

    @Test
    public void returnsEmptyWhenNothingSaved() {
        List<TopicCountEntity> results = topicCountRepository.selectRunnerRecords(
                "unknown-runner", LocalDateTime.now().minusHours(1), PageRequest.of(0, 100));

        assertEquals(0, results.size());
    }

    @Test
    public void deleteFromRunnerTypeRemovesOnlyTargetRunner() {
        LocalDateTime now = LocalDateTime.now();
        save("runner-A", now, 10);
        save("runner-B", now, 20);

        topicCountRepository.deleteFromRunnerType("runner-A");

        assertEquals(0, topicCountRepository.selectRunnerRecords("runner-A", now.minusMinutes(1), PageRequest.of(0, 100)).size());
        assertEquals(1, topicCountRepository.selectRunnerRecords("runner-B", now.minusMinutes(1), PageRequest.of(0, 100)).size());
    }

    private void save(String runnerType, LocalDateTime time, long count) {
        TopicCountEntity e = new TopicCountEntity();
        e.runnerType = runnerType;
        e.executionTime = time;
        e.topicCount = count;
        topicCountRepository.save(e);
    }
}
