package org.camunda.vercors.ping;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.camunda.vercors.definition.WorkerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class PingWorker extends WorkerBase {

    Logger logger = LoggerFactory.getLogger(PingWorker.class.getName());

    public PingWorker() {
        super("v-ping",
                Collections.emptyList(),
                Collections.emptyList());
    }

    @ZeebeWorker(type = "v-ping", autoComplete = true)
    public void handleWorkerExecution(final JobClient client, final ActivatedJob job) {
        super.handleWorkerExecution(client, job);
    }


    public void execute(final JobClient client, final ActivatedJob job) {
        logger.info("Vercors-Ping");
    }
}
