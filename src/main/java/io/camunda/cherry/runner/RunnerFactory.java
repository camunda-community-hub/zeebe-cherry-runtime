/* ******************************************************************** */
/*                                                                      */
/*  RunnerFactory                                                 */
/*                                                                      */
/*  manipulate all runners.                                             */
/* main API for RunnerEmbedded, RunnerUpload to manipulate different    */
/* kind of runner, and interface to RunnerStorage                       */
/*                                                                      */
/* This is the main entrance for all external access                    */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.SdkRunnerConnector;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Service
public class RunnerFactory {

  @Autowired
  RunnerEmbeddedFactory runnerEmbeddedFactory;
  @Autowired
  RunnerUploadFactory runnerUploadFactory;
  @Autowired
  StorageRunner storageRunner;

  @Autowired
  LogOperation logOperation;

  Logger logger = LoggerFactory.getLogger(RunnerFactory.class.getName());

  public void init() {
    logger.info("----- RunnerFactory.1 Load all embedded runner");

    runnerEmbeddedFactory.registerInternalRunner();

    // second, check all library connector
    logger.info("----- RunnerFactory.2 Load all upload JAR");
    runnerUploadFactory.loadStorageFromUploadPath();

    // Upload the ClassLoaderPath, and load the class
    logger.info("----- RunnerFactory.3 Load JavaMachine from storage");
    runnerUploadFactory.loadJavaFromStorage();

  }

  /**
   * Get All runners
   *
   * @param filter specify the type of runners
   * @return list of runner
   */
  public List<AbstractRunner> getAllRunners(StorageRunner.Filter filter) {
    List<AbstractRunner> listRunners = new ArrayList<>();

    List<RunnerDefinitionEntity> listDefinitionRunners = storageRunner.getRunners(filter);

    for (RunnerDefinitionEntity runnerDefinitionEntity : listDefinitionRunners) {
      AbstractRunner runner = getRunnerFromEntity(runnerDefinitionEntity);
      if (runner != null)
        listRunners.add(runner);
    }
    return listRunners;
  }

  /**
   * Return the list store in the entity. This part contains different information, like the origin of the runner (store? Embedded?)
   * @param filter to select part of the runner
   * @return the list of entity
   */
  public List<RunnerDefinitionEntity> getAllRunnersEntity(StorageRunner.Filter filter) {
    return storageRunner.getRunners(filter);

  }

  /**
   * Get the runner by it's entity
   *
   * @param runnerDefinitionEntity runnerEntity
   * @return the runner
   */
  private AbstractRunner getRunnerFromEntity(RunnerDefinitionEntity runnerDefinitionEntity) {
    try {
      // if this class is embedded?
      AbstractRunner embeddedRunner = runnerEmbeddedFactory.getByName(runnerDefinitionEntity.name);
      if (embeddedRunner != null) {
        return embeddedRunner;
      }

      if (runnerDefinitionEntity.jar != null) {
        String jarFileName =
            runnerUploadFactory.getClassLoaderPath() + File.separator + runnerDefinitionEntity.jar.name;

        ClassLoader loader = new URLClassLoader(new URL[] { new File(jarFileName).toURI().toURL() });

        Class clazz = loader.loadClass(runnerDefinitionEntity.classname);
        Object objectRunner = clazz.getDeclaredConstructor().newInstance();

        if (objectRunner instanceof AbstractRunner runner) {
          return runner;
        } else if (objectRunner instanceof OutboundConnectorFunction outboundConnector) {
          SdkRunnerConnector runner = new SdkRunnerConnector(outboundConnector);
          OutboundConnector connectorAnnotation = (OutboundConnector) clazz.getAnnotation(OutboundConnector.class);
          runner.setType(connectorAnnotation.type());
          runner.setName(connectorAnnotation.name());
          return runner;
        }
      }
      logger.error("No method to get a runner from [" + runnerDefinitionEntity.name + "]");
      return null;
    } catch (Error er) {
      // ControllerPage getting the information
      logOperation.logError(runnerDefinitionEntity.name, "Instantiate the runner ", er);
      return null;
    } catch (Exception e) {
      // ControllerPage getting the informations
      logOperation.logException(runnerDefinitionEntity.name, "Instantiate the runner ", e);
      return null;
    }
  }

}
