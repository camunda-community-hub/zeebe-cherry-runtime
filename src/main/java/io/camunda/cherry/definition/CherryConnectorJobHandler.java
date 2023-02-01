package io.camunda.cherry.definition;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.runtime.CherryHistoricFactory;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.runtime.util.outbound.ConnectorJobHandler;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * This job handler intercept the execution to the result
 */
public class CherryConnectorJobHandler extends ConnectorJobHandler {
  private final AbstractConnector abstractConnector;
  Logger logger = LoggerFactory.getLogger(CherryConnectorJobHandler.class.getName());
  CherryHistoricFactory cherryHistoricFactory;

  public CherryConnectorJobHandler(AbstractConnector abstractConnector, CherryHistoricFactory cherryHistoricFactory) {
    super(abstractConnector);
    this.abstractConnector = abstractConnector;
    this.cherryHistoricFactory = cherryHistoricFactory;
  }

  @Override
  public void handle(JobClient client, ActivatedJob job) {
    Instant executionInstant = Instant.now();
    long beginExecution = System.currentTimeMillis();
    AbstractRunner.ExecutionStatusEnum status;
    ConnectorException connectorException = null;
    try {
      super.handle(client, job);
      status = AbstractRunner.ExecutionStatusEnum.SUCCESS;
    } catch (ConnectorException ce) {
      status = AbstractRunner.ExecutionStatusEnum.BPMNERROR;
      connectorException = ce;
    } catch (Exception e) {
      status = AbstractRunner.ExecutionStatusEnum.FAIL;
    }
    long endExecution = System.currentTimeMillis();
    logger.info(
        "Connector[" + abstractConnector.getName() + "] executed in " + (endExecution - beginExecution) + " ms");

    cherryHistoricFactory.saveExecution(executionInstant, // this instance
        RunnerExecutionEntity.TypeExecutor.CONNECTOR, // this is a connector
        abstractConnector.getType(), // type of connector
        status, // status of execution
        connectorException, // error
        endExecution - beginExecution);

  }

}
