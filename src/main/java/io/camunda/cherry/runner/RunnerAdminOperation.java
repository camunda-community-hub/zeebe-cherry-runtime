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

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarStorageEntityRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.exception.OperationAlreadyStoppedException;
import io.camunda.cherry.exception.OperationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Service
public class RunnerAdminOperation {


    private final JarStorageEntityRepository jarStorageEntityRepository;


    private final RunnerDefinitionRepository runnerDefinitionRepository;


    private final JobRunnerFactory jobRunnerFactory;

    private final JarManagementClassLoader jarManagementClassLoader;

    public RunnerAdminOperation(JarStorageEntityRepository jarStorageEntityRepository,
                                RunnerDefinitionRepository runnerDefinitionRepository,
                                JobRunnerFactory jobRunnerFactory,
                                JarManagementClassLoader jarManagementClassLoader) {
        this.jarStorageEntityRepository = jarStorageEntityRepository;
        this.runnerDefinitionRepository = runnerDefinitionRepository;
        this.jobRunnerFactory = jobRunnerFactory;
        this.jarManagementClassLoader = jarManagementClassLoader;
    }

    public boolean deleteJarFile(Long storageEntityId) throws OperationException {

        // search the StorageEntity
        Optional<JarStorageEntity> storageEntity = jarStorageEntityRepository.findById(storageEntityId);
        if (storageEntity.isEmpty())
            throw new OperationException("JAR_NOT_FOUND", "Can't find Jar by [" + storageEntityId + "]");

        // Need that variable for the stream
        // Identify all worker behind the JarEntity
        List<RunnerDefinitionEntity> listRunnersDefinition = runnerDefinitionRepository.selectAllByJarNotNull();
        List<RunnerDefinitionEntity> listRunners = listRunnersDefinition.stream() // Stream
                .filter(t -> {
                    return storageEntity.get().id.equals(t.jar.id);
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
            runnerDefinitionRepository.delete(runnerEntity);
        }
        // remove Jar
        jarStorageEntityRepository.delete(storageEntity.get());

        // remove from ClassLoader
        jarManagementClassLoader.removeJarFile(storageEntity.get().name);
        return true;

    }
}
