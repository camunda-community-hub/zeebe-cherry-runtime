/* ******************************************************************** */
/*                                                                      */
/*  Cherry application                                                  */
/*                                                                      */
/*  RunnerInternalDetection. detect all internal runners, and update    */
/*  the internal database with all detection                            */
/* ******************************************************************** */

package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class RunnerEmbeddedFactory {

  Logger logger = LoggerFactory.getLogger(RunnerEmbeddedFactory.class.getName());

  /* Thanks to SpringBoot to detects all the AbstractConnector */ List<AbstractConnector> listAbstractConnector;

  /* Thanks to SpringBoot to detects all the AbstractWorker */ List<AbstractWorker> listAbstractWorker;

  /* A ZeebeConnector can be a simple Connector, with the @JobWorker annotations */
  @Autowired
  private ApplicationContext applicationContext;


  StorageRunner storageRunner;
  LogOperation logOperation;

  RunnerEmbeddedFactory(List<AbstractConnector> listAbstractConnector,
                        List<AbstractWorker> listAbstractWorker,
                        StorageRunner storageRunner,
                        LogOperation logOperation) {
    this.listAbstractConnector = listAbstractConnector;
    this.listAbstractWorker = listAbstractWorker;
    this.storageRunner = storageRunner;
    this.logOperation = logOperation;
  }

  public void registerInternalRunner() {
    List<AbstractRunner> listClassicalRunnersFromComponent = detectRunnerInComponents();

    List<AbstractRunner> listRunners = new ArrayList<>();
    listRunners.addAll(listAbstractConnector);
    listRunners.addAll(listAbstractWorker);
    listRunners.addAll(listClassicalRunnersFromComponent);

    for (AbstractRunner runner : listRunners) {
      List<String> listOfErrors = runner.checkValidDefinition().listOfErrors();
      if (!listOfErrors.isEmpty()) {
        logger.error("RunnerEmbeddedFactory: CAN'T LOAD [" + runner.getType() + (runner.getName() != null ?
            " (" + runner.getName() + ")" :
            "") + "] can't start, errors " + String.join(", ", listOfErrors));
        continue;
      }
      try {
        storageRunner.saveEmbeddedRunner(runner);
      } catch (Exception e) {
        logOperation.log(OperationEntity.Operation.ERROR, "RunnerEmbeddedFactory: CAN'T SAVE [" + runner.getType() + (
            runner.getName() != null ?
                " (" + runner.getName() + ")" :
                "") + "]  error " + e.getMessage());
      }
    }
  }

  public List<RunnerLightDefinition> getAllRunners() {
    return Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream()).map(t -> {
      RunnerLightDefinition light = new RunnerLightDefinition();
      light.name = t.getName();
      light.type = t.getType();
      light.origin = RunnerDefinitionEntity.Origin.EMBEDDED;
      return light;
    }).toList();
  }

  /**
   * Return the runner by its name, if it exists
   *
   * @param name name of the runner
   * @return null if not exist, else the runner
   */
  public AbstractRunner getByName(String name) {
    List<AbstractRunner> listRunners = Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream())
        .filter(t -> t.getName().equals(name))
        .toList();
    return listRunners.isEmpty() ? null : listRunners.get(0);
  }

  /**
   * Return the runner by its name, if it exists
   *
   * @param type type of the runner
   * @return null if not exist, else the runner
   */
  public AbstractRunner getByType(String type) {
    List<AbstractRunner> listRunners = Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream())
        .filter(t -> t.getType().equals(type))
        .toList();
    return listRunners.isEmpty() ? null : listRunners.get(0);
  }

  private List<AbstractRunner> detectRunnerInComponents() {
    logger.info("Start detection runner in all Components");
    long begTime = System.currentTimeMillis();
    List<AbstractRunner> listClassicalRunnersFromComponent = new ArrayList<>();

    Map<String, Object> beansOfType = applicationContext.getBeansOfType(Object.class);


    for (Map.Entry bean : beansOfType.entrySet()) {
      // Theses are already detected before, we skip them
      if (bean.getValue() instanceof AbstractRunner ||
          bean.getValue() instanceof AbstractConnector)
        continue;

      listClassicalRunnersFromComponent.addAll(RunnerFactory.detectRunnersInObject(bean.getValue()));
    }
    long endTime = System.currentTimeMillis();
    logger.info("End detection runner in all Components find {} runners n {} ms ({} objects checked",
        listClassicalRunnersFromComponent.size(),
        endTime-begTime,
        beansOfType.size());
    return listClassicalRunnersFromComponent;
  }

}
