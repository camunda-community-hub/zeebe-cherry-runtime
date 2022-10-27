/* ******************************************************************** */
/*                                                                      */
/*  PingWorker                                                          */
/*                                                                      */
/* Realize a simple ping                                                */
/* ******************************************************************** */
package org.camunda.cherry.ping;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.camunda.cherry.definition.AbstractWorker;
import org.camunda.cherry.definition.IntFrameworkRunner;
import org.camunda.cherry.definition.RunnerParameter;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

@Component
public class PingWorker extends AbstractWorker implements IntFrameworkRunner {


    private static final String INPUT_MESSAGE = "message";
    private static final String INPUT_DELAY = "delay";
    private static final String OUTPUT_TIMESTAMP = "timestamp";

    public PingWorker() {
        super("c-ping",
                Arrays.asList(
                        RunnerParameter.getInstance(INPUT_MESSAGE, "Message", String.class, RunnerParameter.Level.OPTIONAL, "Message to log"),
                        RunnerParameter.getInstance(INPUT_DELAY, "Delay", Long.class, RunnerParameter.Level.OPTIONAL, "Delay to sleep")
                ),
                Collections.singletonList(
                        RunnerParameter.getInstance(OUTPUT_TIMESTAMP, "Time stamp", String.class, RunnerParameter.Level.REQUIRED, "Produce a timestamp")
                ),
                Collections.emptyList()
        );

    }

    /**
     * mark this worker as a Framework runner
     *
     * @return true because this worker is part of the Cherry framework
     */
    @Override
    public boolean isFrameworkRunner() {
        return true;
    }

    @Override
    public String getName() {
        return "Ping worker";
    }

    @Override
    public String getLabel() {
        return "Ping (Worker)";
    }


    @Override
    public String getDescription() {
        return "Do a simple ping as a Worker, and return a timestamp. A Delay can be set as parameter.";
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
        setOutputValue(OUTPUT_TIMESTAMP, formattedDate, contextExecution);

    }


}
