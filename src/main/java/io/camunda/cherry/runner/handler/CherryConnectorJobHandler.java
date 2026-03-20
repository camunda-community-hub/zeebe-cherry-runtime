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
import io.camunda.cherry.runtime.CherrySecretProvider;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobHandler;
import io.camunda.client.jobhandling.CommandExceptionHandlingStrategy;
import io.camunda.client.metrics.DefaultNoopMetricsRecorder;
import io.camunda.connector.api.document.DocumentFactory;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.document.DocumentFactoryImpl;
import io.camunda.connector.runtime.core.document.store.CamundaDocumentStore;
import io.camunda.connector.runtime.core.document.store.CamundaDocumentStoreImpl;
import io.camunda.connector.runtime.metrics.ConnectorsOutboundMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * This job handler intercept the execution to the result
 */
public class CherryConnectorJobHandler implements JobHandler {
    final CherrySecretProvider cherrySecretProvider;
    final ValidationProvider validationProvider;
    final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
    final ObjectMapper objectMapper;
    private final AbstractConnector abstractConnector;
    private final SdkRunnerConnector sdkRunnerConnector;
    CamundaClient camundaClient;
    CamundaDocumentStore documentStore;
    DocumentFactory documentFactory;
    Logger logger = LoggerFactory.getLogger(CherryConnectorJobHandler.class.getName());
    HistoryFactory historyFactory;

    public CherryConnectorJobHandler(AbstractConnector abstractConnector,
                                     HistoryFactory historyFactory,
                                     CherrySecretProvider cherrySecretProvider,
                                     ValidationProvider validationProvider,
                                     CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
                                     CamundaClient camundaClient,
                                     DocumentFactory documentFactory,
                                     ObjectMapper objectMapper) {
        this.abstractConnector = abstractConnector;
        this.sdkRunnerConnector = null;
        this.historyFactory = historyFactory;
        this.cherrySecretProvider = cherrySecretProvider;
        this.validationProvider = validationProvider;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
        this.camundaClient = camundaClient;
        this.documentFactory = documentFactory;
        this.objectMapper = objectMapper;
        documentStore = new CamundaDocumentStoreImpl(camundaClient);
        documentFactory = new DocumentFactoryImpl(documentStore);

    }

    public CherryConnectorJobHandler(SdkRunnerConnector sdkRunnerConnector,
                                     HistoryFactory historyFactory,
                                     CherrySecretProvider cherrySecretProvider,
                                     ValidationProvider validationProvider,
                                     CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
                                     DocumentFactory documentFactory,
                                     ObjectMapper objectMapper) {
        this.sdkRunnerConnector = sdkRunnerConnector;
        this.abstractConnector = null;
        this.historyFactory = historyFactory;
        this.cherrySecretProvider = cherrySecretProvider;
        this.validationProvider = validationProvider;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
        this.documentFactory = documentFactory;
        this.objectMapper = objectMapper;

    }

    @Override
    public void handle(JobClient client, ActivatedJob job) {
        Instant executionInstant = Instant.now();
        // abstractConnector or sdkRunnerConnector is not null
        String type = abstractConnector != null ? abstractConnector.getType() : sdkRunnerConnector.getType();
        logger.info("ConnectorJobHandler: Handle JobId[{}] TenantId[{}] of type[{}]", job.getKey(), job.getTenantId(),
                type);
        long beginExecution = System.currentTimeMillis();
        StatusContainer status;
        ConnectorException connectorException = null;

        try {
            // JobHandlerContext context = new JobHandlerContext(job, secretProvider, validationProvider, objectMapper);
            // Execute the connector now
            OutboundConnectorFunction connectorFunction = null;
            if (abstractConnector != null)
                connectorFunction = abstractConnector;
            else if (sdkRunnerConnector != null) {
                connectorFunction = sdkRunnerConnector.getTransportedConnector();
            } else
                throw new ConnectorException("Can't execute Connector : abstractConnector and sdkRunnerConnector are null");

            DefaultNoopMetricsRecorder jobWorkerMetrics = new DefaultNoopMetricsRecorder();
            SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

            ConnectorsOutboundMetrics outboundMetrics = new ConnectorsOutboundMetrics(meterRegistry);

            SuperConnectorJobHandler connectorJobHandler = new SuperConnectorJobHandler(connectorFunction,
                    outboundMetrics,
                    jobWorkerMetrics,
                    cherrySecretProvider,
                    validationProvider,
                    commandExceptionHandlingStrategy,
                    documentFactory,
                    objectMapper);
            connectorJobHandler.handle(client, job);
            status = new StatusContainer(connectorJobHandler.getExecutionStatus());
            status.exception = connectorJobHandler.getLogException();

        } catch (ConnectorException ce) {
            status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.BPMNERROR, ce);
            connectorException = ce;
        } catch (Exception e) {
            status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.FAIL, e);
        }
        long endExecution = System.currentTimeMillis();

        logger.info("Connector[" + (abstractConnector != null ? abstractConnector.getName() : sdkRunnerConnector.getName())
                + "] executed in " + (endExecution - beginExecution) + " ms");
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
