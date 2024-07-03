package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWatcher;
import io.camunda.cherry.definition.WatcherOrderInformation;
import io.camunda.cherry.zeebe.ZeebeContainer;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "watcher")
public class WatcherFactory {

  private static final Logger logger = LoggerFactory.getLogger(WatcherFactory.class.getName());
  private final List<AbstractWatcher.WatcherExecution> listWatchersExecution = new ArrayList<>();
  @Autowired
  HistoryFactory historyFactory;
  @Autowired
  ZeebeContainer zeebeContainer;
  @Autowired
  WatcherPropertyList watcherPropertyList;
  @Autowired
  private List<AbstractWatcher> listDefinitionWatchers;

  WatcherFactory() {
  }

  @PostConstruct
  public void init() {
    for (AbstractWatcher watcher : listDefinitionWatchers) {
      watcher.setWatcherFactory(this);
    }

    // Read all Watcher to starts
    for (Map<String, Object> record : watcherPropertyList.getExecution()) {

      AbstractWatcher.WatcherExecution watcherExecution = getWatcherFromRecord(record);
      if (watcherExecution != null) {
        logger.info("WatcherFactory: detect Watcher [" + watcherExecution.getName() + "]");
        listWatchersExecution.add(watcherExecution);
      }
    }

    initialiseExecution();
  }

  public void initialiseExecution() {
    // Create each watcher
    for (AbstractWatcher.WatcherExecution watcherExecution : listWatchersExecution) {

      watcherExecution.getWatcher().start(watcherExecution);

      if (watcherExecution.getWatcher().needTourOfDuty()) {
        // We create one schedule executor per watcher
        watcherExecution.setExecutor(Executors.newSingleThreadScheduledExecutor());
        if (watcherExecution.getWatcher().isRunning(watcherExecution)) {
          WatcherTimerExecution watcherTimerExecution = new WatcherTimerExecution(watcherExecution, this);

          watcherExecution.getExecutor()
              .scheduleAtFixedRate(watcherTimerExecution, 0, Long.valueOf(
                      watcherExecution.getParameter(AbstractWatcher.WatcherParameter.TIMEBETWEENDUTYINMS).toString()),
                  TimeUnit.MILLISECONDS);
        }
      }
    }
  }

  /* execute a list of order from a watcher

   */
  public void executeOrders(AbstractWatcher.WatcherExecution watcherExecution,
                            List<WatcherOrderInformation> listOrdersInformation) {

    for (WatcherOrderInformation orderInformation : listOrdersInformation) {
      Instant executionInstant = Instant.now();
      long beginExecution = System.currentTimeMillis();
      AbstractRunner.ExecutionStatusEnum status = AbstractRunner.ExecutionStatusEnum.SUCCESS;
      String errorCode = null;
      String errorMessage = null;
      ConnectorException connectorException = null;
      try {
        switch (orderInformation.orderAction) {
        case CREATEPROCESSINSTANCEPERID -> createProcessInstance(orderInformation);
        }
        watcherExecution.getWatcher().orderExecuted(watcherExecution, orderInformation, true);
      } catch (ConnectorException ce) {
        watcherExecution.getWatcher().orderExecuted(watcherExecution, orderInformation, false);
        status = AbstractRunner.ExecutionStatusEnum.BPMNERROR;
        errorCode = ce.getErrorCode();
        errorMessage = ce.getMessage();
        connectorException = ce;
      } catch (Exception e) {
        watcherExecution.getWatcher().orderExecuted(watcherExecution, orderInformation, false);
        status = AbstractRunner.ExecutionStatusEnum.FAIL;
        errorCode = "Exception";
        errorMessage = e.getMessage();
      }
      long endExecution = System.currentTimeMillis();
      logger.info(
          "Watcher[" + watcherExecution.getWatcher().getName() + "] executed in " + (endExecution - beginExecution)
              + " ms for " + listOrdersInformation.size() + " orders");

      if (!listOrdersInformation.isEmpty()) {
        historyFactory.saveExecution(executionInstant, RunnerExecutionEntity.TypeExecutor.WATCHER,
            watcherExecution.getWatcher().getType(), status, errorCode, errorMessage, endExecution - beginExecution);
      }
    }
  }

  /**
   * Create a process instance
   *
   * @param orderInformation order to create the process instance
   * @throws Exception exception during creation
   */
  private void createProcessInstance(WatcherOrderInformation orderInformation) throws Exception {
    logger.info("WatcherFactory.createProcessInstance: Start orderLabel[" + orderInformation.getLabel() + "]");
    try {
      ZeebeFuture<ProcessInstanceEvent> zeebeFuture = zeebeContainer.getZeebeClient()
          .newCreateInstanceCommand()
          .bpmnProcessId(orderInformation.processId)
          .latestVersion()
          .variables(orderInformation.variables)
          .send();
      zeebeFuture.join();

      ProcessInstanceEvent response = zeebeFuture.get();
      logger.info("WatcherFactory.createProcessInstance: OrderLabel[" + orderInformation.getLabel() + "] response="
          + response.toString());

    } catch (Exception e) {
      logger.info("WatcherFactory.createProcessInstance  ControllerPage=" + e.getMessage());
      throw e;
    }
  }

  public void executeTask(WatcherOrderInformation orderInformation) throws Exception {
    // to be developed
  }

  /**
   * @param type type searched
   * @return the Watcher, null if not found
   */
  public AbstractWatcher getByType(String type) {
    Optional<AbstractWatcher> first = listDefinitionWatchers.stream().filter(t -> t.getType().equals(type)).findFirst();
    return first.isPresent() ? first.get() : null;
  }

  /**
   * @param record information to create teh WatcherExecution
   * @return a Watcher Execution
   */
  private AbstractWatcher.WatcherExecution getWatcherFromRecord(Map<String, Object> record) throws ConnectorException {
    AbstractWatcher watcher = null;
    String name = null;
    AbstractWatcher.WatcherAction action = AbstractWatcher.WatcherAction.CREATEPROCESSINSTANCEPERID;
    Map<String, Object> inputs = new HashMap<>();
    Map<AbstractWatcher.WatcherParameter, Object> parameters = new HashMap<>();
    // populate it
    for (Map.Entry<String, Object> entry : record.entrySet()) {
      if (entry.getKey().equals("type")) {
        watcher = getByType((String) record.get("type"));
        if (watcher == null) {
          throw new ConnectorException("UNKNOW_WATCHER", "Unknow watcher [" + record.get("type") + "]");
        }
        continue;
      }
      if (entry.getKey().equals("action")) {
        try {
          action = AbstractWatcher.WatcherAction.valueOf(entry.getValue().toString());
        } catch (Exception e) {
          throw new ConnectorException("UNKNOW_ACTION", "Action [" + entry.getValue() + "] is an unknown action");
        }
        continue;
      }
      if (entry.getKey().equals("name")) {
        name = entry.getValue().toString();
        continue;
      }
      AbstractWatcher.WatcherParameter parameter = null;
      try {
        parameter = AbstractWatcher.WatcherParameter.valueOf(entry.getKey().toUpperCase());
        parameters.put(parameter, entry.getValue());
      } catch (Exception e) {
        // exception? No worry, it was't a parameter then.
        inputs.put(entry.getKey(), entry.getValue());
      }
    }
    if (watcher == null) {
      throw new ConnectorException("WATCHER_NOT_SPECIFIED", "A watcher must by specify with [type] attribute");
    }

    return watcher.createExecution(name, action, inputs, parameters);
  }

  /**
   * Execute a task at a specific time
   */
  public static class WatcherTimerExecution extends TimerTask {
    AbstractWatcher.WatcherExecution watcherExecution;
    WatcherFactory watcherFactory;

    public WatcherTimerExecution(AbstractWatcher.WatcherExecution watcherExecution, WatcherFactory watcherFactory) {
      this.watcherExecution = watcherExecution;
      this.watcherFactory = watcherFactory;
    }

    /**
     * execute: collect the listOfOrder and execute them
     */
    public void run() {
      logger.debug("Start Tour Or Duty[" + watcherExecution.getName() + "]");
      List<WatcherOrderInformation> listOrderInformation = watcherExecution.getWatcher().tourOfDuty(watcherExecution);
      this.watcherFactory.executeOrders(watcherExecution, listOrderInformation);
      logger.info("End Tour Or Duty [" + watcherExecution.getName() + "] - " + listOrderInformation.size()
          + " orders executed");
    }
  }
}
