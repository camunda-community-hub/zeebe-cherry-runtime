/* ******************************************************************** */
/*                                                                      */
/*  RunnerMonitor                                               */
/*                                                                      */
/*  Collecter every x minutes topicCount for all workers managed by Cherry  */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.definition.AbstractRunner;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Configuration
public class RunnerMonitor {
    @Value("${cherry.runner.refreshTopicsInMinutes:10}")
    private Integer refreshTopicsInMinutes;

    private final RunnerFactory runnerFactory;

    RunnerMonitor(RunnerFactory runnerFactory) {
        this.runnerFactory = runnerFactory;
    }
private void refreshListTopics() {
    List<AbstractRunner> runnerList = runnerFactory.getAllRunners(StorageRunner.Filter filter)
            for (AbstractRunner runner: runnerList) {
                runner.getType();
                camundaClient or orchestrtionAPI?
                        camundaClient ==> ZeebeContainer.getTopic(runnger.getType())

                        orderchestationApi..getTopic(runnger.getType())



            }
}

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        refreshListTopics();
        scheduleNext();
    }


    private void scheduleNext() {
        scheduler.schedule(this::refreshListTenants, Instant.now().plusSeconds(this.refreshTopicsInMinutes * 20));
    }
}
}