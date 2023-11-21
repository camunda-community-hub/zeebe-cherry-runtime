package io.camunda.cherry.embeddedrunner.ping.basicworker;

import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class BasicPingWorker implements IntFrameworkRunner {

  /**
   * Basic Ping Worker + Throw error
   */

  private final Logger logger = LoggerFactory.getLogger(BasicPingWorker.class.getName());

  public static final String TYPE_BASICPINGWORKER = "c-basicpingworker";
  private static final String INPUT_MESSAGE = "message";
  private static final String INPUT_DELAY = "delay";
  private static final String INPUT_THROWERRORPLEASE = "throwErrorPlease";
  private static final String OUTPUT_TIMESTAMP = "timestamp";
  public static final String ERROR_BAD_WEATHER = "BAD_WEATHER";

  private final Random random = new Random();

  @JobWorker(type = "c-basicpingworker-plus", name = "basicpingconnector-2")
  public void handleBasicWorker2(final JobClient client, final ActivatedJob job) {
    logger.info("WorkerVariables.handleWorkerVariable : >>>>>>>>>>> start [" + job.getKey() + "]");
    Map<String, Object> variablesAsMap = job.getVariablesAsMap();

  }
    @JobWorker(type = TYPE_BASICPINGWORKER)
  public void handleBasicWorker(final JobClient client, final ActivatedJob job) {
    logger.info("WorkerVariables.handleWorkerVariable : >>>>>>>>>>> start [" + job.getKey() + "]");
    Map<String, Object> variablesAsMap = job.getVariablesAsMap();
    String message = (String) variablesAsMap.getOrDefault(INPUT_MESSAGE, new String());
    Long delay = (Long) variablesAsMap.getOrDefault(INPUT_DELAY, Long.valueOf(0));
    Boolean throwErrorPlease = (Boolean) variablesAsMap.getOrDefault(INPUT_THROWERRORPLEASE, Boolean.FALSE);

    logger.info(message);

    if (Boolean.TRUE.equals(throwErrorPlease)) {

      HashMap<String, Object> variables = new HashMap<>();
      variables.put("temperature", "6C");
      variables.put("humidity", "100%");
      client.newThrowErrorCommand(job)
          .errorCode(ERROR_BAD_WEATHER)
          .errorMessage("Weath is rainy")
          .variables(variables)
          .send()
          .join();
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
    HashMap<String, Object> variables = new HashMap<>();

    DateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    String formattedDate = formatter.format(new Date());
    variables.put(OUTPUT_TIMESTAMP, formattedDate);
    client.newCompleteCommand(job).variables(variables).send().join();

  }

  @Override
  public boolean isFrameworkRunner() {
    return true;
  }
}

