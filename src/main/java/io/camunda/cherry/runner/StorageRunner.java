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
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarDefinitionRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.exception.TechnicalException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class StorageRunner {

  Logger logger = LoggerFactory.getLogger(StorageRunner.class.getName());

  @Autowired
  RunnerDefinitionRepository runnerDefinitionRepository;

  @Autowired
  JarDefinitionRepository jarDefinitionRepository;

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  DataSource dataSource;

  @Autowired
  LogOperation logOperation;

  /* ******************************************************************** */
  /*                                                                      */
  /*  jarDefinition function                                              */
  /*                                                                      */
  /*  Manipulate any external JAR file                                    */
  /* ******************************************************************** */

  /**
   * Update (or save) a JarRunner
   *
   * @param jarStorageEntity existing jarRunner or null for a creation
   * @param jarFile          file to save
   * @return the jarStorageEntity
   * @throws TechnicalException for any error
   */
  public JarStorageEntity updateJarRunner(JarStorageEntity jarStorageEntity, File jarFile) throws TechnicalException {
    try (FileInputStream fis = new FileInputStream(jarFile);
        Session session = sessionFactory.openSession();
        Connection con = dataSource.getConnection()) {

      if (jarStorageEntity == null)
        jarStorageEntity = new JarStorageEntity();
      jarStorageEntity.name = jarFile.getName();
      jarStorageEntity.loadedTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
      jarStorageEntity.loadLog = "Loaded correctly from file [" + jarFile.getPath() + "]";

      if (con.getMetaData().getDatabaseProductName().equals("H2")) {
        writeJarBlob(session, jarStorageEntity, fis, jarFile.length());
      } else {
        jarStorageEntity.jarfileByte = new byte[(int) jarFile.length()];
        fis.read(jarStorageEntity.jarfileByte);
      }
      // Save it
      // session.persist(jarStorageEntity);
      jarDefinitionRepository.save(jarStorageEntity);
      return jarStorageEntity;
    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR,
          "StorageRunner.StorageRunner: Can't load jarFile[" + jarFile.getAbsolutePath() + "]" + e.getMessage());
      throw new TechnicalException(e);
    }
  }

  /**
   * Save the connector to the storage The JAR file may contain multiple runner.
   *
   * @param jarFile jarfile to save
   * @throws IOException error during saving
   */
  public JarStorageEntity saveJarRunner(File jarFile) throws TechnicalException {
    String connectorName = jarFile.getName();
    logger.info("StorageRunner.saveJarRunner: file[{}] connectorName[{}]", jarFile.getPath(), connectorName);

    JarStorageEntity jarStorageEntity = jarDefinitionRepository.findByName(connectorName);
    if (jarStorageEntity != null)
      return jarStorageEntity;

    return updateJarRunner(jarStorageEntity, jarFile);
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
   * @return all StorageEntity in the database
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

  /**
   * Save a runner for the class. For example when the JAR contains multiple class.
   *
   * @param name          name of the runner
   * @param type          type of the runner
   * @param clazz         class of the runner
   * @param jarDefinition jar where the runner is
   * @return a RunnerDefinitionEntity, saved.
   */
  public RunnerDefinitionEntity saveUploadRunner(String name,
                                                 String type,
                                                 Class clazz,
                                                 JarStorageEntity jarDefinition) {
    RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByName(name);
    if (runnerDefinition == null) {
      runnerDefinition = new RunnerDefinitionEntity();
    }
    // now update it (new or update)
    runnerDefinition.name = name;
    runnerDefinition.classname = clazz.getCanonicalName();
    runnerDefinition.type = type;
    runnerDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;

    runnerDefinition.jar = jarDefinition;
    // start it by default
    runnerDefinition.activeRunner = true;
    logger.info("StorageRunner.saveUploadRunner: Save Upload runner name[{}] type[{}] active[{}]",
        runnerDefinition.name, runnerDefinition.type, runnerDefinition.activeRunner);

    return runnerDefinitionRepository.save(runnerDefinition);
  }

  /**
   * Save a RunnerDefinitionEntity in for the Runner and the JarEntity
   *
   * @param runner        Runner to use to create the DefinitionEntity
   * @param jarDefinition Entity which contain the Jar File
   * @return a RunnerDefinitionEntity, saved.
   */
  public RunnerDefinitionEntity saveUploadRunner(AbstractRunner runner, JarStorageEntity jarDefinition) {
    RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByType(runner.getType());
    if (runnerDefinition == null) {
      runnerDefinition = new RunnerDefinitionEntity();
    }
    runnerDefinition.name = runner.getName();
    runnerDefinition.classname = runner.getClass().getCanonicalName();
    runnerDefinition.jar = jarDefinition;
    runnerDefinition.type = runner.getType();
    runnerDefinition.collectionName = runner.getCollectionName();
    runnerDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;

    // start it by default
    runnerDefinition.activeRunner = true;
    logger.info("StorageRunner.saveUploadRunner: Save Upload runner name[{}] type[{}] active[{}]",
        runnerDefinition.name, runnerDefinition.type, runnerDefinition.activeRunner);

    return runnerDefinitionRepository.save(runnerDefinition);
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Operation to manipulate jarFile Blob                                */
  /*                                                                      */
  /*  Manipulate any embedded runner, when the Cherry Library is used     */
  /* ******************************************************************** */

  /**
   * @param jarStorageEntity jarEntity to read from
   * @param outputStream     the Stream to produce the content
   * @throws SQLException the SQL Exception
   * @throws IOException  the IO Exception
   */
  public void readJarBlob(JarStorageEntity jarStorageEntity, OutputStream outputStream)
      throws SQLException, IOException {
    InputStream inputBlobStream = jarStorageEntity.jarfileBlob.getBinaryStream();
    inputBlobStream.transferTo(outputStream);
    outputStream.flush();
  }

  /**
   * Attention, an openSession must be done before
   *
   * @param jarStorageEntity jarEntity to write to
   * @param inputStream      the inputStream where the content is
   * @param length           the length of the data
   */
  public void writeJarBlob(Session session, JarStorageEntity jarStorageEntity, InputStream inputStream, long length) {
    jarStorageEntity.jarfileBlob = session.getLobHelper().createBlob(inputStream, length);

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
    RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByType(runner.getType());
    if (runnerDefinition == null) {
      runnerDefinition = new RunnerDefinitionEntity();
      // start it by default
      runnerDefinition.activeRunner = true;
    }
    runnerDefinition.name = runner.getName();
    runnerDefinition.classname = runner.getClass().getCanonicalName();
    runnerDefinition.type = runner.getType();
    runnerDefinition.collectionName = runner.getCollectionName();
    runnerDefinition.origin = RunnerDefinitionEntity.Origin.EMBEDDED;
    logger.info("StorageRunner.saveEmbeddedRunner: Save Embedded runner name[{}] type[{}] active[{}]",
        runnerDefinition.name, runnerDefinition.type, runnerDefinition.activeRunner);
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
          if (filter.storeOnly == null) {
            return true;
          } else {
            return t.origin == RunnerDefinitionEntity.Origin.STORE;
          }
        }).filter(t -> {
          if (filter.filterName == null)
            return true;
          return t.name.equals(filter.filterName);
        }).filter(t -> {
          if (filter.filterType == null)
            return true;
          return t.type.equals(filter.filterType);
        }).filter(t -> {
          if (filter.jarFileName == null) {
            return true;
          } else {
            return t.jar != null && filter.jarFileName.equals(t.jar.name);
          }
        }).toList();
  }

  /**
   * existRunner by type
   *
   * @param runnerType type of runner
   * @return true if the runner exists
   */
  public boolean existRunnerByType(String runnerType) {
    return runnerDefinitionRepository.selectByType(runnerType) != null;
  }

  /**
   * Remove an entity - does not remove the history of execution
   *
   * @param runnerDefinition entity to remove
   */

  public void removeEntity(RunnerDefinitionEntity runnerDefinition) {
    logger.info("StorageRunner.removeEntity: Remove runner name[{}] type[{}] active[{}]", runnerDefinition.name,
        runnerDefinition.type, runnerDefinition.activeRunner);
    runnerDefinitionRepository.delete(runnerDefinition);
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Remove entity                                                       */
  /*                                                                      */
  /*  Remove the entity     */
  /* ******************************************************************** */

  public static class Filter {
    /**
     * Null: all runners, else True or False
     */
    Boolean activeOnly;

    String filterName;
    String filterType;
    /**
     * We just want the store
     */
    Boolean storeOnly;

    /**
     * Only jar runner inside a specific JarFile
     */
    String jarFileName;

    public Filter isActive(boolean activeOnly) {
      this.activeOnly = activeOnly;
      return this;
    }

    public Filter isStore(boolean storeOnly) {
      this.storeOnly = storeOnly;
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

    public Filter jarFileName(String jarFileName) {
      this.jarFileName = jarFileName;
      return this;
    }
  }
}
