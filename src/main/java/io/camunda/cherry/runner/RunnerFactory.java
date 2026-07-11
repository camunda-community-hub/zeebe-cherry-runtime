/* ******************************************************************** */
/*                                                                      */
/*  RunnerFactory                                                       */
/*                                                                      */
/* Manipulate all runners, and portal class for all access to runner.   */
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
/*                                                                      */
/*                                                                      */
/*                                                                      */
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Service
public class RunnerFactory {

    private static final Logger logger = LoggerFactory.getLogger(RunnerFactory.class.getName());
    private final RunnerEmbeddedFactory runnerEmbeddedFactory;
    private final RunnerClassLoaderFactory runnerClassLoaderFactory;
    private final StorageRunner storageRunner;
    private final RunnerExecutionRepository runnerExecutionRepository;
    private final LogOperation logOperation;
    private final SessionFactory sessionFactory;
    private final RunnerUploadFactory runnerUploadFactory;

    /**
     * A runner (worker, connector) is instantiate only one time. it maybe a object to create, or a component.
     * When it's create/find, keep it in the cache.
     */
    private final Map<String, AbstractRunner> cacheRunner = new HashMap<>();
    /**
     * There is only one object per runner, so it's possible to cache them
     */
    private final Map<String, Object> runnerCache = new HashMap<>();
    @Autowired
    private ApplicationContext context;

    RunnerFactory(RunnerEmbeddedFactory runnerEmbeddedFactory,
                  RunnerClassLoaderFactory runnerClassLoaderFactory,
                  StorageRunner storageRunner,
                  RunnerExecutionRepository runnerExecutionRepository,
                  RunnerUploadFactory runnerUploadFactory,
                  LogOperation logOperation,
                  SessionFactory sessionFactory) {
        this.runnerEmbeddedFactory = runnerEmbeddedFactory;
        this.runnerClassLoaderFactory = runnerClassLoaderFactory;
        this.storageRunner = storageRunner;
        this.runnerExecutionRepository = runnerExecutionRepository;
        this.logOperation = logOperation;
        this.sessionFactory = sessionFactory;
        this.runnerUploadFactory = runnerUploadFactory;
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
            AbstractRunner last = listDetectedRunners.getLast();
            logger.info("Detect Runner in Object [{}] class [{}] [{}] type [{}] ", candidateRunner.getClass().getName(),
                    (last instanceof SdkRunnerCherryConnector ? "Cherry" : "Classic"), last.getName(), last.getType());

            return listDetectedRunners;
        }

        for (Method method : candidateRunner.getClass().getMethods()) {
            io.camunda.client.annotation.JobWorker annotation = method.getAnnotation(io.camunda.client.annotation.JobWorker.class);
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
    public void init() {
        runnerUploadFactory.init();
    }




    /* ******************************************************************** */
    /*                                                                      */
    /*  Operations                                                       */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * Must be call after the initialisation
     * all runners are loaded amd identified. The storageRunner are checked, and all runner in the database
     * which are not loaded are purged.
     */
    public void synchronize() {
        // not possible to use a Stream: external worker may upgrade embedded worker
        Map<String, RunnerLightDefinition> mapExistingRunners = new HashMap<>();
        for (RunnerLightDefinition runner : runnerEmbeddedFactory.getAllRunners()) {
            if (mapExistingRunners.containsKey(runner.getType()))
                logger.warn("RunnerEmbedded[{}] Already loaded", runner.getType());
            // last one is the winner
            mapExistingRunners.put(runner.getType(), runner);
        }

        for (RunnerLightDefinition runner : runnerUploadFactory.getAllRunners()) {
            if (mapExistingRunners.containsKey(runner.getType()))
                logger.warn("RunnerUpload[{}] Already loaded", runner.getType());
            // last one is the winner
            mapExistingRunners.put(runner.getType(), runner);
        }

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

    /**
     * Install the jar, and return the list of runner detected in the jar.
     * Attention: runners are not stopped/restarted. The runnerFactory can't access the running runner (managed by jobRunnerFactory)
     * @param jarFileName jar file name
     * @param jarFileInputStream InputStream
     * @return list of runners detected in the JAR
     */
    public List<RunnerLightDefinition> installJar(String jarFileName, ByteArrayInputStream jarFileInputStream) {
        List<RunnerLightDefinition> runners = runnerUploadFactory.installJar(jarFileName, jarFileInputStream);
        logOperation.log(OperationEntity.Operation.LOADJAR, "UploadJar[" + jarFileName + "]");
        synchronize();


        return runners;
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
            AbstractRunner runner = cacheRunner.get(runnerDefinitionEntity.type);
            if (runner != null) {
                logger.debug("Return runner {} from cache", runnerDefinitionEntity.type);
                return List.of(runner);
            }

            // if this class is embedded?
            AbstractRunner embeddedRunner = runnerEmbeddedFactory.getByType(runnerDefinitionEntity.type);
            if (embeddedRunner != null) {
                cacheRunner.put(embeddedRunner.getType(), embeddedRunner);
                return List.of(embeddedRunner);
            }

            if (runnerDefinitionEntity.jar == null) {
                logOperation.logError("No Jar file, not an embedded runner for [{}" + runnerDefinitionEntity.name + "]");
                return Collections.emptyList();
            }
            Class clazz = runnerClassLoaderFactory.loadClassInJavaMachine(runnerDefinitionEntity.jar.name,
                    runnerDefinitionEntity.classname);

            Object objectRunner = getRunnerObjectFromClass(clazz);

            List<AbstractRunner> listRunners = detectRunnersInObject(objectRunner);
            if (listRunners.isEmpty()) {
                /* we must have a runner detected in an entity */
                logger.error("No method to get a runner from [{}]", runnerDefinitionEntity.name);
                logOperation.logError(
                        "Class [" + runnerDefinitionEntity.classname + "] in jar[" + runnerDefinitionEntity.jar.name
                                + "] not a Runner or OutboundConnectorFunction");
                return Collections.emptyList();
            }

            for (AbstractRunner runnerIterator : listRunners) {
                cacheRunner.put(runnerIterator.getType(), runnerIterator);
            }
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
            logger.info("Error " + e);
        }

        return clazz.getDeclaredConstructor().newInstance();

    }

}
