package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.connector.api.annotation.OutboundConnector;
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
  @Autowired
  StorageRunner storageRunner;
  @Value("${cherry.connectorslib.uploadpath}")
  private String uploadPath;
  @Value("${cherry.connectorslib.classloaderpath}")
  private String classLoaderPath;
  @Value("${cherry.connectorslib.forcerefresh}")
  private Boolean forceRefresh;

  public void loadConnectorsFromClassLoaderPath() {

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
        outputStream.write(jarStorageEntity.jarfile);
        outputStream.flush();
        outputStream.close();
      } catch (Exception e) {
        logger.error(
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

    logger.info("Load from directory[" + uploadPath + "]");

    File uploadFileDir = new File(uploadPath);
    if (uploadFileDir == null || !uploadFileDir.exists()) {
      String defaultDir = System.getProperty("user.dir");
      logger.error("Upload file does not exist [" + uploadPath + "] (default is [" + defaultDir + "])");
      return;
    }
    for (File jarFile : uploadFileDir.listFiles()) {
      if (jarFile.isDirectory())
        continue;
      if (!jarFile.getName().endsWith(".jar"))
        continue;
      try {

        JarStorageEntity jarStorageEntity = storageRunner.getJarStorageByName(jarFile.getName());
        if (jarStorageEntity != null && !Boolean.TRUE.equals(forceRefresh)) {
          continue;
        }
        StringBuilder logLoadJar = new StringBuilder();
        if (jarStorageEntity == null) {
          // save it
          jarStorageEntity = storageRunner.saveJarRunner(jarFile);
        }
        // Explore the JAR file and detect any connector inside
        ZipFile zipJarFile = new ZipFile(jarFile);

        Enumeration<? extends ZipEntry> entries = zipJarFile.entries();

        URLClassLoader loader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() },
            this.getClass().getClassLoader());

        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          String entryName = entry.getName();
          if (entryName != null && entryName.endsWith(".class")) {
            String className = entryName.replace(".class", "").replace('/', '.');
            try {
              Class<?> clazz = loader.loadClass(className);
              OutboundConnector connectorAnnotation = clazz.getAnnotation(OutboundConnector.class);
              if (connectorAnnotation != null) {
                // this is a Outbound connector
                logLoadJar.append("ConnectorDetection[");
                logLoadJar.append(connectorAnnotation.name());
                logLoadJar.append("], type[");
                logLoadJar.append(connectorAnnotation.type());
                logLoadJar.append("]; ");
                storageRunner.saveUploadRunner(connectorAnnotation.name(), connectorAnnotation.type(), clazz,
                    jarStorageEntity);
              }
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

              }
            } catch (Error er) {
              logger.info("Can't load class [" + className + "] : " + er.getMessage());
              logLoadJar.append("ERROR,Class[");
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
        jarStorageEntity.loadLog = logLoadJar.toString();
        storageRunner.updateJarStorage(jarStorageEntity);
      } catch (Exception e) {
        logger.error("Can't register JAR [" + jarFile.getName() + "] " + e.getMessage());
      }

    }
  }
}
