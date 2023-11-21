package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.connector.api.annotation.OutboundConnector;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Configuration
public class RunnerUploadFactory {

  private final StorageRunner storageRunner;
  private final LogOperation logOperation;
  private final SessionFactory sessionFactory;
  Logger logger = LoggerFactory.getLogger(RunnerUploadFactory.class.getName());
  private final List<RunnerLightDefinition> listLightRunners = new ArrayList<>();
  @Value("${cherry.connectorslib.uploadpath:@null}")
  private String uploadPath;
  @Value("${cherry.connectorslib.classloaderpath:@null}")
  private String classLoaderPath;
  @Value("${cherry.connectorslib.forcerefresh:false}")
  private Boolean forceRefresh;

  public RunnerUploadFactory(StorageRunner storageRunner, LogOperation logOperation, SessionFactory sessionFactory) {
    this.storageRunner = storageRunner;
    this.logOperation = logOperation;
    this.sessionFactory = sessionFactory;
  }

  private static RunnerLightDefinition getLightFromRunnerDefinitionEntity(RunnerDefinitionEntity entityRunner) {
    RunnerLightDefinition runnerLightDefinition = new RunnerLightDefinition();
    runnerLightDefinition.name = entityRunner.name;
    runnerLightDefinition.type = entityRunner.type;
    runnerLightDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;
    return runnerLightDefinition;
  }

  public void loadConnectorsFromClassLoaderPath() {
    // No special operation to do
  }

  /**
   * get the list from the database, and compare what we have locally. Refresh the local path folder
   *
   * @param clearAllBefore clear the path before
   * @return listJarLoaded loaded
   */
  public List<String> loadJarFromStorage(boolean clearAllBefore) {
    List<String> listJarLoaded = new ArrayList<>();

    if (clearAllBefore) {
      clearFolder(new File(classLoaderPath), false);
    }
    // All JAR file in the database must be load in the JavaMachine
    for (JarStorageEntity jarStorageEntity : storageRunner.getAll()) {
      String jarFileName = classLoaderPath + File.separator + jarStorageEntity.name;
      File saveJarFile = new File(jarFileName);

      try (FileOutputStream outputStream = new FileOutputStream(saveJarFile)) {
        if (jarStorageEntity.jarfileByte != null) {
          outputStream.write(jarStorageEntity.jarfileByte);
        } else {
          storageRunner.readJarBlob(jarStorageEntity, outputStream);
        }
        outputStream.flush();
        listJarLoaded.add(saveJarFile.getName());
      } catch (Exception e) {
        logOperation.log(OperationEntity.Operation.ERROR,
            "Can't save jarFile[" + jarStorageEntity.name + "] to file [" + jarFileName + "] : " + e.getMessage());
      }
    }
    return listJarLoaded;
  }

  public File getClassLoaderPath() {
    return new File(classLoaderPath);
  }

  /**
   * Load all files detected in the upload file to the storageRunner
   */
  public List<RunnerLightDefinition> loadStorageFromUploadPath() {

    logger.info("Load from directory[{}]", uploadPath);
    List<RunnerLightDefinition> listRunnerdDetected = new ArrayList<>();

    if (uploadPath == null) {
      logOperation.log(OperationEntity.Operation.SERVERINFO, "No Uploadpath is provided");
      return Collections.emptyList();
    }
    File uploadFileDir = new File(uploadPath);
    if (!uploadFileDir.exists() || uploadFileDir.listFiles() == null) {
      String defaultDir = System.getProperty("user.dir");
      logger.error("Upload file does not exist [{}] (default is [{}])", uploadPath, defaultDir);
      return Collections.emptyList();
    }
    for (File jarFile : uploadFileDir.listFiles()) {
      if (jarFile.isDirectory())
        continue;
      if (!jarFile.getName().endsWith(".jar"))
        continue;
      logger.info("  Check file [{}]...", jarFile.getName());
      JarStorageEntity jarStorageEntity;
      String analysis = "";
      try {

        jarStorageEntity = storageRunner.getJarStorageByName(jarFile.getName());
        boolean reload = false;
        if (forceRefresh) {
          reload = true;
          analysis += "ForceRefresh,";
        }
        if (jarStorageEntity == null) {
          reload = true;
          analysis += "New,";
        }
        if (jarStorageEntity != null) {

          // Convert the timestamp to a LocalDateTime
          LocalDateTime fileLocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(jarFile.lastModified()),
              ZoneId.systemDefault());

          // transform this local dateTime in UTC, because all comparaison on date in made n UTC
          ZonedDateTime zonedDateTime = fileLocalDateTime.atZone(ZoneId.systemDefault());
          ZonedDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
          LocalDateTime utcLocalDateTime = utcDateTime.toLocalDateTime();

          if (jarStorageEntity.loadedTime.isBefore(utcLocalDateTime)) {
            reload = true;
            analysis += "NewVersion,";
          }
        }
        List<RunnerDefinitionEntity> runners = null;
        if (!reload) {
          // we don't reload the JAR file, so we believe what we have in the database
          runners = storageRunner.getRunners(new StorageRunner.Filter().jarFileName(jarStorageEntity.name));
          // there is something wrong here: why there is no runners behind this JAR?
          if (runners.isEmpty())
            reload = true;
          analysis += "found "+runners.size()+" runners,";
        }
        analysis+="reload:"+reload+",";

        if (!reload && runners != null) {
          listLightRunners.addAll(
              runners.stream().map(RunnerUploadFactory::getLightFromRunnerDefinitionEntity).toList());
          listRunnerdDetected.addAll(
              runners.stream().map(RunnerUploadFactory::getLightFromRunnerDefinitionEntity).toList());
          logOperation.log(OperationEntity.Operation.LOADJAR, "Jar[" + jarFile.getName() + "] :" + analysis);
          continue;
        }

        analysis+= jarStorageEntity==null? "SaveEntity":"UpdateEntity";
        logOperation.log(OperationEntity.Operation.LOADJAR, "Jar[" + jarFile.getName() + "] :" + analysis);
        if (jarStorageEntity == null) {
          // save it
          jarStorageEntity = storageRunner.saveJarRunner(jarFile);
        } else {
          jarStorageEntity = storageRunner.updateJarRunner(jarStorageEntity, jarFile);
        }

      } catch (Exception e) {
        logOperation.log(OperationEntity.Operation.ERROR,
            "Can't load JAR [" + jarFile.getName() + "] " + analysis+" : "+ e.getMessage());
        return listRunnerdDetected;
      }
      loadJarFile(jarFile, jarStorageEntity);
    }
    return listRunnerdDetected;
  }

  public List<RunnerLightDefinition> getAllRunners() {
    return listLightRunners;
  }

  private RunnerLightDefinition getLightFromRunner(AbstractRunner runner) {
    RunnerLightDefinition runnerLightDefinition = new RunnerLightDefinition();
    runnerLightDefinition.name = runner.getName();
    runnerLightDefinition.type = runner.getType();
    runnerLightDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;
    return runnerLightDefinition;
  }

  private RunnerLightDefinition getLightFromConnectorAnnotation(OutboundConnector connectorAnnotation) {
    RunnerLightDefinition runnerLightDefinition = new RunnerLightDefinition();
    runnerLightDefinition.name = connectorAnnotation.name();
    runnerLightDefinition.type = connectorAnnotation.type();
    runnerLightDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;
    return runnerLightDefinition;
  }

  /**
   * Open the JAR file and load all runners detected inside
   *
   * @param jarFile          jarFile to open
   * @param jarStorageEntity jarStorageEntity related to the JAR file - all runners will be attached to this one
   */
  private void loadJarFile(File jarFile, JarStorageEntity jarStorageEntity) {

    StringBuilder logLoadJar = new StringBuilder();
    StringBuilder errLogLoadJar = new StringBuilder();
    long beginOperation = System.currentTimeMillis();
    logger.info("---- Start load Jar[{}]", jarFile.getPath());

    // Explore the JAR file and detect any connector inside
    try (ZipFile zipJarFile = new ZipFile(jarFile);
        URLClassLoader loader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() },
            this.getClass().getClassLoader())) {

      Enumeration<? extends ZipEntry> entries = zipJarFile.entries();
      int nbConnectors = 0;
      int nbRunners = 0;
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        if (entryName.endsWith(".class")) {
          String className = entryName.replace(".class", "").replace('/', '.');
          try {
            Class<?> clazz = loader.loadClass(className);
            OutboundConnector connectorAnnotation = clazz.getAnnotation(OutboundConnector.class);
            if (AbstractRunner.class.isAssignableFrom(clazz)) {
              Object instanceClass = clazz.getDeclaredConstructor().newInstance();
              // this is a AbstractConnector
              AbstractRunner runner = (AbstractRunner) instanceClass;
              storageRunner.saveUploadRunner(runner, jarStorageEntity);
              listLightRunners.add(getLightFromRunner(runner));

              logLoadJar.append("RunnerDectection[");
              logLoadJar.append(runner.getName());
              logLoadJar.append("], type[");
              logLoadJar.append(runner.getType());
              logLoadJar.append("]; ");
              logOperation.log(OperationEntity.Operation.SERVERINFO,
                  "Load Jar[" + jarFile.getName() + "] Runner[" + runner.getName() + "] type[" + runner.getType()
                      + "]");
              nbRunners++;
            } else if (connectorAnnotation != null) {
              // this is a Outbound connector
              storageRunner.saveUploadRunner(connectorAnnotation.name(), connectorAnnotation.type(), clazz,
                  jarStorageEntity);

              listLightRunners.add(getLightFromConnectorAnnotation(connectorAnnotation));

              logLoadJar.append("ConnectorDetection[");
              logLoadJar.append(connectorAnnotation.name());
              logLoadJar.append("], type[");
              logLoadJar.append(connectorAnnotation.type());
              logLoadJar.append("]; ");
              logOperation.log(OperationEntity.Operation.SERVERINFO,
                  "Load Connector[" + connectorAnnotation.name() + "] type[" + connectorAnnotation.type()
                      + "] from Jar[" + jarFile.getName() + "]");
              nbConnectors++;
            }

          } catch (Error er) {
            if (className.startsWith("io.camunda")) {
              logger.info("ErrLoadClass [" + className + "] : " + er.getMessage());
              errLogLoadJar.append("ERROR, Class[");
              errLogLoadJar.append(className);
              errLogLoadJar.append("]:");
              errLogLoadJar.append(er.getMessage());
              errLogLoadJar.append("; ");
            }
          } catch (Exception e) {
            // the class may extend some class which are not present at this moment
            if (className.startsWith("io.camunda")) {
              logger.info("Can't load class [" + className + "] : " + e.getMessage());
              errLogLoadJar.append("ERROR,Class[");
              errLogLoadJar.append(className);
              errLogLoadJar.append("]:");
              errLogLoadJar.append(e.getMessage());
              errLogLoadJar.append("; ");
            }
          }
        }
      }
      // update the Jar information
      long endOperation = System.currentTimeMillis();
      logLoadJar.append(" in ");
      logLoadJar.append(endOperation - beginOperation);
      logLoadJar.append(" ms");

      jarStorageEntity.loadLog = logLoadJar.toString() + errLogLoadJar.toString();
      if (jarStorageEntity.loadLog.length() > 1999)
        jarStorageEntity.loadLog = jarStorageEntity.loadLog.substring(0, 1999);

      storageRunner.updateJarStorage(jarStorageEntity);
      logOperation.log(OperationEntity.Operation.SERVERINFO,
          "Load [" + jarFile.getName() + "] connectors: " + nbConnectors + " runners: " + nbRunners + " in " + (
              endOperation - beginOperation) + " ms ");

    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR,
          "Can't register JAR [" + jarFile.getName() + "] " + e.getMessage());
    } // end manage Zip file

  }

  /**
   * Clear a folder
   *
   * @param folder    folder to clear
   * @param recursive recursive cleaning
   * @return true if all is correct, false in one error was detected
   */
  private boolean clearFolder(File folder, boolean recursive) {
    boolean finalStatus = true;
    File[] files = folder.listFiles();

    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          if (recursive) {
            // Recursively clear subdirectories
            if (!clearFolder(new File(file.getAbsolutePath()), true))
              finalStatus = false;
          }
        } else {
          // Delete files
          if (!file.delete()) {
            logger.error("Failed to delete file: [{}]", file.getAbsolutePath());
            finalStatus = false;
          }
        }
      }

    }
    return finalStatus;
  }
}
