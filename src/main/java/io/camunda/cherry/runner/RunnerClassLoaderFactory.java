/* ******************************************************************** */
/*                                                                      */
/*  RunnerClassLoaderFactory                                            */
/*                                                                      */
/*  This class manage the class loader path                             */
/*  To be accessible to the kjava, the jar must be saved in a folder    */
/*  and then accessible to the Java Machine                             */
/*                                                                      */
/*  NB: to replace a jar file, all runners must be stopped before,      */
/* and this is not the responsability of this factory                   */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;

@Service
public class RunnerClassLoaderFactory {

  private final StorageRunner storageRunner;
  private final LogOperation logOperation;
  Logger logger = LoggerFactory.getLogger(RunnerClassLoaderFactory.class.getName());
  /**
   * To be loaded in the Java Machine, the file must be saved on the filesyztem, in this path
   */
  @Value("${cherry.connectorslib.classloaderpath:@null}")
  private String classLoaderPath;

  public RunnerClassLoaderFactory(StorageRunner storageRunner, LogOperation logOperation) {
    this.storageRunner = storageRunner;
    this.logOperation = logOperation;
  }

  public boolean clearClassLoaderFolder() {
    return clearFolder(new File(classLoaderPath), false);
  }

  /**
   * copy the class loader from the JarStorageEntity
   *
   * @param jarStorageEntity jar to copy in the ClassFolder
   * @return name of jar, null in case of error
   */
  public String copyJarEntity(JarStorageEntity jarStorageEntity) {

    String jarFileName = classLoaderPath + File.separator + jarStorageEntity.name;
    File saveJarFile = new File(jarFileName);

    try (FileOutputStream outputStream = new FileOutputStream(saveJarFile)) {
      if (jarStorageEntity.jarfileByte != null) {
        outputStream.write(jarStorageEntity.jarfileByte);
      } else {
        storageRunner.readJarBlob(jarStorageEntity, outputStream);
      }
      outputStream.flush();
      return saveJarFile.getName();
    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR,
          "Can't save jarFile[" + jarStorageEntity.name + "] to file [" + jarFileName + "] : " + e.getMessage());
      return null;
    }
  }

  /**
   * Load a JarFile in the Java Machine, and return a class which must be in the Jar
   *
   * @param jarFileName jar file to load, assuming it was already copied in the ClassLoader
   * @param className   class name to access
   * @return the class of the classname
   * @throws ClassNotFoundException
   */
  public Class<?> loadClassInJavaMachine(String jarFileName, String className) throws Exception {
    String pathFileName = getClassLoaderPath() + File.separator + jarFileName;
    ClassLoader loader = new URLClassLoader(new URL[] { new File(pathFileName).toURI().toURL() });
    return loader.loadClass(className);
  }

  /**
   * Get the class loader path
   *
   * @return the classloader path
   */
  public File getClassLoaderPath() {
    return new File(classLoaderPath);
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
