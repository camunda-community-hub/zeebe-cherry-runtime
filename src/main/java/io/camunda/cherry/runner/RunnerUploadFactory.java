package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.connector.api.annotation.OutboundConnector;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Configuration
public class RunnerUploadFactory {

  Logger logger = LoggerFactory.getLogger(RunnerUploadFactory.class.getName());
  private final StorageRunner storageRunner;

  private final LogOperation logOperation;

  @Value("${cherry.connectorslib.uploadpath:@null}")
  private String uploadPath;

  @Value("${cherry.connectorslib.classloaderpath:@null}")
  private String classLoaderPath;

  @Value("${cherry.connectorslib.forcerefresh:false}")
  private Boolean forceRefresh;

  private final SessionFactory sessionFactory;

  public RunnerUploadFactory(StorageRunner storageRunner, LogOperation logOperation,  SessionFactory sessionFactory) {
    this.storageRunner = storageRunner;
    this.logOperation = logOperation;
    this.sessionFactory = sessionFactory;
  }


  public void loadConnectorsFromClassLoaderPath() {
    // No special operation to do
  }

  /**
   * get the list from the database, and compare what we have locally. Refresh the local file
   */
  public void loadJavaFromStorage() {
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

      } catch (Exception e) {
        logOperation.log(OperationEntity.Operation.ERROR,
            "Can't save jarFile[" + jarStorageEntity.name + "] to file [" + jarFileName + "] : " + e.getMessage());
      }
    }
  }

  public File getClassLoaderPath() {
    return new File(classLoaderPath);
  }

  /**
   * Load all files detected in the upload file to the storageRunner
   */
  public void loadStorageFromUploadPath() {

    logger.info("Load from directory[{}]", uploadPath);

    if (uploadPath == null) {
      logOperation.log(OperationEntity.Operation.SERVERINFO, "No Uploadpath is provided");
      return;
    }
    File uploadFileDir = new File(uploadPath);
    if (!uploadFileDir.exists() || uploadFileDir.listFiles() == null) {
      String defaultDir = System.getProperty("user.dir");
      logger.error("Upload file does not exist [{}] (default is [{}])", uploadPath, defaultDir);
      return;
    }
    for (File jarFile : uploadFileDir.listFiles()) {
      if (jarFile.isDirectory())
        continue;
      if (!jarFile.getName().endsWith(".jar"))
        continue;
      JarStorageEntity jarStorageEntity;
      try {

        jarStorageEntity = storageRunner.getJarStorageByName(jarFile.getName());
        if (jarStorageEntity != null && !Boolean.TRUE.equals(forceRefresh)) {
          continue;
        }
        if (jarStorageEntity == null) {
          // save it
          jarStorageEntity = storageRunner.saveJarRunner(jarFile);
        }
      } catch (Exception e) {
        logOperation.log(OperationEntity.Operation.ERROR,
            "Can't load JAR [" + jarFile.getName() + "] " + e.getMessage());
        return;
      }

      StringBuilder logLoadJar = new StringBuilder();
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

                logLoadJar.append("RunnerDectection[");
                logLoadJar.append(runner.getName());
                logLoadJar.append("], type[");
                logLoadJar.append(runner.getType());
                logLoadJar.append("]; ");
                logOperation.log(OperationEntity.Operation.SERVERINFO,
                    "Load Jar[" + jarFile.getName() + "] Runner[" + runner.getName() + "] type["
                        + runner.getType() + "]");
                nbRunners++;
              } else if (connectorAnnotation != null) {
                // this is a Outbound connector
                storageRunner.saveUploadRunner(connectorAnnotation.name(), connectorAnnotation.type(), clazz,
                    jarStorageEntity);

                logLoadJar.append("ConnectorDetection[");
                logLoadJar.append(connectorAnnotation.name());
                logLoadJar.append("], type[");
                logLoadJar.append(connectorAnnotation.type());
                logLoadJar.append("]; ");
                logOperation.log(OperationEntity.Operation.SERVERINFO,
                    "Load Jar[" + jarFile.getName() + "] Connector[" + connectorAnnotation.name() + "] type["
                        + connectorAnnotation.type() + "]");
                nbConnectors++;
              }

            } catch (Error er) {
              logger.info("Can't load class [" + className + "] : " + er.getMessage());
              logLoadJar.append("ERROR, Class[");
              logLoadJar.append(className);
              logLoadJar.append("]:");
              logLoadJar.append(er.getMessage());
              logLoadJar.append("; ");

            } catch (Exception e) {
              // the class may extends some class which are not present at this moment
              logger.info("Can't load class [" + className + "] : " + e.getMessage());
              logLoadJar.append("ERROR,Class[");
              logLoadJar.append(className);
              logLoadJar.append("]:");
              logLoadJar.append(e.getMessage());
              logLoadJar.append("; ");
            }
          }
        }
        // update the Jar information
        long endOperation = System.currentTimeMillis();
        logLoadJar.append(" in ");
        logLoadJar.append(endOperation - beginOperation);
        logLoadJar.append(" ms");

        jarStorageEntity.loadLog = logLoadJar.toString();
        storageRunner.updateJarStorage(jarStorageEntity);
        logOperation.log(OperationEntity.Operation.SERVERINFO,
            "Load [" + jarFile.getPath() + "] connectors: " + nbConnectors + " runners: " + nbRunners + " in "
                + (endOperation - beginOperation) + " ms ");

      } catch (Exception e) {
        logOperation.log(OperationEntity.Operation.ERROR,
            "Can't register JAR [" + jarFile.getName() + "] " + e.getMessage());
      } // end manage Zip file

    }
  }
}
