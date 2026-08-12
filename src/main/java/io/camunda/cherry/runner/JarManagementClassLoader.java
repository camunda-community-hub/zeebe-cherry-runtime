/* ******************************************************************** */
/*                                                                      */
/*  JarManagementClassLoader                                                */
/*                                                                      */
/*  This class manage the class loader path, and load Jar in the Java   */
/*  To be accessible to the java, the jar must be saved in a folder     */
/*  and then accessible to the Java Machine                             */
/*                                                                      */
/*  This class:                                                         */
/*     - can return the list of Jar present in the folder               */
/*     - upload a new Jar                                               */
/*  Does not:                                                           */
/*      - introspect a JAR to find worker / connector                   */
/*      - never deal with worker (stop, start)                          */
/*      - don't save the Jar in the database                            */
/*                                                                      */
/*  NB: to replace a jar file, all runners must be stopped before,      */
/* and this is not the responsability of this factory                   */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.connector.SdkRunnerCherryConnector;
import io.camunda.cherry.definition.connector.SdkRunnerConnector;
import io.camunda.cherry.definition.connector.SdkRunnerWorker;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.outbound.OutboundConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class JarManagementClassLoader {

    private final StorageRunner storageRunner;
    private final LogOperation logOperation;
    private final ConcurrentHashMap<String, URLClassLoader> jarClassLoaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConfigurableApplicationContext> jarContexts = new ConcurrentHashMap<>();
    Logger logger = LoggerFactory.getLogger(JarManagementClassLoader.class.getName());
    /**
     * To be loaded in the Java Machine, the file must be saved on the filesyztem, in this path
     */
    @Value("${cherry.connectorslib.classloaderpath:@null}")
    private String classLoaderPath;
    @Autowired(required = false)
    private ApplicationContext parentContext;

    public JarManagementClassLoader(StorageRunner storageRunner, LogOperation logOperation) {
        this.storageRunner = storageRunner;
        this.logOperation = logOperation;
    }

    /**
     * Detect classical runner in an object
     *
     * @param candidateRunner object to search inside
     * @return list of runners detected
     */
    public static List<AbstractRunner> detectRunnersInObject(Object candidateRunner) {

        List<AbstractRunner> listDetectedRunners = new ArrayList<>();

        if (AbstractRunner.class.isAssignableFrom(candidateRunner.getClass())) {
            listDetectedRunners.add((AbstractRunner) candidateRunner);
            return listDetectedRunners;
        }
        if (candidateRunner instanceof OutboundConnectorFunction outboundConnector) {

            // we have two kind of SDK runner :
            // the classical connector
            // the Cherry Enrichment Connector
            if (SdkRunnerCherryConnector.isRunnerCherryConnector(candidateRunner.getClass())) {
                listDetectedRunners.add(new SdkRunnerCherryConnector(outboundConnector));
            } else {
                listDetectedRunners.add(new SdkRunnerConnector(outboundConnector));
            }

            return listDetectedRunners;
        }

        if (candidateRunner instanceof OutboundConnectorProvider outboundConnectorProvider) {
            listDetectedRunners.add(new SdkRunnerConnector(outboundConnectorProvider));
            return listDetectedRunners;
        }


        for (Method method : candidateRunner.getClass().getMethods()) {
            io.camunda.client.annotation.JobWorker annotation = method.getAnnotation(io.camunda.client.annotation.JobWorker.class);
            if (annotation != null) listDetectedRunners.add(new SdkRunnerWorker(candidateRunner, annotation, method));
        }
        return listDetectedRunners;
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
    public File copyFromJarEntity(JarStorageEntity jarStorageEntity) {

        String jarFileName = classLoaderPath + File.separator + jarStorageEntity.name;
        File saveJarFile = new File(jarFileName);

        try (FileOutputStream outputStream = new FileOutputStream(saveJarFile)) {
            if (jarStorageEntity.jarfileByte != null) {
                outputStream.write(jarStorageEntity.jarfileByte);
            } else {
                storageRunner.readJarBlob(jarStorageEntity, outputStream);
            }
            outputStream.flush();
            return saveJarFile;
        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR, "Can't save jarFile[" + jarStorageEntity.name + "] to file [" + jarFileName + "] : " + e.getMessage());
            return null;
        }
    }

    public File copyFromJarFile(String jarFileName, InputStream jarFileInputStream) {
        String fullPath = classLoaderPath + File.separator + jarFileName;
        logger.info("Copying inputStream to jar[" + jarFileName + "] to path[" + fullPath + "]");
        File saveJarFile = new File(fullPath);

        try (FileOutputStream outputStream = new FileOutputStream(saveJarFile)) {
            jarFileInputStream.transferTo(outputStream);
            outputStream.flush();
            return saveJarFile;
        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR, "Can't save jarFile[" + jarFileName + "] to file [" + fullPath + "] : " + e.getMessage());
            return null;
        }
    }

    /**
     * Remove the JarFile
     *
     * @param jarFileName the jar file name to remove
     */
    public void removeJarFile(String jarFileName) {
        String fullPath = classLoaderPath + File.separator + jarFileName;
        Path fileToDelete = Path.of(fullPath);
        try {
            // if a UrlClassLoader was created for this jar, then Close it before
            URLClassLoader urlClassLoader = jarClassLoaders.get(jarFileName);
            if (urlClassLoader != null) {
                urlClassLoader.close();
            }
            // Now we can delete it
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            logOperation.log(OperationEntity.Operation.ERROR, "Can't save jarFile[" + jarFileName + "] to file [" + fullPath + "] : " + e.getMessage());
        }
    }

    /**
     * Load a JAR file and create a Spring context to discover and instantiate all Spring components.
     * If the JAR contains @Component, @Service, @Bean, etc., Spring will auto-discover and instantiate them.
     *
     * @param jarFileName jar file to load (must already be in classLoaderPath)
     */
    public void loadJarInJavaMachine(String jarFileName) {
        try {
            String pathFileName = classLoaderPath + File.separator + jarFileName;
            File jarFile = new File(pathFileName);

            if (!jarFile.exists()) {
                logger.warn("JAR file not found: {}", pathFileName);
                return;
            }

            // Create URLClassLoader for the JAR
            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            if (parent == null) {
                parent = getClass().getClassLoader();
            }

            URLClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, parent);

            // Store the classloader for later use
            jarClassLoaders.put(jarFileName, jarClassLoader);

            // Create and configure Spring context with the JAR's ClassLoader
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(jarClassLoader);

                try {
                    // Try with auto-configuration first
                    GenericWebApplicationContext context = new GenericWebApplicationContext();
                    if (parentContext != null) {
                        context.setParent(parentContext);
                    }
                    context.setClassLoader(jarClassLoader);
                    context.setResourceLoader(new DefaultResourceLoader(jarClassLoader));
                    context.refresh();

                    jarContexts.put(jarFileName, context);
                    logger.info("Successfully loaded JAR [{}] with Spring context", jarFileName);

                } catch (Exception e) {
                    logger.warn("Failed to load JAR [{}] with auto-configuration, using minimal context: {}", jarFileName, e.getMessage());

                    // Fallback: Create minimal context without auto-configuration
                    GenericWebApplicationContext context = new GenericWebApplicationContext();
                    if (parentContext != null) {
                        context.setParent(parentContext);
                    }
                    context.setClassLoader(jarClassLoader);
                    context.setResourceLoader(new DefaultResourceLoader(jarClassLoader));
                    context.refresh();

                    jarContexts.put(jarFileName, context);
                    logger.info("Successfully loaded JAR [{}] with minimal Spring context", jarFileName);
                }

            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }

        } catch (Exception e) {
            logger.error("Failed to load JAR [{}]: {}", jarFileName, e.getMessage(), e);
        }
    }

    /**
     * Load a class from a specific loaded JAR using its isolated ClassLoader.
     * This method properly handles ClassLoader isolation for dynamically loaded JARs.
     *
     * @param className   fully qualified class name
     * @param jarFileName JAR file where the class is located
     * @return the Class object, or null if not found
     */
    public Class<?> getClassFromJar(String className, String jarFileName) {
        try {
            // Try to get from Spring context first (if available)
            ConfigurableApplicationContext context = jarContexts.get(jarFileName);
            if (context != null) {
                try {
                    return context.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    logger.debug("Class not found in Spring context, trying direct URLClassLoader");
                }
            }

            // Fall back to URLClassLoader
            URLClassLoader classLoader = jarClassLoaders.get(jarFileName);
            if (classLoader == null) {
                logger.warn("No ClassLoader found for JAR [{}]", jarFileName);
                return null;
            }

            return classLoader.loadClass(className);

        } catch (ClassNotFoundException e) {
            logger.error("Class [{}] not found in JAR [{}]: {}", className, jarFileName, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Failed to load class [{}] from JAR [{}]: {}", className, jarFileName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create an instance of a class from a loaded JAR.
     * Handles both Spring-managed beans and direct instantiation via reflection.
     *
     * @param className   fully qualified class name
     * @param jarFileName JAR file where the class is located
     * @return new instance of the class, or null if not found or instantiation fails
     */
    public Object createInstanceFromJar(String className, String jarFileName) {
        try {
            Class<?> clazz = getClassFromJar(className, jarFileName);
            if (clazz == null) {
                return null;
            }

            // Try to get Spring-managed instance first
            ConfigurableApplicationContext context = jarContexts.get(jarFileName);
            if (context != null) {
                try {
                    Map<String, ?> beans = context.getBeansOfType(clazz);
                    if (!beans.isEmpty()) {
                        return beans.values().iterator().next();
                    }
                } catch (Exception e) {
                    logger.debug("Not a Spring bean, creating via reflection");
                }
            }

            // Direct instantiation via reflection
            return clazz.getDeclaredConstructor().newInstance();

        } catch (Exception e) {
            logger.error("Failed to create instance of [{}] from JAR [{}]: {}", className, jarFileName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get a runner instance from a specific loaded JAR.
     * - If it's a Spring component (@Component, @Service, etc.), returns the Spring-managed instance
     * - Otherwise, creates a new instance via reflection
     *
     * @param className   fully qualified class name
     * @param jarFileName JAR file where the class is located
     * @return the instance, or null if not found
     */
    public Object getInstance(String className, String jarFileName) {
        try {
            // Get the Spring context for this JAR
            ConfigurableApplicationContext context = jarContexts.get(jarFileName);
            if (context == null) {
                logger.warn("No Spring context found for JAR [{}]", jarFileName);
                return null;
            }

            // Try to get as Spring bean
            try {
                Class<?> clazz = context.getClassLoader().loadClass(className);
                if (context.containsBean(className) || context.getBeansOfType(clazz).size() > 0) {
                    // It's a Spring component, get it from the context
                    Map<String, ?> beans = context.getBeansOfType(clazz);
                    if (!beans.isEmpty()) {
                        return beans.values().iterator().next();
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.debug("Class not found in context: {}", className);
            }

            // Not a Spring component, try direct instantiation
            URLClassLoader classLoader = jarClassLoaders.get(jarFileName);
            if (classLoader == null) {
                logger.warn("No ClassLoader found for JAR [{}]", jarFileName);
                return null;
            }

            Class<?> clazz = classLoader.loadClass(className);
            return clazz.getDeclaredConstructor().newInstance();

        } catch (Exception e) {
            logger.error("Failed to get runner [{}] from JAR [{}]: {}", className, jarFileName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Load a JarFile in the Java Machine, and return a class which must be in the Jar
     *
     * @param jarFileName jar file to load, assuming it was already copied in the ClassLoader
     * @param className   class name to access
     * @return the class of the classname
     * @throws ClassNotFoundException can arrive
     */
    public Class<?> loadClassInJavaMachine2(String jarFileName, String className) throws Exception {
        String pathFileName = getClassLoaderPath() + File.separator + jarFileName;

        // Load inside Docker: we need to search the parentClassloader, else the load will failed
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = getClass().getClassLoader();
        }

        logger.debug("Loading class from jar file [" + pathFileName + "] using classLoader[" + parent.getName() + "]");
        ClassLoader loader = new URLClassLoader(new URL[]{new File(pathFileName).toURI().toURL()}, parent);

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
                        if (!clearFolder(new File(file.getAbsolutePath()), true)) finalStatus = false;
                    }
                } else {
                    if (file.getName().equals("README.md")) {
                        continue;
                    }
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

    public boolean detectRunnersInClass(String runnerClassName, String jarFileName) {
        // Skip anonymous inner classes
        try {
            if (runnerClassName.contains("$")) {
                return false;
            }
            ConfigurableApplicationContext context = jarContexts.get(jarFileName);
            if (context == null) {
                logger.info("No Spring context found for JAR [{}]", jarFileName);
            }

            // Try to get the class name now
            Class runnerClass;
            if (context != null) runnerClass = context.getClassLoader().loadClass(runnerClassName);
            else runnerClass = Class.forName(runnerClassName);

            // Check if it's an AbstractRunner
            if (AbstractRunner.class.isAssignableFrom(runnerClass)) {
                return true;
            }

            // Check if it's an OutboundConnectorFunction
            if (OutboundConnectorFunction.class.isAssignableFrom(runnerClass)) {
                return true;
            }

            // Check if it's an OutboundConnectorProvider
            if (OutboundConnectorProvider.class.isAssignableFrom(runnerClass)) {
                return true;
            }

            // Check if it's a SdkRunnerCherryConnector
            if (SdkRunnerCherryConnector.isRunnerCherryConnector(runnerClass)) {
                return true;
            }

            // Check if it has a @JobWorker annotated method
            for (Method method : runnerClass.getMethods()) {
                io.camunda.client.annotation.JobWorker annotation = method.getAnnotation(io.camunda.client.annotation.JobWorker.class);
                if (annotation != null) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            logger.info("Can't instantiate class[{}] : {}", runnerClassName, e.getMessage(), e);
            return false;
        }
    }

}
