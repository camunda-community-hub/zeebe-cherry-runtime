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
import io.camunda.cherry.exception.OperationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RunnerFactory {

    private static final Logger logger = LoggerFactory.getLogger(RunnerFactory.class.getName());
    private final JarManagementClassLoader jarManagementClassLoader;
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
    private final ApplicationContext context;
    private final ConfigurableApplicationContext parentContext;
    private final Map<ClassLoader, ConfigurableApplicationContext> pluginContexts = new ConcurrentHashMap<>();


    RunnerFactory(JarManagementClassLoader jarManagementClassLoader,
                  StorageRunner storageRunner,
                  RunnerExecutionRepository runnerExecutionRepository,
                  RunnerUploadFactory runnerUploadFactory,
                  LogOperation logOperation,
                  SessionFactory sessionFactory,
                  ApplicationContext context) {
        this.jarManagementClassLoader = jarManagementClassLoader;
        this.storageRunner = storageRunner;
        this.runnerExecutionRepository = runnerExecutionRepository;
        this.logOperation = logOperation;
        this.sessionFactory = sessionFactory;
        this.runnerUploadFactory = runnerUploadFactory;
        this.context = context;
        this.parentContext = (ConfigurableApplicationContext) context;

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

            if (runnerDefinitionEntity.jar == null) {
                logOperation.logError("No Jar file, not an embedded runner for [{}" + runnerDefinitionEntity.name + "]");
                return Collections.emptyList();
            }

            Object objectRunner = jarManagementClassLoader.getInstance(runnerDefinitionEntity.classname, runnerDefinitionEntity.jar.name);

            List<AbstractRunner> listRunners = objectRunner == null ? Collections.emptyList() : jarManagementClassLoader.detectRunnersInObject(objectRunner);
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


    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class PluginBootstrap {
    }
}
