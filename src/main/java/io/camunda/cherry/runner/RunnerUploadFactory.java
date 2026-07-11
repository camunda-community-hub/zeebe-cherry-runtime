/* ******************************************************************** */
/*                                                                      */
/*  RunnerUploadFactory                                                 */
/*                                                                      */
/* This factory are in charge to upload jar in the storage (database)   */
/* and in the ClassLoader                                               */
/* Different usages:                                                    */
/*  - from a path "upload" at startup                                   */
/*  - from a upload in the UI                                           */
/*  - from a Marketplace store installation                             */
/*  - from a list of jar to upload at begining                          */
/*                                                                      */
/* ******************************************************************** */
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

import java.io.ByteArrayInputStream;
import java.io.File;
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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Configuration
public class RunnerUploadFactory {

    private final StorageRunner storageRunner;
    private final LogOperation logOperation;
    private final SessionFactory sessionFactory;
    private final RunnerClassLoaderFactory runnerClassLoaderFactory;
    private final List<RunnerLightDefinition> listLightRunners = new ArrayList<>();
    private final RunnerEmbeddedFactory runnerEmbeddedFactory;

    Logger logger = LoggerFactory.getLogger(RunnerUploadFactory.class.getName());

    @Value("${cherry.connectorslib.uploadpath:@null}")
    private String uploadPath;

    @Value("${cherry.connectorslib.forcerefresh:false}")
    private Boolean forceRefresh;

    public RunnerUploadFactory(StorageRunner storageRunner,
                               LogOperation logOperation,
                               RunnerClassLoaderFactory runnerClassLoaderFactory,
                               SessionFactory sessionFactory,
                               RunnerEmbeddedFactory runnerEmbeddedFactory) {
        this.storageRunner = storageRunner;
        this.logOperation = logOperation;
        this.runnerClassLoaderFactory = runnerClassLoaderFactory;
        this.sessionFactory = sessionFactory;
        this.runnerEmbeddedFactory = runnerEmbeddedFactory;

    }

    private static RunnerLightDefinition getLightFromRunnerDefinitionEntity(RunnerDefinitionEntity entityRunner) {
        return new RunnerLightDefinition(entityRunner.name, entityRunner.type, entityRunner.classname,
                RunnerDefinitionEntity.Origin.JARFILE);
    }

    protected void loadConnectorsFromClassLoaderPath() {
        // No special operation to do
    }


    /**
     * Initialise step
     * 1: detect/load all runners in the storage:
     * - from embedbed (thanks to runnerEmbeddedFactory),
     * - UploadPath (runnerUploadFactory)
     * 2; copy all Jar from the storage to the Classloaderpath (runnerUploadFactory)
     * <p>
     * The class does not start any runners
     */
    protected void init() {
        logger.info("----- RunnerFactory.1 Load all embedded runner");

        runnerEmbeddedFactory.registerInternalRunner();

        // second, check all library connector
        logger.info("----- RunnerUploadFactory.2 Load to storage all upload JAR");
        List<RunnerLightDefinition> runnerLightDefinitions = loadStorageFromUploadPath();
        String logInfo = runnerLightDefinitions.stream()
                .map(RunnerLightDefinition::getName)
                .collect(Collectors.joining(","));
        logger.info("Load StorageFromUploadPath [{}]", logInfo);

        // Upload the ClassLoaderPath, and load the class
        logger.info("----- RunnerUploadFactory.3 Load JavaClassLoaderPath from storage");
        List<String> lisJars = loadClassLoaderJarsFromStorage(true);
        logInfo = String.join(",", lisJars);
        logger.info("Load JarUploadPath [{}]", logInfo);

        logger.info("----- RunnerUploadFactory.4 Load configuration Jar (todo)");


    }


    /**
     * get the list from the storage (database), and compare what we have in the class loader.
     * Reload the class in the class loader if needed
     *
     * @param clearAllBefore clear the path before
     * @return listJarLoaded loaded
     */
    protected List<String> loadClassLoaderJarsFromStorage(boolean clearAllBefore) {
        List<String> listJarLoaded = new ArrayList<>();

        if (clearAllBefore) {
            runnerClassLoaderFactory.clearClassLoaderFolder();
        }
        // All JAR file in the database must be load in the JavaMachine
        for (JarStorageEntity jarStorageEntity : storageRunner.getAll()) {
            listJarLoaded.add(runnerClassLoaderFactory.copyJarEntity(jarStorageEntity).getName());
        }
        return listJarLoaded;
    }

    /**
     * Retrieve a file in the database and upload it on the Storage Class Load
     *
     * @param jarFileName jar file name to load
     * @return true if the jar can be loaded in the storage path, else false
     */
    protected boolean jarFileStorageToClassLoader(String jarFileName) {
        JarStorageEntity jarStorageEntity = storageRunner.getJarStorageByName(jarFileName);
        if (jarStorageEntity == null)
            return false;
        File jarSaved = runnerClassLoaderFactory.copyJarEntity(jarStorageEntity);
        return jarSaved != null;

    }

    /**
     * installJar
     *
     * @param jarFileName jarfilename
     * @param jarFileInputStream inputstream with the content
     * @return list of runners detected
     */
    protected List<RunnerLightDefinition> installJar(String jarFileName, ByteArrayInputStream jarFileInputStream) {
        File jarFile = runnerClassLoaderFactory.copyJarFile(jarFileName, jarFileInputStream);
        List<RunnerLightDefinition> runners = saveJarFileToStorage(jarFile, jarFileName, true);
        logger.info("RunnerUploadFactory jar[{}] installed, found {} runners", jarFileName, runners.size());
        return runners;
    }

    /**
     * Load all files detected in the upload file to the storageRunner. Update database and factories
     *
     * @return the list of all Runners detected in the uploadPath
     */
    protected List<RunnerLightDefinition> loadStorageFromUploadPath() {

        logger.info("Load from directory[{}]", uploadPath);
        List<RunnerLightDefinition> listRunnersDetected = new ArrayList<>();

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
            List<RunnerLightDefinition> list = saveJarFileToStorage(jarFile, jarFile.getName(), forceRefresh);
            listRunnersDetected.addAll(list);
            listLightRunners.addAll(list);

        }
        return listRunnersDetected;
    }


    /**
     * Load a Jar file in the storage and in the factory. All classes are loaded in memory and investigated to find runners.
     * Runners are not started, just loaded in the ClassLoader, and in the Storage
     *
     * @param jarFile            file to load
     * @param originalFileName   the original file name (jarFile maybe a temporary file). If null, use the fileName
     * @param forceReloadThisJar if true, the storage is uploaded, else depends on the date of the jar in ths storage
     * @return list of runnerLight Definition
     */
    protected List<RunnerLightDefinition> saveJarFileToStorage(File jarFile,
                                                               String originalFileName,
                                                               boolean forceReloadThisJar) {
        List<RunnerLightDefinition> listRunnersLoaded = new ArrayList<>();
        JarStorageEntity jarStorageEntity;
        String analysis = "";
        String jarName = originalFileName;
        try {
            jarStorageEntity = storageRunner.getJarStorageByName(
                    originalFileName == null ? jarFile.getName() : originalFileName);
            boolean reload = false;
            if (forceReloadThisJar) {
                reload = true;
                analysis += "ForceRefresh,";
            }
            if (jarStorageEntity == null) {
                reload = true;
                analysis += "NewJar,";
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
                runners = storageRunner.getRunnersFromJarName(jarStorageEntity.name);
                // there is something wrong here: why there is no runners behind this JAR?
                if (runners.isEmpty())
                    reload = true;
                analysis += "found " + runners.size() + " runners,";
            }
            analysis += "reload:" + reload + ",";

            if (!reload && runners != null) {
                listRunnersLoaded.addAll(
                        runners.stream().map(RunnerUploadFactory::getLightFromRunnerDefinitionEntity).toList());
                logOperation.log(OperationEntity.Operation.LOADJAR, "Jar[" + jarFile.getName() + "] :" + analysis);
                return listRunnersLoaded;
            }

            analysis += jarStorageEntity == null ? "SaveEntity" : "UpdateEntity";
            logOperation.log(OperationEntity.Operation.LOADJAR, "Jar[" + jarFile.getName() + "] :" + analysis);
            if (jarStorageEntity == null) {
                // save it
                jarStorageEntity = storageRunner.saveJarRunner(jarName, jarFile);
            } else {
                jarStorageEntity = storageRunner.updateJarRunner(jarStorageEntity, jarName, jarFile);
            }
            listRunnersLoaded.addAll(saveStorageJarFile(jarFile, jarStorageEntity));

        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't load JAR [" + jarFile.getName() + "] " + analysis + " : " + e.getMessage());
            return listRunnersLoaded;
        }

        return listRunnersLoaded;
    }

    /**
     * get All runners
     *
     * @return list of all runners knows byt the factory
     */
    protected List<RunnerLightDefinition> getAllRunners() {
        return listLightRunners;
    }

    private RunnerLightDefinition getLightFromRunner(AbstractRunner runner) {
        return new RunnerLightDefinition(runner.getName(), runner.getType(), runner.getClass().getName(),
                RunnerDefinitionEntity.Origin.JARFILE);

    }

    private RunnerLightDefinition getLightFromConnectorAnnotation(OutboundConnector connectorAnnotation) {
        return new RunnerLightDefinition(connectorAnnotation.name(), connectorAnnotation.type(),
                connectorAnnotation.getClass().getName(), RunnerDefinitionEntity.Origin.JARFILE);
    }

    /**
     * Open the JAR file and load all runners detected inside.
     *
     * @param jarFile          jarFile to open
     * @param jarStorageEntity jarStorageEntity related to the JAR file - all runners will be attached to this one
     * @return the runner detected in the jar
     */
    private List<RunnerLightDefinition> saveStorageJarFile(File jarFile, JarStorageEntity jarStorageEntity) {

        List<RunnerLightDefinition> listRunnersDetected = new ArrayList<>();

        StringBuilder logLoadJar = new StringBuilder();
        StringBuilder errLogLoadJar = new StringBuilder();
        long beginOperation = System.currentTimeMillis();
        logger.info("---- Start load Jar[{}]", jarFile.getPath());


        // Explore the JAR file and detect any connector inside
        try (ZipFile zipJarFile = new ZipFile(jarFile);
             URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()},
                     this.getClass().getClassLoader())) {

            int totalClasses = Collections.list(zipJarFile.entries()).size();

            // now process it
            Enumeration<? extends ZipEntry> entries = zipJarFile.entries();
            int nbConnectors = 0;
            int nbRunners = 0;
            int nbClass = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                nbClass++;
                if (nbClass % 1000 == 0 && System.currentTimeMillis() - beginOperation > 2000) {
                    logger.info("JAR [{}] check {}/{} classes in {} ms", jarFile.getName(), nbClass, totalClasses, System.currentTimeMillis() - beginOperation);
                }

                if (!entryName.endsWith(".class")) {
                    continue;
                }
                String className = entryName.replace(".class", "").replace('/', '.');
                // save time
                if (className.startsWith("org.apache")
                        || className.startsWith("com.google")
                        || className.startsWith("scala")
                        || className.startsWith("com.fasterxml"))
                    continue;
                // Connector onboard the CamundaStarter function
                if (className.startsWith("io.camunda.connector.runtime") || className.startsWith("io.camunda.zeebe"))
                    continue;
                try {
                    Class<?> clazz = loader.loadClass(className);
                    OutboundConnector connectorAnnotation = clazz.getAnnotation(OutboundConnector.class);
                    if (AbstractRunner.class.isAssignableFrom(clazz)) {
                        Object instanceClass = clazz.getDeclaredConstructor().newInstance();
                        // this is a AbstractConnector
                        AbstractRunner runner = (AbstractRunner) instanceClass;
                        storageRunner.saveUploadRunner(runner, jarStorageEntity);
                        listRunnersDetected.add(getLightFromRunner(runner));

                        logLoadJar.append("RunnerDetection[");
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

                        listRunnersDetected.add(getLightFromConnectorAnnotation(connectorAnnotation));

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
                        logger.info("ErrLoadClass [{}] : {} ", className, er.getMessage());
                        errLogLoadJar.append("ERROR, Class[");
                        errLogLoadJar.append(className);
                        errLogLoadJar.append("]:");
                        errLogLoadJar.append(er.getMessage());
                        errLogLoadJar.append("; ");
                    }
                } catch (Exception e) {
                    // the class may extend some class which are not present at this moment
                    if (className.startsWith("io.camunda")) {
                        logger.info("Can't load class [{}] : {}", className, e.getMessage());
                        errLogLoadJar.append("ERROR,Class[");
                        errLogLoadJar.append(className);
                        errLogLoadJar.append("]:");
                        errLogLoadJar.append(e.getMessage());
                        errLogLoadJar.append("; ");
                    }

                }
            }
            // update the Jar information
            long endOperation = System.currentTimeMillis();
            logLoadJar.append(" in ");
            logLoadJar.append(endOperation - beginOperation);
            logLoadJar.append(" ms");

            String logLoadJarSt = logLoadJar.toString() + errLogLoadJar;
            if (logLoadJarSt.length() > 1999)
                logLoadJarSt = logLoadJarSt.substring(0, 1999);

            storageRunner.uploadLoadLog(jarStorageEntity, logLoadJarSt);
            logOperation.log(OperationEntity.Operation.SERVERINFO,
                    "Load [" + jarFile.getName() + "] connectors: " + nbConnectors + " runners: " + nbRunners + " in " + (
                            endOperation - beginOperation) + " ms ");

        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't register JAR [" + jarFile.getName() + "] " + e.getMessage());
        } // end manage Zip file
        return listRunnersDetected;
    }

}
