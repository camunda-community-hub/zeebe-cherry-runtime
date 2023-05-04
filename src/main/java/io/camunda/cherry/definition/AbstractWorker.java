/* ******************************************************************** */
/*                                                                      */
/*  Abstract Worker                                                     */
/*                                                                      */
/*  All workers extends this class. It gives tool to access parameters, */
/*  and the contract implementation on parameters                       */
/* ******************************************************************** */
package io.camunda.cherry.definition;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractWorker extends AbstractRunner implements JobHandler {
  Logger loggerAbstractWorker = LoggerFactory.getLogger(AbstractWorker.class.getName());

  @Autowired
  HistoryFactory historyFactory;

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Administration                                          */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * Constructor
   *
   * @param type           type of the worker
   * @param listInput      list of Input parameters for the worker
   * @param listOutput     list of Output parameters for the worker
   * @param listBpmnErrors list of potential BPMN ControllerPage the worker can generate
   */
  protected AbstractWorker(String type,
                           List<RunnerParameter> listInput,
                           List<RunnerParameter> listOutput,
                           List<BpmnError> listBpmnErrors) {
    super(type, listInput, listOutput, listBpmnErrors);
  }

  /**
   * The connector must call immediately this method, which is the skeleton for all executions
   * Attention: this is a Spring Component, so this is the same object called.
   *
   * @param jobClient    connection to Zeebe
   * @param activatedJob information on job to execute
   */
  public void handle(final JobClient jobClient, final ActivatedJob activatedJob) {
    Instant executionInstant = Instant.now();

    ContextExecution contextExecution = new ContextExecution();
    contextExecution.beginExecution = System.currentTimeMillis();

    // log input
    String logInput = getListInput().stream().map(t -> {
      Object value = getValueFromJob(t.name, activatedJob);
      if (value != null && value.toString().length() > 15)
        value = value.toString().substring(0, 15) + "...";
      return t.name + "=[" + value + "]";
    }).collect(Collectors.joining(","));
    if (isLog())
      logInfo("Start " + logInput);
    // first, see if the process respect the contract for this connector
    checkInput(activatedJob);

    validateInput();

    // ok, this is correct, execute it now
    ExecutionStatusEnum status;
    String errorCode = null;
    String errorMessage = null;
    ConnectorException connectorException = null;
    try {
      execute(jobClient, activatedJob, contextExecution);

      validateOutput();

      // let's verify the execution respect the output contract
      checkOutput(contextExecution);
      status = ExecutionStatusEnum.SUCCESS;
    } catch (ConnectorException ce) {
      loggerAbstractWorker.error("ControllerPage during execution " + ce.getMessage() + " " + ce.getMessage());
      status = ExecutionStatusEnum.BPMNERROR;
      errorCode = ce.getErrorCode();
      errorMessage = ce.getMessage();
      connectorException = ce;

    } catch (Exception e) {
      loggerAbstractWorker.error("ControllerPage during execution " + e.getMessage() + " " + e.getCause());
      status = ExecutionStatusEnum.FAIL;
      errorCode = "Exception";
      errorMessage = e.getMessage();
    }
    // save the output in the process instance
    jobClient.newCompleteCommand(activatedJob.getKey()).variables(contextExecution.outVariablesValue).send().join();

    contextExecution.endExecution = System.currentTimeMillis();
    if (isLog())
      logInfo("End in " + (contextExecution.endExecution - contextExecution.beginExecution) + " ms");
    else if (contextExecution.endExecution - contextExecution.beginExecution > 2000)
      logInfo("End in " + (contextExecution.endExecution - contextExecution.beginExecution) + " ms (long)");

    // save execution
    historyFactory.saveExecution(executionInstant, // save this instant
        RunnerExecutionEntity.TypeExecutor.WORKER, // this is a worker
        getType(), // type of worker
        status, // status of execution
        errorCode, errorMessage, // if an error is detected
        contextExecution.endExecution - contextExecution.beginExecution);
  }

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  OperationLog worker                                             */
  /*                                                          */
  /* to normalize the log use these methods
  /* -------------------------------------------------------- */

  /**
   * Worker must implement this method. Real job has to be done here.
   *
   * @param jobClient        connection to Zeebe
   * @param activatedJob     information on job to execute
   * @param contextExecution the same object is used for all call. The contextExecution is an object
   *                         for each execution
   */
  public abstract void execute(final JobClient jobClient,
                               final ActivatedJob activatedJob,
                               ContextExecution contextExecution);

  /**
   * log info
   *
   * @param message message to log
   */
  @Override
  public void logInfo(String message) {
    loggerAbstractWorker.info("CherryWorker[" + getIdentification() + "]:" + message);
  }

  public boolean isWorker() {
    return true;
  }

  public boolean isConnector() {
    return false;
  }
  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Contracts operation on input/output                     */
  /*                                                          */
  /* -------------------------------------------------------- */

}
