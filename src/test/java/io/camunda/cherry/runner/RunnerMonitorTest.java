package io.camunda.cherry.runner;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.zeebe.OrchestrationAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RunnerMonitorTest {

    @Mock
    RunnerFactory runnerFactory;

    @Mock
    OrchestrationAPI orchestrationAPI;

    @Mock
    HistoryFactory historyFactory;

    @InjectMocks
    RunnerMonitor runnerMonitor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(runnerMonitor, "refreshTopicsInMinutes", 10);
        ReflectionTestUtils.setField(runnerMonitor, "refreshTopicsInSeconds", 0);
    }

    @Test
    public void savesTopicCountForEachRunner() {
        AbstractRunner runnerA = mockRunner("worker-email");
        AbstractRunner runnerB = mockRunner("worker-pdf");

        when(runnerFactory.getAllRunners(any())).thenReturn(List.of(runnerA, runnerB));
        when(orchestrationAPI.getJobCount("worker-email")).thenReturn(5L);
        when(orchestrationAPI.getJobCount("worker-pdf")).thenReturn(12L);

        // call refreshListTopics via init() — scheduler fires immediately then reschedules
        // we test just the refresh logic by calling init and verifying interactions
        // (scheduler is internal; we verify the side effects instead)
        runnerMonitor.init();

        verify(historyFactory).saveTopicCount("worker-email", 5L);
        verify(historyFactory).saveTopicCount("worker-pdf", 12L);
    }

    @Test
    public void apiErrorForOneRunnerDoesNotStopOthers() {
        AbstractRunner runnerA = mockRunner("worker-email");
        AbstractRunner runnerB = mockRunner("worker-pdf");

        when(runnerFactory.getAllRunners(any())).thenReturn(List.of(runnerA, runnerB));
        when(orchestrationAPI.getJobCount("worker-email")).thenThrow(new RuntimeException("Zeebe unreachable"));
        when(orchestrationAPI.getJobCount("worker-pdf")).thenReturn(3L);

        runnerMonitor.init();

        // email failed — should not save
        verify(historyFactory, never()).saveTopicCount(eq("worker-email"), anyLong());
        // pdf still saved
        verify(historyFactory).saveTopicCount("worker-pdf", 3L);
    }

    private AbstractRunner mockRunner(String type) {
        AbstractRunner runner = mock(AbstractRunner.class);
        when(runner.getType()).thenReturn(type);
        return runner;
    }
}
