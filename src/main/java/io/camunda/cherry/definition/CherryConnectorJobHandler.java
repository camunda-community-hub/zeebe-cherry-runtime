package io.camunda.cherry.definition;

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
  Logger logger = LoggerFactory.getLogger(CherryConnectorJobHandler.class.getName());

  CherryHistoricFactory cherryHistoricFactory;

  private AbstractConnector abstractConnector;

  public CherryConnectorJobHandler(AbstractConnector abstractConnector, CherryHistoricFactory cherryHistoricFactory) {
    super(abstractConnector);
    this.abstractConnector = abstractConnector;
    this.cherryHistoricFactory = cherryHistoricFactory;
  }

  public void handle(JobClient client, ActivatedJob job) {
    Instant executionInstant = Instant.now();
    long beginExecution = System.currentTimeMillis();
    AbstractRunner.ExecutionStatusEnum status;
    try {
      super.handle(client, job);
      status = AbstractRunner.ExecutionStatusEnum.SUCCESS;
    } catch (ConnectorException e) {
      status = AbstractRunner.ExecutionStatusEnum.BPMNERROR;
    } catch (Exception e) {
      status = AbstractRunner.ExecutionStatusEnum.FAIL;
    }
    long endExecution = System.currentTimeMillis();
    logger.info(
        "Connector[" + abstractConnector.getName() + "] executed in " + (endExecution - beginExecution) + " ms");

    cherryHistoricFactory.saveExecution(executionInstant, abstractConnector.getName(), status,
        endExecution - beginExecution);

  }

}
