/* ******************************************************************** */
/*                                                                      */
/*  Cherry application                                                  */
/*                                                                      */
/*  RunnerInternalDetection. detect all internal runner, and update     */
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class RunnerEmbeddedFactory {

  Logger logger = LoggerFactory.getLogger(RunnerEmbeddedFactory.class.getName());

  List<AbstractConnector> listAbstractConnector;

  List<AbstractWorker> listAbstractWorker;

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
    List<AbstractRunner> listRunners = Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream())
        .toList();
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
}
