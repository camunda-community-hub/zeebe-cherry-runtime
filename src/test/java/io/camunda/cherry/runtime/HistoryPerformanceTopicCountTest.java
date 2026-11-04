package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.TopicCountEntity;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import io.camunda.cherry.db.repository.TopicCountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HistoryPerformanceTopicCountTest {

    @Mock
    RunnerExecutionRepository runnerExecutionRepository;

    @Mock
    TopicCountRepository topicCountRepository;

    @InjectMocks
    HistoryPerformance historyPerformance;

    private static final String RUNNER_TYPE = "my-worker";

    @BeforeEach
    void setUp() {
        // no executions — we only care about topic count binning
        when(runnerExecutionRepository.selectRunnerRecords(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    public void topicCountAppearsInCorrectSlot() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0, 0);
        // snapshot taken 5 minutes ago → should land in the 12:00 slot (FOURHOUR uses 10-min slots)
        LocalDateTime snapshotTime = now.minusMinutes(5);

        TopicCountEntity snapshot = new TopicCountEntity();
        snapshot.runnerType = RUNNER_TYPE;
        snapshot.executionTime = snapshotTime;
        snapshot.topicCount = 42;

        when(topicCountRepository.selectRunnerRecords(eq(RUNNER_TYPE), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(snapshot));

        HistoryPerformance.Performance performance = historyPerformance.getPerformance(RUNNER_TYPE,
                now,
                HistoryPerformance.PeriodStatistic.FOURHOUR,
                0);

        // find the slot that contains our snapshot time
        HistoryPerformance.IntervalRule rule = historyPerformance.getIntervalRuleByPeriod(HistoryPerformance.PeriodStatistic.FOURHOUR);
        String expectedSlot = rule.getSlotFromDate(snapshotTime);

        HistoryPerformance.Interval matchingInterval = performance.listIntervals.stream()
                .filter(i -> i.slot.equals(expectedSlot))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Slot not found: " + expectedSlot));

        assertEquals(42, matchingInterval.topicCount);
    }

    @Test
    public void emptyTopicCountLeavesIntervalAtZero() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0, 0);

        when(topicCountRepository.selectRunnerRecords(eq(RUNNER_TYPE), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        HistoryPerformance.Performance performance = historyPerformance.getPerformance(RUNNER_TYPE,
                now,
                HistoryPerformance.PeriodStatistic.FOURHOUR,
                0);


        performance.listIntervals.forEach(i -> assertEquals(0, i.topicCount));
    }
}
