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
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.jobhandling.CommandExceptionHandlingStrategy;
import io.camunda.connector.api.document.DocumentFactory;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.secret.SecretProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.error.BpmnError;
import io.camunda.connector.runtime.core.outbound.ConnectorResult;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.metrics.ConnectorsOutboundMetrics;
import io.camunda.connector.runtime.outbound.job.SpringConnectorJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SuperConnectorJobHandler extends SpringConnectorJobHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperConnectorJobHandler.class);

    private AbstractRunner.ExecutionStatusEnum executionStatus;
    private Exception logException;


    public SuperConnectorJobHandler(
            OutboundConnectorFunction connectorFunction,
            ConnectorsOutboundMetrics outboundMetrics,
            io.camunda.client.metrics.DefaultNoopMetricsRecorder noopMetricsRecorder,
            SecretProvider secretProvider,
            ValidationProvider validationProvider,
            CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
            DocumentFactory documentFactory,
            ObjectMapper objectMapper) {

        super(outboundMetrics,
                commandExceptionHandlingStrategy,
                new SecretProviderAggregator(List.of(secretProvider)),
                validationProvider,
                documentFactory,
                objectMapper,
                connectorFunction,
                noopMetricsRecorder);
    }

    protected void completeJob(JobClient client, ActivatedJob job, ConnectorResult.SuccessResult result) {
        executionStatus = AbstractRunner.ExecutionStatusEnum.SUCCESS;
        super.completeJob(client, job, result);
    }

    protected void failJob(JobClient client, ActivatedJob job, ConnectorResult.ErrorResult result) {
        executionStatus = AbstractRunner.ExecutionStatusEnum.FAIL;
        super.failJob(client, job, result);
    }

    protected void throwBpmnError(JobClient client, ActivatedJob job, BpmnError value) {
        executionStatus = AbstractRunner.ExecutionStatusEnum.BPMNERROR;
        super.throwBpmnError(client, job, value);
    }

    protected void logError(ActivatedJob job, Exception ex) {
        logException = ex;
    }

    public AbstractRunner.ExecutionStatusEnum getExecutionStatus() {
        return executionStatus;
    }

    public Exception getLogException() {
        return logException;
    }
}
