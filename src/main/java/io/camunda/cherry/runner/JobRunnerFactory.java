/* ******************************************************************** */
/*                                                                      */
/*  JobRunnerFactory                                                 */
/*                                                                      */
/*  Detect and start workers                                            */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWorker;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.cherry.definition.connector.SdkRunnerCherryConnector;
import io.camunda.cherry.definition.connector.SdkRunnerConnector;
import io.camunda.cherry.definition.connector.SdkRunnerWorker;
import io.camunda.cherry.embeddedrunner.ping.PingIntRunner;
import io.camunda.cherry.exception.OperationTooManyRunnersException;
import io.camunda.cherry.exception.OperationAlreadyStartedException;
import io.camunda.cherry.exception.OperationAlreadyStoppedException;
import io.camunda.cherry.exception.OperationCantStopRunnerException;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.handler.CherryConnectorJobHandler;
import io.camunda.cherry.runner.handler.CherryWorkerJobHandler;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.SecretProvider;
import io.camunda.cherry.zeebe.ZeebeCherryConfiguration;
import io.camunda.cherry.zeebe.ZeebeContainer;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Service
public class JobRunnerFactory {

  public static final String RUNNER_NOT_FOUND = "RunnerNotFound";

  public static final String UNKNOWN_RUNNER_CLASS = "UnknownRunnerClass";
  public static final String RUNNER_INVALID_DEFINITION = "RUNNER_INVALID_DEFINITION";

  private static final ObjectMapper objectMapper = new ObjectMapper();
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
  ZeebeCherryConfiguration zeebeConfiguration;
  @Autowired
  LogOperation logOperation;
  @Autowired
  SecretProvider secretProvider;
  @Autowired
  ValidationProvider validationProvider;
  /**
   * Key is runnerType
   */
  Map<String, Running> mapRunning = new HashMap<>();
  @Value("${cherry.runners.embeddedrunner:true}")
  private Boolean executeEmbeddedRunner = Boolean.TRUE;

  @Value("${cherry.runners.pingrunner:true}")
  private Boolean executePingRunner = Boolean.FALSE;

  public void startAll() {

    // read the configuration
    zeebeConfiguration.init();

    // now start the Zeebe Client
    try {
      zeebeContainer.startZeebeeClient();
    } catch (TechnicalException e) {
      logOperation.log(OperationEntity.Operation.ERROR, "Can't start zeebe Client " + e.getMessage());
      logger.error("ZeebeClient is not started, can't start runner");
      return;
    }

    if (!zeebeContainer.isOk()) {
      logger.error("ZeebeClient is not started, can't start runner");
      return;
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String utcDateString = sdf.format(new Date());

    logOperation.log(OperationEntity.Operation.STARTRUNTIME, utcDateString + " UTC");

    resumeAllRunners();
  }

  /**
   *
   */
  public void stopAll() {
    logOperation.log(OperationEntity.Operation.STOPRUNTIME, "");

    suspendAllRunners();
    zeebeContainer.stopZeebeeClient();
  }

  /**
   * Restart all runners
   */
  public void resumeAllRunners() {
    // get the list from the storage
    List<AbstractRunner> listRunners = runnerFactory.getAllRunners(new StorageRunner.Filter().isActive(true));
    logger.info("Start executeEmbeddedRunner:{} executePingRunner:{}", executeEmbeddedRunner, executePingRunner);
    if (Boolean.FALSE.equals(executeEmbeddedRunner)) {
      logger.info("Don't start the EmbeddedWorker");

      // remove from the list the embeddedRunner
      listRunners = listRunners.stream().filter(t -> {
        return !isEmbeddedWorker(t);
      }).toList();
    }
    if (Boolean.FALSE.equals(executePingRunner)) {
      logger.info("Don't start the PingWorker");
      // remove from the list the embeddedRunner
      listRunners = listRunners.stream().filter(t -> {
        return !isPingWorker(t);
      }).toList();
    }

    List<AbstractRunner> listSdkRunners = listRunners.stream()
        .filter(t -> t instanceof SdkRunnerConnector)
        .filter(t -> !(t instanceof SdkRunnerCherryConnector))
        .toList();
    List<AbstractRunner> listSdkCherryRunners = listRunners.stream()
        .filter(t -> t instanceof SdkRunnerCherryConnector)
        .toList();
    List<AbstractRunner> listOtherRunners = listRunners.stream()
        .filter(element -> !listSdkRunners.contains(element))
        .filter(element -> !listSdkCherryRunners.contains(element))
        .toList();

    logger.info("--- SdkRunner to start (active runner only)");
    for (AbstractRunner runner : listSdkRunners) {
      logger.info("  [{}] - [{}]", runner.getName(), runner.getType());
    }
    logger.info("--- SdkCherryRunner to start");
    for (AbstractRunner runner : listSdkCherryRunners) {
      logger.info("  [{}] - [{}]", runner.getName(), runner.getType());
    }
    logger.info("--- OtherRunners to start");
    for (AbstractRunner runner : listOtherRunners) {
      logger.info("  [{}] - [{}]", runner.getName(), runner.getType());
    }
    logger.info("---");

    for (AbstractRunner runner : listRunners) {

      try {
        JobWorker jobWorker = createJobWorker(runner);
        if (jobWorker != null) {
          logOperation.log(OperationEntity.Operation.STARTRUNNER, runner, "Started[" + runner.getType() + "]");

          mapRunning.put(runner.getType(), new Running(runner, new ContainerJobWorker(jobWorker)));
        }

      } catch (Exception e) {
        logger.error("Can't start runner [{}] : {} ", runner.getIdentification(), e);
      }
    }
  }

  /**
   * stop all runners
   */
  public void suspendAllRunners() {
    for (Running running : mapRunning.values()) {
      if (running.runner != null) {
        try {
          stopRunner(running.runner.getType());

        } catch (Exception e) {
          logger.error("ControllerPage on runner [{}] : {}", running.runner.getIdentification(), e);
        }
      }
    }

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
      throw new OperationAlreadyStoppedException();
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
    if (mapRunning.containsKey(runnerType)) {
      throw new OperationAlreadyStartedException();
    }
    List<AbstractRunner> listRunners = runnerFactory.getAllRunners(new StorageRunner.Filter().type(runnerType));
    // we expect only one runner
    if (listRunners.isEmpty()) {
      throw new OperationException(RUNNER_NOT_FOUND, "Runner not found");
    }
    if (listRunners.size() > 1) {
      throw new OperationTooManyRunnersException("runnerType [" + runnerType + "]");
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

  public boolean isRunnerExist(String runnerType) {
    return mapRunning.containsKey(runnerType);
  }

  public boolean isActiveRunner(String runnerType) {
    if (!mapRunning.containsKey(runnerType))
      return false;
    Running running = mapRunning.get(runnerType);
    return running.containerJobWorker.getJobWorker() != null;
  }

  /**
   * We ask the container what is the number of job active configured
   *
   * @return number of job active
   */
  public int getMaxJobActive() {
    return zeebeContainer.getMaxJobsActive();
  }

  public int getNumberOfThreads() {
    return zeebeContainer.getNumberOfThreads();
  }

  public void setNumberOfThreads(int numberOfThreadsRequired) throws TechnicalException {
    zeebeConfiguration.setNumberOfThreads(numberOfThreadsRequired);
    zeebeContainer.stopZeebeeClient();
    zeebeContainer.startZeebeeClient();

    // stop all running and restart them
    for (Running running : mapRunning.values()) {
      JobWorker jobWorker = null;
      try {
        closeJobWorker(running.containerJobWorker.getJobWorker());

        jobWorker = createJobWorker(running.runner);
      } catch (OperationException e) {
        logger.error("Can't restart [{}] : {} ", running.runner.getName(), e.getMessage());
      }
      running.containerJobWorker.setJobWorker(jobWorker);
    }
  }
  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Administration on Runner (stop,start)                   */
  /*                                                          */
  /* -------------------------------------------------------- */

  private void closeJobWorker(JobWorker jobWorker) throws OperationCantStopRunnerException {
    if (jobWorker == null)
      return;
    if (jobWorker.isClosed())
      return;
    jobWorker.close();
    // protection: wait one minutes, and then we consider the worker as stopped
    long beginTimeOperation = System.currentTimeMillis();
    while (!jobWorker.isClosed() && System.currentTimeMillis() - beginTimeOperation < 1000 * 60) {
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        // do nothing
      }
    }
    if (!jobWorker.isClosed())
      throw new OperationCantStopRunnerException();
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
    else if (runner instanceof AbstractConnector abstractConnector) {
      jobHandler = new CherryConnectorJobHandler(abstractConnector, historyFactory, secretProvider, validationProvider,
          objectMapper);
    } else if (runner instanceof SdkRunnerConnector sdkRunnerConnector) {
      jobHandler = new CherryConnectorJobHandler(sdkRunnerConnector, historyFactory, secretProvider, validationProvider,
          objectMapper);
    } else if (runner instanceof SdkRunnerWorker sdkRunnerWorker) {
      jobHandler = new CherryWorkerJobHandler(sdkRunnerWorker, historyFactory, secretProvider);
    } else {
      throw new OperationException(UNKNOWN_RUNNER_CLASS, "Unknown AbstractRunner class");
    }
    JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild3 = zeebeContainer.getZeebeClient()
        .newWorker()
        .jobType(runner.getType())
        .handler(jobHandler)
        .name(runner.getName() == null ? runner.getType() : runner.getName());

    // jobWorkerBuild3.maxJobsActive()

    List<String> listVariablesInput = runner.getListFetchVariables();
    if (listVariablesInput != null && !listVariablesInput.isEmpty()) {
      jobWorkerBuild3.fetchVariables(listVariablesInput);
    }
    return jobWorkerBuild3.open();

  }

  @Scheduled(fixedDelay = 30000)
  public void checkZeebeConnection() {
    boolean checkConnection = zeebeContainer.retryConnection();
    // if the connection is false, pause all runners

    // if the connection is true, resume all runners which need to be resume

  }

  private boolean isEmbeddedWorker(AbstractRunner runner) {
    return runner instanceof IntFrameworkRunner && !(runner instanceof PingIntRunner);
  }

  private boolean isPingWorker(AbstractRunner runner) {
    if (runner instanceof SdkRunnerWorker)
      return ((SdkRunnerWorker) runner).getWorker() instanceof PingIntRunner;
    return runner instanceof PingIntRunner;
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

}
