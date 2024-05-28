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

  /* A ZeebeConnector can be a simple Connector, with the @JobWorker annotations */ List<AbstractRunner> listClassicalRunnersFromComponent;
  StorageRunner storageRunner;
  LogOperation logOperation;
  @Autowired
  private ApplicationContext applicationContext;

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

    /// run the detection in all components
    listClassicalRunnersFromComponent = detectRunnerInComponents();

    List<AbstractRunner> listRunners = new ArrayList<>();
    listRunners.addAll(listAbstractConnector);
    listRunners.addAll(listAbstractWorker);
    listRunners.addAll(listClassicalRunnersFromComponent);

    for (AbstractRunner runner : listRunners) {
      List<String> listOfErrors = runner.checkValidDefinition().listOfErrors();
      if (!listOfErrors.isEmpty()) {
        logger.error("RunnerEmbeddedFactory: CAN'T LOAD runner name[{}] type[{}]: errors:{}", runner.getName(),
            runner.getType(), String.join(", ", listOfErrors));
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
    return concatAllRunners().map(t -> {
      return new RunnerLightDefinition(t.getName(), t.getType(), t.getClass().getName(),
          RunnerDefinitionEntity.Origin.EMBEDDED);
    }).toList();
  }

  /**
   * Return the runner by its name, if it exists
   *
   * @param name name of the runner
   * @return null if not exist, else the runner
   */
  public AbstractRunner getByName(String name) {
    List<AbstractRunner> listRunners = concatAllRunners().filter(t -> t.getName().equals(name)).toList();
    return listRunners.isEmpty() ? null : listRunners.get(0);
  }

  private Stream<AbstractRunner> concatAllRunners() {
    return Stream.concat(Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream()),
        listClassicalRunnersFromComponent.stream());
  }

  /**
   * Return the runner by its name, if it exists
   *
   * @param type type of the runner
   * @return null if not exist, else the runner
   */
  public AbstractRunner getByType(String type) {
    List<AbstractRunner> listRunners = concatAllRunners().filter(t -> t.getType().equals(type)).toList();
    return listRunners.isEmpty() ? null : listRunners.get(0);
  }

  /**
   * DetectRunnerInComponents
   * Check all Bean and Components and search for Workers
   *
   * @return list of detected runners
   */
  private List<AbstractRunner> detectRunnerInComponents() {
    logger.info("Start detection runners in all Components");
    long begTime = System.currentTimeMillis();
    List<AbstractRunner> listDetectedRunners = new ArrayList<>();

    Map<String, Object> beansOfType = applicationContext.getBeansOfType(Object.class);

    for (Map.Entry bean : beansOfType.entrySet()) {
      // Theses are already detected before, we skip them
      if (bean.getValue() instanceof AbstractRunner || bean.getValue() instanceof AbstractConnector)
        continue;

      listDetectedRunners.addAll(RunnerFactory.detectRunnersInObject(bean.getValue()));
    }
    long endTime = System.currentTimeMillis();
    logger.info("End detection runner in all Components find {} runners in {} ms ({} objects checked",
        listDetectedRunners.size(), endTime - begTime, beansOfType.size());
    for (AbstractRunner runner : listDetectedRunners) {
      logger.info("  Runner detected name[{}] type[{}]", runner.getName(), runner.getType());
    }
    return listDetectedRunners;
  }

}
