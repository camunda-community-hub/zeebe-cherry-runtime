/* ******************************************************************** */
/*                                                                      */
/*  WorkerJobHandler                                                    */
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

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.connector.SdkRunnerWorker;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runtime.CherrySecretProvider;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.zeebe.ZeebeContainer;
import io.camunda.client.annotation.AnnotationUtil;
import io.camunda.client.annotation.value.JobWorkerValue;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobHandler;
import io.camunda.client.bean.BeanInfo;
import io.camunda.client.bean.MethodInfo;
import io.camunda.client.jobhandling.BeanJobHandlerFactory;
import io.camunda.client.jobhandling.CommandExceptionHandlingStrategy;
import io.camunda.client.jobhandling.JobHandlerFactory;
import io.camunda.client.jobhandling.parameter.DefaultParameterResolverStrategy;
import io.camunda.client.jobhandling.result.DefaultDocumentResultProcessorFailureHandlingStrategy;
import io.camunda.client.jobhandling.result.DefaultResultProcessorStrategy;
import io.camunda.client.metrics.DefaultNoopMetricsRecorder;
import io.camunda.connector.api.secret.SecretContext;
import io.camunda.connector.runtime.core.secret.SecretFilter;
import io.camunda.connector.runtime.core.secret.SecretHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A @JobWorker method can declare any parameter shape Camunda's own annotation-processing
 * supports (@Variable, @VariablesAsType, JobClient, ActivatedJob, @CustomHeaders, Document
 * types, key parameters, ...), not just (JobClient, ActivatedJob). Reimplementing that
 * resolution logic here would mean re-inventing (and keeping in sync with) a sizeable, fast
 * moving part of Camunda's client. Instead, this class builds the same MethodInfo/JobWorkerValue
 * that camunda-spring-boot-starter builds for its own @JobWorker beans, and delegates to
 * Camunda's own io.camunda.client.jobhandling.BeanJobHandlerFactory to get a fully working
 * JobHandler - parameter resolution, invocation and job completion/failure are all handled by
 * Camunda's code, not Cherry's.
 */
public class CherryWorkerJobHandler implements JobHandler {

    private final SdkRunnerWorker sdkRunnerWorker;
    private final HistoryFactory historyFactory;
    private final JobHandler delegate;
    private final CherrySecretProvider cherrySecretProvider;
    private final SecretHandler secretHandler;
    Logger logger = LoggerFactory.getLogger(CherryWorkerJobHandler.class.getName());

    public CherryWorkerJobHandler(SdkRunnerWorker sdkRunnerWorker,
                                  HistoryFactory historyFactory,
                                  CherrySecretProvider cherrySecretProvider,
                                  CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
                                  ZeebeContainer zeebeContainer) {
        this.sdkRunnerWorker = sdkRunnerWorker;
        this.historyFactory = historyFactory;
        this.cherrySecretProvider = cherrySecretProvider;
        this.secretHandler = new SecretHandler(cherrySecretProvider, SecretFilter.allowAll());

        BeanInfo beanInfo = BeanInfo.builder()
                .bean(sdkRunnerWorker.getTransportedObject())
                .targetClass(sdkRunnerWorker.getTransportedObject().getClass())
                .beanName(sdkRunnerWorker.getName())
                .build();
        MethodInfo methodInfo = MethodInfo.builder()
                .beanInfo(beanInfo)
                .method(sdkRunnerWorker.getHandleMethod())
                .build();

        // Re-derive the JobWorkerValue from the @JobWorker annotation on the method itself -
        // the same way camunda-spring-boot-starter does it for its own annotated beans.
        JobWorkerValue jobWorkerValue = AnnotationUtil.getJobWorkerValue(methodInfo)
                .orElseThrow(() -> new TechnicalException(
                        "Method [" + sdkRunnerWorker.getHandleMethod() + "] is not annotated with @JobWorker"));

        BeanJobHandlerFactory handlerFactory = new BeanJobHandlerFactory(
                methodInfo,
                commandExceptionHandlingStrategy,
                new DefaultParameterResolverStrategy(zeebeContainer.getZeebeClient().getConfiguration().getJsonMapper()),
                new DefaultResultProcessorStrategy(new DefaultDocumentResultProcessorFailureHandlingStrategy()),
                new DefaultNoopMetricsRecorder());

        this.delegate = handlerFactory.getJobHandler(
                new JobHandlerFactory.JobHandlerFactoryContext(jobWorkerValue, zeebeContainer.getZeebeClient()));
    }

    @Override
    public void handle(JobClient client, ActivatedJob job) throws Exception {
        Instant executionInstant = Instant.now();
        logger.info("WorkerJobHandler: Handle JobId[{}] TenantId[{}] type[{}] ProcessInstance[{}]", job.getKey(), job.getTenantId(),
                sdkRunnerWorker.getType(), job.getProcessInstanceKey());
        long beginExecution = System.currentTimeMillis();

        AbstractRunner.ExecutionStatusEnum status = AbstractRunner.ExecutionStatusEnum.SUCCESS;
        Exception exception = null;
        try {
            delegate.handle(client, resolveSecrets(job));
        } catch (Exception e) {
            status = AbstractRunner.ExecutionStatusEnum.FAIL;
            exception = e;
            logger.error("Worker[{}] failed", sdkRunnerWorker.getName(), e);
        }

        long endExecution = System.currentTimeMillis();
        logger.info("Worker[{}] executed in {} ms", sdkRunnerWorker.getName(), endExecution - beginExecution);

        historyFactory.saveExecution(executionInstant, // this instance
                RunnerExecutionEntity.TypeExecutor.CONNECTOR, // this is a connector
                sdkRunnerWorker.getType(), // type of connector
                status, // status of execution
                exception != null ? "Exception" : null,
                exception != null ? exception.getMessage() : null,
                endExecution - beginExecution);

        // let the exception propagate so Camunda's own worker/retry machinery still sees it
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Camunda's BeanJobHandlerFactory resolves @Variable parameters by calling
     * ActivatedJob.getVariablesAsMap()/getVariable(String) - neither of those knows about
     * Cherry's {{secrets.xxx}} convention, which is otherwise only wired up for connectors
     * (see CherrySecretProvider). Wrap the job so those two methods return variables run
     * through the same SecretHandler the connector runtime uses, and delegate everything
     * else unchanged.
     */
    private ActivatedJob resolveSecrets(ActivatedJob job) {
        SecretContext secretContext = new SecretContext(job.getTenantId(), job.getBpmnProcessId());
        Map<String, Object> resolvedVariables = new HashMap<>();
        job.getVariablesAsMap().forEach((name, value) ->
                resolvedVariables.put(name, value instanceof String stringValue
                        ? secretHandler.replaceSecrets(stringValue, secretContext)
                        : value));

        return (ActivatedJob) Proxy.newProxyInstance(
                ActivatedJob.class.getClassLoader(),
                new Class<?>[]{ActivatedJob.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getVariablesAsMap":
                            return resolvedVariables;
                        case "getVariable":
                            return resolvedVariables.get((String) args[0]);
                        default:
                            try {
                                return method.invoke(job, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                    }
                });
    }

}
