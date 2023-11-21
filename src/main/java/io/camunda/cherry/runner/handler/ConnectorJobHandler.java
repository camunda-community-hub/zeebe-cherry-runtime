/* ******************************************************************** */
/*                                                                      */
/*  ConnectorJobHandler                                                 */
/*                                                                      */
/*  Execution - ZeebeClient lib call this handle then we can collect    */
/*  statistics                                                          */
/*                                                                      */
/* this class get the object to run as the                              */
/*  sdkRunnerWorker.getTransportedObject()                              */
/* It register itself with the same topic, so capture the "handle()"    */
/* call from ZeebeClient. Implements statistics, then call the          */
/*  sdkRunnerWorker.getTransportedObject().handle() method              */

/* ******************************************************************** */
package io.camunda.cherry.runner.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.definition.connector.SdkRunnerConnector;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.SecretProvider;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * This job handler intercept the execution to the result
 */
public class ConnectorJobHandler implements JobHandler {
  private final AbstractConnector abstractConnector;
  private final SdkRunnerConnector sdkRunnerConnector;
  public Map<Long, StatusContainer> statusPerJob = new HashMap<>();
  Logger logger = LoggerFactory.getLogger(ConnectorJobHandler.class.getName());
  HistoryFactory historyFactory;

  final SecretProvider secretProvider;
  final ValidationProvider validationProvider;
  final ObjectMapper objectMapper;

  public ConnectorJobHandler(AbstractConnector abstractConnector,
                             HistoryFactory historyFactory,
                             SecretProvider secretProvider,
                             ValidationProvider validationProvider,
                             ObjectMapper objectMapper) {
    this.abstractConnector = abstractConnector;
    this.sdkRunnerConnector = null;
    this.historyFactory = historyFactory;
    this.secretProvider = secretProvider;
    this.validationProvider = validationProvider;
    this.objectMapper = objectMapper;
  }

  public ConnectorJobHandler(SdkRunnerConnector sdkRunnerConnector,
                             HistoryFactory historyFactory,
                             SecretProvider secretProvider,
                             ValidationProvider validationProvider,
                             ObjectMapper objectMapper) {
    this.sdkRunnerConnector = sdkRunnerConnector;
    this.abstractConnector = null;
    this.historyFactory = historyFactory;
    this.secretProvider = secretProvider;
    this.validationProvider = validationProvider;
    this.objectMapper = objectMapper;

  }

  @Override
  public void handle(JobClient client, ActivatedJob job) {
    Instant executionInstant = Instant.now();
    logger.info("ConnectorJobHancler: Handle JobId[{}] of [{}]", job.getKey(), sdkRunnerConnector.getType());
    long beginExecution = System.currentTimeMillis();
    StatusContainer status;
    ConnectorException connectorException = null;
    try {
      JobHandlerContext context = new JobHandlerContext(job, secretProvider, validationProvider, objectMapper);
      // Execute the connector now
      sdkRunnerConnector.getTransportedConnector().execute(context);

      status = statusPerJob.getOrDefault(job.getKey(), new StatusContainer(AbstractRunner.ExecutionStatusEnum.SUCCESS));
    } catch (ConnectorException ce) {
      status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.BPMNERROR, ce);
      connectorException = ce;
    } catch (Exception e) {
      status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.FAIL, e);
    }
    long endExecution = System.currentTimeMillis();

    logger.info("Connector[" + (abstractConnector != null ? abstractConnector.getName() : sdkRunnerConnector.getName())
        + "] executed in " + (endExecution - beginExecution) + " ms");
    synchronized (statusPerJob) {
      statusPerJob.remove(job.getKey());
    }
    String type = abstractConnector != null ? abstractConnector.getType() : sdkRunnerConnector.getType();
    String errorCode = null;
    String errorMessage = null;
    if (status.bpmnError != null) {
      errorCode = status.bpmnError.getCode();
      errorMessage = status.bpmnError.getExplanation();
    }
    if (status.exception != null) {
      errorCode = "Exception";
      errorMessage = status.exception.getMessage();
    }
    historyFactory.saveExecution(executionInstant, // this instance
        RunnerExecutionEntity.TypeExecutor.CONNECTOR, // this is a connector
        type, // type of connector
        status.status, // status of execution
        errorCode, errorMessage, // error
        endExecution - beginExecution);
  }

  private class StatusContainer {
    AbstractRunner.ExecutionStatusEnum status;
    BpmnError bpmnError;
    Exception exception;

    StatusContainer(AbstractRunner.ExecutionStatusEnum status) {
      this.status = status;
    }

    StatusContainer(AbstractRunner.ExecutionStatusEnum status, BpmnError bpmnError) {
      this.status = status;
      this.bpmnError = bpmnError;
    }

    StatusContainer(AbstractRunner.ExecutionStatusEnum status, Exception exception) {
      this.status = status;
      this.exception = exception;
    }
  }

}
