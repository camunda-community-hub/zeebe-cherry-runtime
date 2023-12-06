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
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.SecretProvider;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class CherryWorkerJobHandler implements JobHandler {

  SdkRunnerWorker sdkRunnerWorker ;
  HistoryFactory historyFactory;
  Logger logger = LoggerFactory.getLogger(CherryWorkerJobHandler.class.getName());

  public CherryWorkerJobHandler(SdkRunnerWorker sdkRunnerWorker,
                                HistoryFactory historyFactory,
                                SecretProvider secretProvider) {
    this.sdkRunnerWorker = sdkRunnerWorker;
    this.historyFactory = historyFactory;
  }

  @Override
  public void handle(JobClient client, ActivatedJob job) {
    Instant executionInstant = Instant.now();
    logger.info("WorkerJobHandler: Handle JobId[{}] TenantId[{}] type[{}]", job.getKey(), job.getTenantId(), sdkRunnerWorker.getType());
    long beginExecution = System.currentTimeMillis();

    ConnectorException connectorException = null;
    try {
      Class sdkRunnerClass = sdkRunnerWorker.getTransportedObject().getClass();
      sdkRunnerWorker.getHandleMethod().invoke( sdkRunnerWorker.getTransportedObject(),
          client, job);

// the worker complete fail or throw a BPMN error: there is no way to knows what's happenned
    } catch (Exception e) {
      logger.error("Worker[{}] failed {}" + sdkRunnerWorker.getName(), e.toString() );
   }

    long endExecution = System.currentTimeMillis();

      logger.info("Worker[{}] executed in {} ms" ,
          sdkRunnerWorker.getName(),
          endExecution - beginExecution);
      String type = sdkRunnerWorker.getType();
      historyFactory.saveExecution(executionInstant, // this instance
          RunnerExecutionEntity.TypeExecutor.CONNECTOR, // this is a connector
          type, // type of connector
          AbstractRunner.ExecutionStatusEnum.SUCCESS, // status of execution
          null, null, // error
          endExecution - beginExecution);
    }

  }
