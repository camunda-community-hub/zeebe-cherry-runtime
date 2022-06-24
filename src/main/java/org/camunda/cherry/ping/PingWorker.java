/* ******************************************************************** */
/*                                                                      */
/*  PingWorker                                                          */
/*                                                                      */
/* Realize a simple ping                                                */
/* ******************************************************************** */
package org.camunda.cherry.ping;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.camunda.cherry.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

@Component
public class PingWorker extends AbstractWorker {


    private static final String INPUT_MESSAGE = "message";
    private static final String INPUT_DELAY = "delay";
    private static final String OUTPUT_TIMESTAMP = "timestamp";

    private final Logger logger = LoggerFactory.getLogger(PingWorker.class.getName());

    public PingWorker() {
        super("v-ping",
                Arrays.asList(
                        WorkerParameter.getInstance(INPUT_MESSAGE, String.class, Level.OPTIONAL, "Message to log"),
                        WorkerParameter.getInstance(INPUT_DELAY, Long.class, Level.OPTIONAL, "Delay to sleep")),

                Arrays.asList(
                        WorkerParameter.getInstance(OUTPUT_TIMESTAMP, String.class, Level.REQUIRED, "Produce a timestamp")),
                Collections.emptyList()
        );

    }

    @Override
    @ZeebeWorker(type = "v-ping", autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    @Override
    public void execute(final JobClient jobClient, final ActivatedJob activatedJob, ContextExecution contextExecution) {
        String message = getInputStringValue(INPUT_MESSAGE, null, activatedJob);
        Long delay = getInputLongValue(INPUT_DELAY, null, activatedJob);
        logInfo(message);
        if (delay != null) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        String formattedDate = formatter.format(new Date());
        setValue(OUTPUT_TIMESTAMP, formattedDate, contextExecution);

    }
}
