/* ******************************************************************** */
/*                                                                      */
/*  CherryJobWorkerFactory                                                 */
/*                                                                      */
/*  Detect and start workers                                            */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWorker;
import io.camunda.cherry.definition.CherryConnectorJobHandler;
import io.camunda.cherry.definition.SdkRunnerConnector;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.SecretProvider;
import io.camunda.cherry.zeebe.ZeebeContainer;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Service
public class JobRunnerFactory {
  public static final String RUNNER_NOT_FOUND = "RunnerNotFound";
  public static final String TOO_MANY_RUNNERS = "TooManyRunners";
  public static final String UNKNOWN_RUNNER_CLASS = "UnknownRunnerClass";
  public static final String RUNNER_INVALID_DEFINITION = "RUNNER_INVALID_DEFINITION";

  Logger logger = LoggerFactory.getLogger(JobRunnerFactory.class.getName());

  @Autowired
  HistoryFactory historyFactory;

  @Autowired
  StorageRunner storageRunner;

  @Autowired
  RunnerFactory runnerFactory;
  @Autowired
  ZeebeContainer zeebeContainer;

  @Autowired
  LogOperation logOperation;

  @Autowired
  SecretProvider secretProvider;
  /**
   * Key is runnerType
   */
  Map<String, Running> mapRunning = new HashMap<>();

  public void startAll() {
    zeebeContainer.startZeebeeClient();
    if (!zeebeContainer.isOk()) {
      logger.error("ZeebeClient is not started, can't start runner");
      return;
    }

    // get the list from the storage
    List<AbstractRunner> listRunners = runnerFactory.getAllRunners(new StorageRunner.Filter().isActive(true));

    for (AbstractRunner runner : listRunners) {

      try {
        JobWorker jobWorker = createJobWorker(runner);
        if (jobWorker != null) {
          logger.info("CherryJobRunnerFactory: start [" + runner.getType() + (runner.getName() != null ?
              " (" + runner.getName() + ")" :
              "") + "]");
          logOperation.log(OperationEntity.Operation.STARTRUNNER, runner, "");

          mapRunning.put(runner.getType(), new Running(runner, new ContainerJobWorker(jobWorker)));
        }

      } catch (Exception e) {
        logger.error("Can't start runner " + runner.getIdentification() + " : " + e);
      }
    }
  }

  public void stopAll() {
    for (Running running : mapRunning.values()) {
      if (running.runner != null) {
        try {
          stopRunner(running.runner.getIdentification());

        } catch (Exception e) {
          logger.error("ControllerPage on runner [" + running.runner.getIdentification() + "] : " + e);
        }
      }
    }
    zeebeContainer.stopZeebeeClient();
  }

  /**
   * Stop a runner
   *
   * @param runnerType name of the runner (connector/worker)
   * @return true if the runner is stopped
   */
  public boolean stopRunner(String runnerType) throws OperationException {
    Running running = mapRunning.get(runnerType);
    if (running == null) {
      throw new OperationException(RUNNER_NOT_FOUND, "Runner not found");
    }
    closeJobWorker(running.containerJobWorker.getJobWorker());
    running.containerJobWorker.setJobWorker(null);
    mapRunning.remove(runnerType);
    logOperation.log(OperationEntity.Operation.STOPRUNNER, running.runner, "");

    return true;
  }

  /**
   * Start a runner
   *
   * @param runnerType name of the runner (connector/worker)
   * @return true if the runner started
   * @throws OperationException runner can't start
   */
  public boolean startRunner(String runnerType) throws OperationException {
    if (mapRunning.containsKey(runnerType))
      return true; // already started

    List<AbstractRunner> listRunners = runnerFactory.getAllRunners(new StorageRunner.Filter().type(runnerType));
    // we expect only one runner
    if (listRunners.isEmpty()) {
      throw new OperationException(RUNNER_NOT_FOUND, "Runner not found");
    }
    if (listRunners.size() > 1) {
      throw new OperationException(TOO_MANY_RUNNERS, "Too many runner with this runnerType [" + runnerType + "]");
    }
    AbstractRunner runner = listRunners.get(0);
    List<String> listOfErrors = runner.checkValidDefinition().listOfErrors();
    if (!listOfErrors.isEmpty())
      throw new OperationException(RUNNER_INVALID_DEFINITION,
          "Worker has error in the definition : " + String.join(";", listOfErrors));

    JobWorker jobWorker = createJobWorker(runner);
    mapRunning.put(runner.getType(), new Running(runner, new ContainerJobWorker(jobWorker)));
    logOperation.log(OperationEntity.Operation.STARTRUNNER, runner, "");

    return true;
  }

  public boolean isRunnerActive(String runnerType) throws OperationException {
    if (!mapRunning.containsKey(runnerType))
      return false;
    Running running = mapRunning.get(runnerType);
    return running.containerJobWorker.getJobWorker() != null;
  }

  public int getNumberOfThreads() {
    return zeebeContainer.getNumberOfhreads();
  }

  public void setNumberOfThreads(int numberOfThreadsRequired) {
    zeebeContainer.setNumberOfThreadsRequired(numberOfThreadsRequired);

    // stop all running and restart them
    for (Running running : mapRunning.values()) {
      closeJobWorker(running.containerJobWorker.getJobWorker());
      JobWorker jobWorker = null;
      try {
        jobWorker = createJobWorker(running.runner);
      } catch (OperationException e) {
        logger.error("Can't restart [" + running.runner.getName() + "] " + e.getMessage());
      }
      running.containerJobWorker.setJobWorker(jobWorker);
    }
  }
  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Administration on Runner (stop,start)                   */
  /*                                                          */
  /* -------------------------------------------------------- */

  private void closeJobWorker(JobWorker jobWorker) {
    if (jobWorker == null)
      return;
    if (jobWorker.isClosed())
      return;
    jobWorker.close();
    while (!jobWorker.isClosed()) {
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        // do nothing
      }
    }
  }

  /**
   * Create the jobWorker from the Runner
   *
   * @param runner runner to start
   * @return the JobWorker
   * @throws OperationException in case of error
   */
  private JobWorker createJobWorker(AbstractRunner runner) throws OperationException {

    JobHandler jobHandler;
    if (runner instanceof AbstractWorker abstractWorker)
      jobHandler = abstractWorker;
    else if (runner instanceof AbstractConnector abstractConnector)
      jobHandler = new CherryConnectorJobHandler(abstractConnector, historyFactory, secretProvider);
    else if (runner instanceof SdkRunnerConnector sdkRunnerConnector) {
      jobHandler = new CherryConnectorJobHandler(sdkRunnerConnector, historyFactory, secretProvider);
    } else
      throw new OperationException(UNKNOWN_RUNNER_CLASS, "Unknown AbstractRunner class");

    JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild3 = zeebeContainer.getZeebeClient()
        .newWorker()
        .jobType(runner.getType())
        .handler(jobHandler)
        .name(runner.getName() == null ? runner.getType() : runner.getName());

    List<String> listVariablesInput = runner.getListFetchVariables();
    if (listVariablesInput != null) {
      jobWorkerBuild3.fetchVariables(listVariablesInput);
    }
    return jobWorkerBuild3.open();
  }

  /**
   * Not possible to restart a jobWorker: must be created again !
   */
  private static class ContainerJobWorker {
    private JobWorker jobWorker;

    public ContainerJobWorker(JobWorker jobWorker) {
      this.jobWorker = jobWorker;
    }

    public JobWorker getJobWorker() {
      return jobWorker;
    }

    public void setJobWorker(JobWorker jobWorker) {
      this.jobWorker = jobWorker;
    }
  }

  record Running(AbstractRunner runner, ContainerJobWorker containerJobWorker) {
  }

  /**
   * Declare an exception on an operation
   */
  public static class OperationException extends Exception {
    private final String exceptionCode;
    private final String explanation;

    OperationException(String exceptionCode, String explanation) {
      this.exceptionCode = exceptionCode;
      this.explanation = explanation;
    }

    public String getExceptionCode() {
      return exceptionCode;
    }

    public String getExplanation() {
      return explanation;
    }
  }
}
