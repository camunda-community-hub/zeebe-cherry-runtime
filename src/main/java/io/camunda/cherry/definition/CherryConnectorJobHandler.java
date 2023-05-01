package io.camunda.cherry.definition;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.SecretProvider;
import io.camunda.connector.api.error.BpmnError;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.runtime.util.outbound.ConnectorJobHandler;
import io.camunda.connector.runtime.util.outbound.ConnectorResult;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * This job handler intercept the execution to the result
 */
public class CherryConnectorJobHandler extends ConnectorJobHandler {
  private final AbstractConnector abstractConnector;
  private final SdkRunnerConnector sdkRunnerConnector;
  public Map<Long, StatusContainer> statusPerJob = new HashMap<>();
  Logger logger = LoggerFactory.getLogger(CherryConnectorJobHandler.class.getName());
  HistoryFactory historyFactory;

  public CherryConnectorJobHandler(AbstractConnector abstractConnector,
                                   HistoryFactory historyFactory,
                                   SecretProvider secretProvider) {
    super(abstractConnector, secretProvider);
    this.abstractConnector = abstractConnector;
    this.sdkRunnerConnector = null;
    this.historyFactory = historyFactory;
  }

  public CherryConnectorJobHandler(SdkRunnerConnector sdkRunnerConnector,
                                   HistoryFactory historyFactory,
                                   SecretProvider secretProvider) {
    super(sdkRunnerConnector.getTransportedConnector(), secretProvider);
    this.sdkRunnerConnector = sdkRunnerConnector;
    this.abstractConnector = null;
    this.historyFactory = historyFactory;
  }

  @Override
  public void handle(JobClient client, ActivatedJob job) {
    Instant executionInstant = Instant.now();
    long beginExecution = System.currentTimeMillis();
    StatusContainer status;
    ConnectorException connectorException = null;
    try {
      super.handle(client, job);
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
      errorMessage = status.bpmnError.getMessage();
    }
    if (status.exception != null) {
      errorCode = "Exception";
      errorMessage = status.exception.getMessage();
    }
    historyFactory.saveExecution(executionInstant, // this instance
        RunnerExecutionEntity.TypeExecutor.CONNECTOR, // this is a connector
        type, // type of connector
        status.status, // status of execution
        errorCode, errorMessage,// error
        endExecution - beginExecution);

  }

  @Override
  protected void failJob(JobClient client, ActivatedJob job, Exception exception) {
    synchronized (statusPerJob) {
      statusPerJob.put(job.getKey(), new StatusContainer(AbstractRunner.ExecutionStatusEnum.FAIL, exception));
    }
    super.failJob(client, job, exception);
  }

  @Override
  protected void throwBpmnError(JobClient client, ActivatedJob job, BpmnError value) {
    synchronized (statusPerJob) {
      statusPerJob.put(job.getKey(), new StatusContainer(AbstractRunner.ExecutionStatusEnum.BPMNERROR, value));
    }
    super.throwBpmnError(client, job, value);
  }

  @Override
  protected void completeJob(JobClient client, ActivatedJob job, ConnectorResult result) {

    synchronized (statusPerJob) {
      statusPerJob.put(job.getKey(), new StatusContainer(AbstractRunner.ExecutionStatusEnum.SUCCESS));
    }
    super.completeJob(client, job, result);

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
