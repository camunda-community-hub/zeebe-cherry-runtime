/* ******************************************************************** */
/*                                                                      */
/*  StorageRunner                                                       */
/*                                                                      */
/* This class manage the Storage Service                              */
/* A connector can be provided from the upload Path, or uploader, or    */
/* come from the marker place. It will be saved in the Storage.         */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarDefinitionRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.definition.AbstractRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
/**
 */ public class StorageRunner {

  Logger logger = LoggerFactory.getLogger(StorageRunner.class.getName());

  @Autowired
  RunnerDefinitionRepository runnerDefinitionRepository;

  @Autowired
  JarDefinitionRepository jarDefinitionRepository;




  /* ******************************************************************** */
  /*                                                                      */
  /*  jarDefinition function                                              */
  /*                                                                      */
  /*  Manipulate any external JAR file                                    */
  /* ******************************************************************** */

  /**
   * Save the connector to the storage
   * The JAR file may contain multiple runner.
   *
   * @param jarFile jarfile to save
   * @throws IOException error during saving
   */
  public JarStorageEntity saveJarRunner(File jarFile) throws IOException {
    String connectorName = jarFile.getName();
    JarStorageEntity jarStorageEntity = jarDefinitionRepository.findByName(connectorName);
    if (jarStorageEntity != null)
      return jarStorageEntity;

    jarStorageEntity = new JarStorageEntity();

    jarStorageEntity.name = connectorName;
    jarStorageEntity.jarfile = new byte[(int) jarFile.length()];
    try (FileInputStream fis = new FileInputStream(jarFile)) {
      fis.read(jarStorageEntity.jarfile);
    }
    // Save it
    jarDefinitionRepository.save(jarStorageEntity);
    return jarStorageEntity;
  }

  /**
   * Update the JarStorage
   *
   * @param jarStorageEntity entity to update
   */
  public void updateJarStorage(JarStorageEntity jarStorageEntity) {
    jarDefinitionRepository.save(jarStorageEntity);
  }

  /**
   * get the JarStorageEntity
   *
   * @param jarFileName jar file
   * @return JarStorageEntity, null if not exist
   */
  public JarStorageEntity getJarStorageByName(String jarFileName) {
    return jarDefinitionRepository.findByName(jarFileName);
  }

  /**
   * Return all JAR available
   *
   * @return
   */
  public List<JarStorageEntity> getAll() {
    return jarDefinitionRepository.getAll();
  }

  /**
   * check if the JarStorageEntity exist
   *
   * @param jarFileName jar file
   * @return true if exist
   */
  public boolean existJarDefinition(String jarFileName) {
    return jarDefinitionRepository.findByName(jarFileName) != null;
  }

  public RunnerDefinitionEntity saveUploadRunner(String name, String type, Class clazz, JarStorageEntity jarDefinition)
      throws IOException {
    RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByName(name);
    if (runnerDefinition != null)
      return runnerDefinition;

    runnerDefinition = new RunnerDefinitionEntity();

    runnerDefinition.name = name;
    runnerDefinition.classname = clazz.getCanonicalName();
    runnerDefinition.type = type;
    runnerDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;

    runnerDefinition.jar = jarDefinition;
    // start it by default
    runnerDefinition.activeRunner = true;
    return runnerDefinitionRepository.save(runnerDefinition);

  }

  public RunnerDefinitionEntity saveUploadRunner(AbstractRunner runner, JarStorageEntity jarDefinition)
      throws IOException {
    RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByName(runner.getName());
    if (runnerDefinition != null)
      return runnerDefinition;

    runnerDefinition = new RunnerDefinitionEntity();

    runnerDefinition.name = runner.getName();
    runnerDefinition.classname = runner.getClass().getCanonicalName();
    runnerDefinition.jar = jarDefinition;
    runnerDefinition.type = runner.getType();
    runnerDefinition.collectionName = runner.getCollectionName();
    runnerDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;

    // start it by default
    runnerDefinition.activeRunner = true;
    return runnerDefinitionRepository.save(runnerDefinition);

  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Embedded Runners                                                    */
  /*                                                                      */
  /*  Manipulate any embedded runner, when the Cherry Library is used     */
  /* ******************************************************************** */

  /**
   * Save a Internal connector: this connector is detected by Spring, because it is a component
   *
   * @param runner runner to save
   * @throws IOException in case of error during the operation
   */
  public RunnerDefinitionEntity saveEmbeddedRunner(AbstractRunner runner) throws IOException {
    RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByName(runner.getName());
    if (runnerDefinition != null)
      return runnerDefinition;

    runnerDefinition = new RunnerDefinitionEntity();

    runnerDefinition.name = runner.getName();
    runnerDefinition.classname = runner.getClass().getCanonicalName();
    runnerDefinition.type = runner.getType();
    runnerDefinition.collectionName = runner.getCollectionName();
    runnerDefinition.origin = RunnerDefinitionEntity.Origin.EMBEDED;

    // start it by default
    runnerDefinition.activeRunner = true;
    return runnerDefinitionRepository.save(runnerDefinition);
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Access runner, whatever the origin                                  */
  /*                                                                      */
  /* ******************************************************************** */

  public List<RunnerDefinitionEntity> getRunners(Filter filter) {

    List<RunnerDefinitionEntity> listRunnerEntity = runnerDefinitionRepository.findAll();
    return listRunnerEntity.stream() // stream to apply each filter
        .filter(t -> {
          if (filter.activeOnly == null) {
            return true;
          } else {
            return t.activeRunner == filter.activeOnly;
          }
        }).filter(t -> {
          if (filter.filterName == null)
            return true;
          return t.name.equals(filter.filterName);
        }).filter(t -> {
          if (filter.filterType == null)
            return true;
          return t.type.equals(filter.filterType);
        }).toList();
  }

  public boolean existRunner(String runnerName) {
    return runnerDefinitionRepository.selectByName(runnerName) != null;
  }

  public static class Filter {
    /**
     * Null: all runners, else True or False
     */
    Boolean activeOnly;
    String filterName;
    String filterType;

    public Filter isActive(boolean activeOnly) {
      this.activeOnly = activeOnly;
      return this;
    }

    public Filter name(String name) {
      this.filterName = name;
      return this;
    }

    public Filter type(String type) {
      this.filterType = type;
      return this;
    }
  }
}
