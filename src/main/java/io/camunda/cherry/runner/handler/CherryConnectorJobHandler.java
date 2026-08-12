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
import io.camunda.connector.api.annotation.Operation;
import io.camunda.connector.api.annotation.Variable;
import io.camunda.connector.api.document.DocumentFactory;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.outbound.OutboundConnectorProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.document.DocumentFactoryImpl;
import io.camunda.connector.runtime.core.document.store.CamundaDocumentStore;
import io.camunda.connector.runtime.core.document.store.CamundaDocumentStoreImpl;
import io.camunda.connector.runtime.metrics.ConnectorsOutboundMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

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
    public void handle(JobClient client, ActivatedJob job) throws Exception {
        Instant executionInstant = Instant.now();
        // abstractConnector or sdkRunnerConnector is not null
        String type = abstractConnector != null ? abstractConnector.getType() : sdkRunnerConnector.getType();
        logger.info("ConnectorJobHandler: Handle JobId[{}] TenantId[{}] of type[{}]",
                job.getKey(),
                job.getTenantId(),
                type);
        long beginExecution = System.currentTimeMillis();
        StatusContainer status;
        ConnectorException connectorException = null;
        Exception exception = null;

        try {
            // JobHandlerContext context = new JobHandlerContext(job, secretProvider, validationProvider, objectMapper);
            // Execute the connector now
            OutboundConnectorFunction connectorFunction = null;
            OutboundConnectorProvider connectorProvider = null;
            if (abstractConnector != null)
                connectorFunction = abstractConnector;
            else if (sdkRunnerConnector != null) {
                connectorFunction = sdkRunnerConnector.getTransportedConnectorFunction();
                connectorProvider = sdkRunnerConnector.getTransportedConnectorProvider();
            } else
                throw new ConnectorException("Can't execute Connector : abstractConnector and sdkRunnerConnector are null");


            if (connectorFunction != null) {
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

                // --------------- call the handle method
                connectorJobHandler.handle(client, job);


                status = new StatusContainer(connectorJobHandler.getExecutionStatus());
                status.exception = connectorJobHandler.getLogException();
            } else if (connectorProvider != null) {
                // Handle OutboundConnectorProvider by discovering and invoking the operation method
                String operationType = job.getCustomHeaders().get("operation");
                if (operationType == null) {
                    throw new ConnectorException("No operationType header found for OutboundConnectorProvider");
                }

                // Find the @Operation method matching this type
                Method operationMethod = findOperationMethod(connectorProvider, operationType);
                if (operationMethod == null) {
                    throw new ConnectorException("No @Operation method found for type: " + operationType + " in provider: " + connectorProvider.getClass().getName());
                }

                // --------------- call the handle method
                // Execute the provider operation directly with job variables
                // OutboundConnectorProvider requires direct invocation since its @Operation methods
                // expect @Variable-annotated parameters that are populated by the provider framework
                Object providerResult = invokeProviderOperation(connectorProvider, operationMethod, job);

                status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.SUCCESS);

            } else {
                throw new ConnectorException("No connector function or provider available to execute");
            }

        } catch (ConnectorException ce) {
            logger.error("ConnectorJobHandler : catch ConnectorException", ce);
            status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.BPMNERROR, ce);
            connectorException = ce;
        } catch (Exception e) {
            logger.error("ConnectorJobHandler : catch Exception", e);
            status = new StatusContainer(AbstractRunner.ExecutionStatusEnum.FAIL, e);
            exception = e;
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
        // ------------ if an exception is catch, time to throw it
        if (connectorException != null) {
            throw connectorException;
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Find the @Operation method on the provider that matches the operation type
     */
    private Method findOperationMethod(OutboundConnectorProvider provider, String operationType) {
        for (Method method : provider.getClass().getMethods()) {
            Operation operation = method.getAnnotation(Operation.class);
            if (operation != null && operation.id().equals(operationType)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Invoke an OutboundConnectorProvider's @Operation method directly with job variables.
     * This extracts variables from the job and populates the method parameters.
     */
    private Object invokeProviderOperation(OutboundConnectorProvider provider, Method operationMethod, ActivatedJob job) throws ConnectorException {
        try {
            // Get method parameters and types
            Class<?>[] paramTypes = operationMethod.getParameterTypes();
            java.lang.reflect.Parameter[] methodParams = operationMethod.getParameters();
            Object[] params = new Object[paramTypes.length];

            // Get all job variables
            Map<String, Object> jobVariables = job.getVariablesAsMap();
            logger.info("Job variables available: {}", jobVariables.keySet());

            // Populate method parameters from job variables or context
            for (int i = 0; i < paramTypes.length; i++) {
                String paramName = methodParams[i].getName();

                // Check for @Variable annotation which might specify a custom variable name
                Variable varAnnotation = methodParams[i].getAnnotation(Variable.class);

                if (varAnnotation != null && !varAnnotation.value().isEmpty()) {
                    paramName = varAnnotation.value();
                }

                // Get the variable value from job variables
                Object varValue = jobVariables.get(paramName);

                logger.info("Parameter[{}] name='{}' type='{}' varValue={}",
                        i, paramName, paramTypes[i].getSimpleName(), varValue != null ? "found" : "null");

                if (varValue == null) {
                    logger.debug("Variable '{}' not found in job for method parameter '{}'", paramName, methodParams[i].getName());
                    params[i] = null;
                } else {
                    // Deserialize the variable to the expected type
                    try {
                        params[i] = objectMapper.convertValue(varValue, paramTypes[i]);
                        logger.info("Successfully deserialized variable '{}' to type {}", paramName, paramTypes[i].getSimpleName());
                    } catch (Exception deserializeError) {
                        logger.warn("Failed to deserialize variable '{}' to type {}: {}",
                                paramName, paramTypes[i].getName(), deserializeError.getMessage());
                        throw new ConnectorException("Failed to deserialize variable '" + paramName + "': " + deserializeError.getMessage());
                    }
                }
            }

            // Invoke the operation method
            return operationMethod.invoke(provider, params);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ConnectorException) {
                throw (ConnectorException) e.getCause();
            }
            throw new ConnectorException("Failed to invoke operation: " + operationMethod.getName(), e.getCause());
        } catch (ConnectorException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConnectorException("Failed to invoke operation: " + operationMethod.getName(), e);
        }
    }

    private static class StatusContainer {
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
