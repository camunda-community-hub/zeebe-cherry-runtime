/* ******************************************************************** */
/*                                                                      */
/*  RunnerAdminOperation                                                 */
/*                                                                      */
/*  All adminstration operation:                                        */
/*   - Delete Jar
/*   - upload Jar
/*   - Download Jar from Repository                                     */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.StorageService;
import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.exception.OperationAlreadyStoppedException;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.runtime.LogOperation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Service
public class RunnerAdminOperation {


    private final StorageService storageService;

    private final JobRunnerFactory jobRunnerFactory;

    private final JarManagementClassLoader jarManagementClassLoader;
    private final LogOperation logOperation;

    public RunnerAdminOperation(StorageService storageService,
                                RunnerDefinitionRepository runnerDefinitionRepository,
                                JobRunnerFactory jobRunnerFactory,
                                JarManagementClassLoader jarManagementClassLoader, LogOperation logOperation) {
        this.storageService = storageService;
        this.jobRunnerFactory = jobRunnerFactory;
        this.jarManagementClassLoader = jarManagementClassLoader;
        this.logOperation = logOperation;
    }

    public boolean deleteJarFile(Long storageEntityId) throws OperationException {

        // search the StorageEntity
        Optional<JarStorageEntity> jarStorageEntity = storageService.findJarStorageById(storageEntityId);
        if (jarStorageEntity.isEmpty())
            throw new OperationException("JAR_NOT_FOUND", "Can't find Jar by [" + storageEntityId + "]");

        // Need that variable for the stream
        // Identify all worker behind the JarEntity
        logOperation.log(OperationEntity.Operation.REMOVEJAR, "Remove Jar [" + jarStorageEntity.get().name + "]");
        List<RunnerDefinitionEntity> listRunnersDefinition = storageService.selectAllRunnerDefinitionByJarNotNull();
        List<RunnerDefinitionEntity> listRunners = listRunnersDefinition.stream() // Stream
                .filter(t -> {
                    return jarStorageEntity.get().id.equals(t.jar.id);
                }).toList();

        // Stop all workers
        String runnerNotStopped = "";
        for (RunnerDefinitionEntity runnerEntity : listRunners) {
            try {
                if (!jobRunnerFactory.stopRunner(runnerEntity.type))
                    runnerNotStopped += runnerEntity.name + ";";
            } catch (OperationAlreadyStoppedException e) {
                // Ok, it's already stopped, proceed
            }
        }

        if (!runnerNotStopped.isEmpty())
            throw new OperationException("CANT_STOP_RUNNER", "Runners[" + runnerNotStopped + "]");

        // remove worker from database
        for (RunnerDefinitionEntity runnerEntity : listRunners) {
            logOperation.log(OperationEntity.Operation.REMOVERUNNER, "Remove Runner (remove Jar) [" + runnerEntity.name + "]");
            storageService.deleteRunnerDefinition(runnerEntity);
        }
        // remove Jar
        storageService.delete(jarStorageEntity.get());

        // remove from ClassLoader
        jarManagementClassLoader.removeJarFile(jarStorageEntity.get().name);
        return true;

    }
}
