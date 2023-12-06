/* ******************************************************************** */
/*                                                                      */
/*  SuperConnectorJobHandler                                            */
/*                                                                      */
/*  We superside the connector job handler to save the result of the    */
/*   execution
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.runner.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.secret.SecretProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.error.BpmnError;
import io.camunda.connector.runtime.core.outbound.ConnectorJobHandler;
import io.camunda.connector.runtime.core.outbound.ConnectorResult;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

public class SuperConnectorJobHandler extends ConnectorJobHandler {

  private AbstractRunner.ExecutionStatusEnum executionStatus;
  private Exception logException;

  public SuperConnectorJobHandler(OutboundConnectorFunction call, SecretProvider secretProvider, ValidationProvider validationProvider, ObjectMapper objectMapper) {
    super(call, secretProvider, validationProvider, objectMapper);
  }


  protected void completeJob(JobClient client, ActivatedJob job, ConnectorResult.SuccessResult result) {
    executionStatus= AbstractRunner.ExecutionStatusEnum.SUCCESS;
    super.completeJob(client,job,result);
  }

  protected void failJob(JobClient client, ActivatedJob job, ConnectorResult.ErrorResult result) {
    executionStatus= AbstractRunner.ExecutionStatusEnum.FAIL;
    super.failJob(client, job, result);
  }

  protected void throwBpmnError(JobClient client, ActivatedJob job, BpmnError value) {
    executionStatus= AbstractRunner.ExecutionStatusEnum.BPMNERROR;
    super.throwBpmnError(client, job, value);
  }

  protected void logError(ActivatedJob job, Exception ex) {
    logException = ex;
    super.logError(job, ex);
  }

  public AbstractRunner.ExecutionStatusEnum getExecutionStatus() {
    return executionStatus;
  }

  public Exception getLogException() {
    return logException;
  }
}
