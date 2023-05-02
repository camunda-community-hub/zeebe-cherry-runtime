/* ******************************************************************** */
/*                                                                      */
/*  Cherry application                                                  */
/*                                                                      */
/*  RunnerInternalDetection. detect all internal runner, and update     */
/*  the internal database with all detection                            */
/* ******************************************************************** */

package io.camunda.cherry.runner;

import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class RunnerEmbeddedFactory {

  Logger logger = LoggerFactory.getLogger(RunnerEmbeddedFactory.class.getName());

  @Autowired
  List<AbstractConnector> listAbstractConnector;

  @Autowired
  List<AbstractWorker> listAbstractWorker;

  @Autowired
  StorageRunner storageRunner;

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
        logger.error("RunnerEmbeddedFactory: CAN'T SAVE [" + runner.getType() + (runner.getName() != null ?
            " (" + runner.getName() + ")" :
            "") + "]  error " + e.getMessage());
        continue;
      }
    }
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
}
