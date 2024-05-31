/* ******************************************************************** */
/*                                                                      */
/*  RunnerFactory                                                       */
/*                                                                      */
/*  manipulate all runners.                                             */
/* main API for RunnerEmbedded, RunnerUpload to manipulate different    */
/* kind of runners, and interface to RunnerStorage                      */
/*                                                                      */
/* The RunnerFactory does not manage execution, just definition and     */
/* storage. See JobRunnerFactory                                        */
/*                                                                      */
/* This is the main entrance for all external access.                   */
/*                                                                      */
/* Note: workers are created in the JobRunnerFactory. This class manage */
/* the runner definition, not the execution                             */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RunnerFactory {

  private static final Logger logger = LoggerFactory.getLogger(RunnerFactory.class.getName());
  private final RunnerEmbeddedFactory runnerEmbeddedFactory;
  private final RunnerUploadFactory runnerUploadFactory;
  private final RunnerClassLoaderFactory runnerClassLoaderFactory;
  private final StorageRunner storageRunner;
  private final RunnerExecutionRepository runnerExecutionRepository;
  private final LogOperation logOperation;
  private final SessionFactory sessionFactory;

  /**
   * There is only one object per runner, so it's possible to cache them
   */
  private Map<String,Object> runnerCache = new HashMap<>();

  RunnerFactory(RunnerEmbeddedFactory runnerEmbeddedFactory,
                RunnerUploadFactory runnerUploadFactory,
                RunnerClassLoaderFactory runnerClassLoaderFactory,
                StorageRunner storageRunner,
                RunnerExecutionRepository runnerExecutionRepository,
                LogOperation logOperation,
                SessionFactory sessionFactory) {
    this.runnerEmbeddedFactory = runnerEmbeddedFactory;
    this.runnerUploadFactory = runnerUploadFactory;
    this.runnerClassLoaderFactory = runnerClassLoaderFactory;
    this.storageRunner = storageRunner;
    this.runnerExecutionRepository = runnerExecutionRepository;
    this.logOperation = logOperation;
    this.sessionFactory = sessionFactory;
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
      logger.info(
          "Candidate Runner is AbstractRunner [{}] CherryConnector[{}] type [{}] inputSize [{}] outputSize [{}]",
          candidateRunner.getClass().getName(),
          (candidateRunner instanceof SdkRunnerCherryConnector ? "Cherry" : "Classic"),
          ((AbstractRunner) candidateRunner).getType(), ((AbstractRunner) candidateRunner).getListOutput().size(),
          ((AbstractRunner) candidateRunner).getListOutput().size());
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

      // temp for debug
      AbstractRunner last = listDetectedRunners.get(listDetectedRunners.size() - 1);
      logger.info("Detect Runner in Object [{}] class [{}] [{}] type [{}] inputSize [{}] outputSize [{}]",
          candidateRunner.getClass().getName(), (last instanceof SdkRunnerCherryConnector ? "Cherry" : "Classic"),
          last.getName(), last.getType(), last.getListInput().size(), last.getListOutput().size());

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


  /* ******************************************************************** */
  /*                                                                      */
  /*  Operations                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Initialise step
   * 1: detect/load all runners in the storage:
   * - from embedbed (thanks to runnerEmbeddedFactory),
   * - UploadPath (runnerUploadFactory)
   * 2; copy all Jar from the storage to the Classloaderpath (runnerUploadFactory)
   * <p>
   * The class does not start any runners
   */
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
    logger.info("----- RunnerFactory.3 Load JavaClassLoaderPath from storage");
    List<String> lisJars = runnerUploadFactory.loadClassLoaderJarsFromStorage(true);
    logInfo = lisJars.stream().collect(Collectors.joining(","));
    logger.info("Load JarUploadPath [{}]", logInfo);

  }

  /**
   * Save a new Jar file. A Jar file contains multiple runners. This method does not stop/restart runners.
   * The method save in the storage and in the classloader path. The load in the JavaClassLoader is not under
   * the responsability of the method, just to place the jar in the storage and the classloader.
   *
   * @param file multipart file
   * @return list of runners detected in the jar file
   */
  public List<RunnerLightDefinition> saveFromMultiPartFile(MultipartFile file, String jarFileName) {

    List<RunnerLightDefinition> listRunnersDetected = new ArrayList<>();
    JarStorageEntity jarStorageEntity = storageRunner.getJarStorageByName(jarFileName);

    // save the file on a temporary disk
    OutputStream outputStream = null;
    Path jarTemp = null;
    try {
      jarTemp = Files.createTempFile(jarFileName, ".jar");
      // Open an OutputStream to the temporary file
      outputStream = new FileOutputStream(jarTemp.toFile());
      // Transfer data from InputStream to OutputStream
      byte[] buffer = new byte[1024 * 100]; // 100Ko
      int bytesRead;
      int count = 0;
      InputStream inputStream = file.getInputStream();
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        count += bytesRead;
        outputStream.write(buffer, 0, bytesRead);
      }
      outputStream.flush();
      outputStream.close();
      outputStream = null;

      listRunnersDetected = runnerUploadFactory.saveJarFileToStorage(jarTemp.toFile(), jarFileName, true);

      logOperation.log(OperationEntity.Operation.LOADJAR, "UploadJar[" + file.getName() + "]");

    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR, "Can't load JAR [" + jarFileName + "] : " + e.getMessage());
    } finally {
      if (outputStream != null)
        try {
          outputStream.close();
        } catch (Exception e) {
          // do nothing
        }
    }
    return listRunnersDetected;
  }

  /**
   * Copy a Jar File from Storage to the ClassLoader path
   *
   * @param jarFileName
   */
  public boolean jarFileToClassLoader(String jarFileName) {
    return runnerUploadFactory.jarFileStorageToClassLoader(jarFileName);
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

        storageRunner.removeRunner(entityToRemove);
        txn.commit();
      } catch (Exception e) {
        logOperation.logError("Can't delete [" + entityToRemove.type + "]", e);
      }
    }

  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  getter/setter                                                       */
  /*                                                                      */
  /* ******************************************************************** */

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
      listRunners.addAll(getRunnersFromEntity(runnerDefinitionEntity));
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
   * Get the runner by its entity. Assuming the Jar is already loaded on the ClassLoader path, and it is loaded
   * in the Java Macbine during the operation
   *
   * @param runnerDefinitionEntity runnerEntity
   * @return the runner
   */
  private List<AbstractRunner> getRunnersFromEntity(RunnerDefinitionEntity runnerDefinitionEntity) {
    ClassLoader loader;
    try {
      // if this class is embedded?
      AbstractRunner embeddedRunner = runnerEmbeddedFactory.getByType(runnerDefinitionEntity.type);
      if (embeddedRunner != null) {
        return List.of(embeddedRunner);
      }

      if (runnerDefinitionEntity.jar == null) {
        logOperation.logError("No Jar file, not an embedded runner for [" + runnerDefinitionEntity.name + "]");
        return Collections.emptyList();
      }
      Class clazz = runnerClassLoaderFactory.loadClassInJavaMachine(runnerDefinitionEntity.jar.name,
          runnerDefinitionEntity.classname);

      Object objectRunner = getRunnerObjectFromClass(clazz);

      List<AbstractRunner> listRunners = detectRunnersInObject(objectRunner);
      if (!listRunners.isEmpty())
        return listRunners;

      /* we must have a runner detected in a entity */
      logger.error("No method to get a runner from [{}]", runnerDefinitionEntity.name);
      logOperation.logError("Class [" + runnerDefinitionEntity.classname + "] in jar[" + runnerDefinitionEntity.jar.name
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

  public boolean deleteJarFile(Long jarEntity) throws OperationException {
    return true;
  }

  @Autowired
  private ApplicationContext context;

  private Object getRunnerObjectFromClass(Class clazz)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    // There is two uses case:
    // 1. the object is complex, and need injection. Then, it may be a @Bean

    // 2. the class is very straightforward, and then we just need to create a new instance
    try {
      // First, ask Spring to load the class.
      GenericWebApplicationContext genericContext = (GenericWebApplicationContext) context;
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
      genericContext.registerBeanDefinition(clazz.getSimpleName(), builder.getBeanDefinition());

      Object beanObject = context.getBean(clazz);
      logOperation.log(OperationEntity.Operation.STARTRUNNER, "Runner is a bean [" + clazz.getName() + "]");
      return beanObject;
    } catch (Exception e) {
      // Don't need to log, this is not a bean
      logger.info("Error "+e);
    }

    return clazz.getDeclaredConstructor().newInstance();

  }

}
