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

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.connector.SdkRunnerCherryConnector;
import io.camunda.cherry.definition.connector.SdkRunnerConnector;
import io.camunda.cherry.definition.connector.SdkRunnerWorker;
import io.camunda.cherry.exception.OperationException;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RunnerFactory {

  private final RunnerEmbeddedFactory runnerEmbeddedFactory;
  private final RunnerUploadFactory runnerUploadFactory;
  private final StorageRunner storageRunner;
  private final RunnerExecutionRepository runnerExecutionRepository;
  private final LogOperation logOperation;
  private final SessionFactory sessionFactory;

  Logger logger = LoggerFactory.getLogger(RunnerFactory.class.getName());

  RunnerFactory(RunnerEmbeddedFactory runnerEmbeddedFactory,
                RunnerUploadFactory runnerUploadFactory,
                StorageRunner storageRunner,
                RunnerExecutionRepository runnerExecutionRepository,
                LogOperation logOperation,
                SessionFactory sessionFactory) {
    this.runnerEmbeddedFactory = runnerEmbeddedFactory;
    this.runnerUploadFactory = runnerUploadFactory;
    this.storageRunner = storageRunner;
    this.runnerExecutionRepository = runnerExecutionRepository;
    this.logOperation = logOperation;
    this.sessionFactory = sessionFactory;
  }

  public void init() {
    logger.info("----- RunnerFactory.1 Load all embedded runner");

    runnerEmbeddedFactory.registerInternalRunner();

    // second, check all library connector
    logger.info("----- RunnerFactory.2 Load to storage all upload JAR");
    List<RunnerLightDefinition> runnerLightDefinitions = runnerUploadFactory.loadStorageFromUploadPath();
    String logInfo = runnerLightDefinitions.stream()
        .map(RunnerLightDefinition::getName)
        .collect(Collectors.joining(","));
    logger.info("Load StorageFromUploadPath [{}]", logInfo);

    // Upload the ClassLoaderPath, and load the class
    logger.info("----- RunnerFactory.3 Load JavaMachine from storage");
    List<String> lisJars = runnerUploadFactory.loadJarFromStorage(true);
    logInfo = lisJars.stream().collect(Collectors.joining(","));
    logger.info("Load JarUploadPath [{}]", logInfo);

  }

  /**
   * Must be call after the initialisation
   * all runners are loaded amd identified. The storageRunner are checked, and all runner in the database
   * which are not loaded are purged.
   */
  public void synchronize() {
    Map<String, RunnerLightDefinition> mapExistingRunners = Stream.concat(
            runnerEmbeddedFactory.getAllRunners().stream(), runnerUploadFactory.getAllRunners().stream())
        .collect(Collectors.toMap(RunnerLightDefinition::getType, Function.identity()));

    // get the list of entities
    List<RunnerDefinitionEntity> listRunnersEntity = storageRunner.getRunners(new StorageRunner.Filter());
    // identify entity which does not exist
    List<RunnerDefinitionEntity> listEntityToRemove = listRunnersEntity.stream()
        .filter(t -> !mapExistingRunners.containsKey(t.type))
        .toList();

    for (RunnerDefinitionEntity entityToRemove : listEntityToRemove) {
      logOperation.log(OperationEntity.Operation.REMOVE,
          "Entity type[" + entityToRemove.type + "] name[" + entityToRemove.name + "]");

      try (Session session = sessionFactory.openSession()) {
        Transaction txn = session.beginTransaction();
        runnerExecutionRepository.deleteFromEntityType(entityToRemove.type);

        storageRunner.removeEntity(entityToRemove);
        txn.commit();
      } catch (Exception e) {
        logOperation.logError("Can't delete [" + entityToRemove.type + "]", e);
      }
    }

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
      listRunners.addAll(getRunnerFromEntity(runnerDefinitionEntity));
    }
    return listRunners;
  }

  /**
   * Return the list store in the entity. This part contains different information, like the origin
   * of the runner (store? Embedded?)
   *
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
  private List<AbstractRunner> getRunnerFromEntity(RunnerDefinitionEntity runnerDefinitionEntity) {
    ClassLoader loader;
    try {
      // if this class is embedded?
      AbstractRunner embeddedRunner = runnerEmbeddedFactory.getByType(runnerDefinitionEntity.type);
      if (embeddedRunner != null) {
        return List.of(embeddedRunner);
      }

      if (runnerDefinitionEntity.jar == null) {
        logOperation.logError("No Jar file, not an embedded runner for [" + runnerDefinitionEntity.name + "]");
        return null;
      }
      String jarFileName = runnerUploadFactory.getClassLoaderPath() + File.separator + runnerDefinitionEntity.jar.name;

      loader = new URLClassLoader(new URL[] { new File(jarFileName).toURI().toURL() });

      Class clazz = loader.loadClass(runnerDefinitionEntity.classname);
      Object objectRunner = clazz.getDeclaredConstructor().newInstance();

      List<AbstractRunner> listRunners = detectRunnersInObject(objectRunner);
      if (!listRunners.isEmpty())
        return listRunners;

      /* we must have a runner detected in a entity */
      logger.error("No method to get a runner from [" + runnerDefinitionEntity.name + "]");
      logOperation.logError("Class [" + runnerDefinitionEntity.classname + "] in jar[" + jarFileName
          + "] not a Runner or OutboundConnectorFunction");
      return listRunners;
    } catch (Error er) {
      // ControllerPage getting the information
      logOperation.logError(runnerDefinitionEntity.name, "Instantiate the runner ", er);
      return Collections.emptyList();
    } catch (Exception e) {
      // ControllerPage getting the informations
      logOperation.logException(runnerDefinitionEntity.name, "Instantiate the runner ", e);
      return Collections.emptyList();
    }
  }

  /**
   * Detect classical runner in an object
   *
   * @param candidateRunner object to search inside
   * @return list of runners detected
   */
  public static List<AbstractRunner> detectRunnersInObject(Object candidateRunner) {

    List<AbstractRunner> listDetectedRunners = new ArrayList<>();

    if (AbstractRunner.class.isAssignableFrom(candidateRunner.getClass())) {
      // if (objectRunner instanceof AbstractRunner runner) {
      listDetectedRunners.add((AbstractRunner) candidateRunner);
      return listDetectedRunners;
    }
    if (candidateRunner instanceof OutboundConnectorFunction outboundConnector) {

      // we have two kind of SDK runner :
      // the classical connector
      // the Cherry Enrichment Connector
      if (SdkRunnerCherryConnector.isRunnerCherryConnector(candidateRunner.getClass())) {
        listDetectedRunners.add(new SdkRunnerCherryConnector(outboundConnector));
      } else {
        listDetectedRunners.add(new SdkRunnerConnector(outboundConnector));
      }
      return listDetectedRunners;
    }

    for (Method method : candidateRunner.getClass().getMethods()) {
      io.camunda.zeebe.spring.client.annotation.JobWorker annotation = method.getAnnotation(
          io.camunda.zeebe.spring.client.annotation.JobWorker.class);
      if (annotation != null)
        listDetectedRunners.add(new SdkRunnerWorker(candidateRunner, annotation, method));
    }

    return listDetectedRunners;
  }



  public boolean deleteJarFile(Long jarEntity) throws OperationException {

    return true;

  }
}
