package io.camunda.cherry.definition;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.runtime.HistoryFactory;
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
  public Map<Long, AbstractRunner.ExecutionStatusEnum> statusPerJob = new HashMap<>();
  Logger logger = LoggerFactory.getLogger(CherryConnectorJobHandler.class.getName());
  HistoryFactory historyFactory;

  public CherryConnectorJobHandler(AbstractConnector abstractConnector, HistoryFactory historyFactory) {
    super(abstractConnector);
    this.abstractConnector = abstractConnector;
    this.sdkRunnerConnector = null;
    this.historyFactory = historyFactory;
  }

  public CherryConnectorJobHandler(SdkRunnerConnector sdkRunnerConnector, HistoryFactory historyFactory) {
    super(sdkRunnerConnector.getTransportedConnector());
    this.sdkRunnerConnector = sdkRunnerConnector;
    this.abstractConnector = null;
    this.historyFactory = historyFactory;
  }

  @Override
  public void handle(JobClient client, ActivatedJob job) {
    Instant executionInstant = Instant.now();
    long beginExecution = System.currentTimeMillis();
    AbstractRunner.ExecutionStatusEnum status;
    ConnectorException connectorException = null;
    try {
      super.handle(client, job);
      status = statusPerJob.getOrDefault(job.getKey(), AbstractRunner.ExecutionStatusEnum.SUCCESS);
    } catch (ConnectorException ce) {
      status = AbstractRunner.ExecutionStatusEnum.BPMNERROR;
      connectorException = ce;
    } catch (Exception e) {
      status = AbstractRunner.ExecutionStatusEnum.FAIL;
    }
    long endExecution = System.currentTimeMillis();

    logger.info("Connector[" + (abstractConnector != null ? abstractConnector.getName() : sdkRunnerConnector.getName())
        + "] executed in " + (endExecution - beginExecution) + " ms");
    synchronized (statusPerJob) {
      statusPerJob.remove(job.getKey());
    }
    String type = abstractConnector != null ? abstractConnector.getType() : sdkRunnerConnector.getType();
    historyFactory.saveExecution(executionInstant, // this instance
        RunnerExecutionEntity.TypeExecutor.CONNECTOR, // this is a connector
        type, // type of connector
        status, // status of execution
        connectorException, // error
        endExecution - beginExecution);

  }

  @Override
  protected void failJob(JobClient client, ActivatedJob job, Exception exception) {
    synchronized (statusPerJob) {
      statusPerJob.put(job.getKey(), AbstractRunner.ExecutionStatusEnum.FAIL);
    }
    super.failJob(client, job, exception);
  }

  @Override
  protected void throwBpmnError(JobClient client, ActivatedJob job, BpmnError value) {
    synchronized (statusPerJob) {
      statusPerJob.put(job.getKey(), AbstractRunner.ExecutionStatusEnum.BPMNERROR);
    }
    super.throwBpmnError(client, job, value);
  }

  @Override
  protected void completeJob(JobClient client, ActivatedJob job, ConnectorResult result) {

    synchronized (statusPerJob) {
      statusPerJob.put(job.getKey(), AbstractRunner.ExecutionStatusEnum.SUCCESS);
    }
    super.completeJob(client, job, result);

  }
}
