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
import io.camunda.cherry.db.repository.JarDefinitionRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.exception.OperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Service
public class RunnerAdminOperation {

  @Autowired
  JarDefinitionRepository jarDefinitionRepository;

  @Autowired
  RunnerDefinitionRepository runnerDefinitionRepository;

  @Autowired
  JobRunnerFactory jobRunnerFactory;

  public boolean deleteJarFile(Long storageEntityId) throws OperationException {

    // search the StorageEntity
    JarStorageEntity storageEntity = jarDefinitionRepository.getReferenceById(storageEntityId);

    // Identify all worker behind the JarEntity
    List<RunnerDefinitionEntity> listRunnersDefinition = runnerDefinitionRepository.selectAllByJarNotNull();
    List<RunnerDefinitionEntity> listRunners = listRunnersDefinition.stream() // Stream
        .filter(t -> {
          return t.jar.id.equals(storageEntity.id);
        }).collect(Collectors.toList());

    // Stop all workers
    String runnerNotStopped = "";
    for (RunnerDefinitionEntity runnerEntity : listRunners) {
      if (!jobRunnerFactory.stopRunner(runnerEntity.type))
        runnerNotStopped += runnerEntity.name + ";";
    }

    if (!runnerNotStopped.isEmpty())
      throw new OperationException("CANT_STOP_RUNNER", "Runners[" + runnerNotStopped + "]");

    // remove worker from database
    for (RunnerDefinitionEntity runnerEntity : listRunners) {
      runnerDefinitionRepository.delete(runnerEntity);
    }
    // remove Jar
    jarDefinitionRepository.delete(storageEntity);

    return true;

  }
}
