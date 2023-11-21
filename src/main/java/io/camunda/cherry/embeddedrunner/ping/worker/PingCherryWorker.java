/* ******************************************************************** */
/*                                                                      */
/*  PingWorker                                                          */
/*                                                                      */
/* Realize a simple ping                                                */
/* ******************************************************************** */
package io.camunda.cherry.embeddedrunner.ping.worker;

import io.camunda.cherry.definition.AbstractWorker;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.cherry.definition.RunnerParameter;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

@Component
public class PingCherryWorker extends AbstractWorker implements IntFrameworkRunner {

  public static final String INPUT_MESSAGE = "message";
  public static final String INPUT_DELAY = "delay";
  public static final String INPUT_THROWERRORPLEASE = "throwErrorPlease";

  public static final String OUTPUT_TIMESTAMP = "timestamp";
  public static final String ERROR_BAD_WEATHER = "BAD_WEATHER";

  public static final String OUTPUT_TEMPERATURE= "temperature";
  public static final String OUTPUT_HUMIDITY = "humidity";

  private final Random random = new Random();

  public PingCherryWorker() {
    super("c-pingworker", Arrays.asList(
        RunnerParameter.getInstance(INPUT_MESSAGE, "Message", String.class, RunnerParameter.Level.OPTIONAL,
            "Message to log"),
        RunnerParameter.getInstance(INPUT_DELAY, "Delay", Long.class, RunnerParameter.Level.OPTIONAL,
            "Delay to sleep"),
        RunnerParameter.getInstance(INPUT_THROWERRORPLEASE, "ThrowError", Boolean.class, RunnerParameter.Level.OPTIONAL,
            "Please throw an error")),

        Collections.singletonList(
        RunnerParameter.getInstance(OUTPUT_TIMESTAMP, "Time stamp", String.class, RunnerParameter.Level.REQUIRED,
            "Produce a timestamp")),
        Collections.singletonList(BpmnError.getInstance(ERROR_BAD_WEATHER, "Bad Weather")));
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

    Boolean throwErrorPlease = getInputBooleanValue(INPUT_THROWERRORPLEASE, Boolean.FALSE, activatedJob);


    if (Boolean.TRUE.equals(throwErrorPlease)) {
      setOutputValue(OUTPUT_TEMPERATURE , "6C", contextExecution);
      setOutputValue(OUTPUT_HUMIDITY, "100%", contextExecution);
      throw new ConnectorException(ERROR_BAD_WEATHER, "Weather is rainy");
    }

    if (delay != null && delay < 0) {
      delay = random.nextLong(10000) + 1500L;
    }
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
