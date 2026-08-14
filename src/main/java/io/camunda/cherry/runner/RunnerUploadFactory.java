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
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.connector.api.annotation.OutboundConnector;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
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
    private final JarManagementClassLoader jarManagementClassLoader;
    private final List<RunnerLightDefinition> listLightRunners = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(RunnerUploadFactory.class.getName());

    @Value("${cherry.connectorslib.uploadpath:@null}")
    private String uploadPath;

    @Value("${cherry.connectorslib.forcerefresh:false}")
    private Boolean forceRefresh;

    public RunnerUploadFactory(StorageRunner storageRunner,
                               LogOperation logOperation,
                               JarManagementClassLoader jarManagementClassLoader,
                               SessionFactory sessionFactory) {
        this.storageRunner = storageRunner;
        this.logOperation = logOperation;
        this.jarManagementClassLoader = jarManagementClassLoader;
        this.sessionFactory = sessionFactory;

    }

    private static RunnerLightDefinition getLightFromRunnerDefinitionEntity(RunnerDefinitionEntity entityRunner) {
        return new RunnerLightDefinition(entityRunner.name,
                entityRunner.type,
                entityRunner.classname,
                RunnerDefinitionEntity.Origin.JARFILE,
                entityRunner.release);
    }

    protected void loadConnectorsFromClassLoaderPath() {
        // No special operation to do
    }


    /**
     * get the list from the storage (database), and compare what we have in the class loader.
     * Reload the class in the class loader if needed
     *
     * @param clearAllBefore clear the path before
     * @return listJarLoaded loaded
     */
    public List<String> loadClassLoaderJarsFromStorage(boolean clearAllBefore) {
        List<String> listJarLoaded = new ArrayList<>();

        if (clearAllBefore) {
            jarManagementClassLoader.clearClassLoaderFolder();
        }
        // All JAR file in the database must be load in the JavaMachine
        for (JarStorageEntity jarStorageEntity : storageRunner.getAll()) {
            listJarLoaded.add(jarManagementClassLoader.copyFromJarEntity(jarStorageEntity).getName());
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
        File jarSaved = jarManagementClassLoader.copyFromJarEntity(jarStorageEntity);
        return jarSaved != null;

    }


    public List<File> detectJarFromUploadPath() {

        logger.info("Detect jar in directory[{}]", uploadPath);
        List<File> listJarFiles = new ArrayList<>();

        if (uploadPath == null) {
            logOperation.log(OperationEntity.Operation.SERVERINFO, "No UploadPath is provided");
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
            listJarFiles.add(jarFile);
        }
        return listJarFiles;
    }
    /** TOREMOVE
     * Load all files detected in the upload file to the storageRunner. Update database and factories
     *
     * @return the list of all Runners detected in the uploadPath
     *
    public List<RunnerLightDefinition> loadStorageFromUploadPath() {

    logger.info("Load from directory[{}]", uploadPath);
    List<RunnerLightDefinition> listRunnersDetected = new ArrayList<>();

    if (uploadPath == null) {
    logOperation.log(OperationEntity.Operation.SERVERINFO, "No UploadPath is provided");
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
    JarStorageEntity= saveJarFileToStorage(jarFile, jarFile.getName(), null, forceRefresh);

    listRunnersDetected.addAll(list);
    listLightRunners.addAll(list);

    }
    return listRunnersDetected;
    }
     */

    /**
     * Load a Jar file in the storage and in the factory. All classes are loaded in memory and investigated to find runners.
     * Runners are not started, just loaded in the ClassLoader, and in the Storage
     *
     * @param jarFile            file to load
     * @param originalFileName   the original file name (jarFile maybe a temporary file). If null, use the fileName
     * @param defautRelease      the default release know from where the jar was uploaded
     * @param forceReloadThisJar if true, the storage is uploaded, else depends on the date of the jar in ths storage
     * @return list of runnerLight Definition
     */
    private JarStorageEntity saveJarFileToStorage(File jarFile,
                                                  String originalFileName,
                                                  String defautRelease,
                                                  boolean forceReloadThisJar) {
        List<RunnerLightDefinition> listRunnersLoaded = new ArrayList<>();
        JarStorageEntity jarStorageEntity = null;
        String analysis = "";
        String jarName = originalFileName;
        try {
            jarStorageEntity = storageRunner.getJarStorageByName(originalFileName == null ? jarFile.getName() : originalFileName);
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


            analysis += jarStorageEntity == null ? "SaveEntity" : "UpdateEntity";
            logOperation.log(OperationEntity.Operation.LOADJAR, "Jar[" + jarFile.getName() + "] :" + analysis);
            if (jarStorageEntity == null) {
                // save it
                jarStorageEntity = storageRunner.saveJarRunner(jarName, jarFile);
            } else {
                jarStorageEntity = storageRunner.updateJarRunner(jarStorageEntity, jarName, jarFile);
            }

        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't load JAR [" + jarFile.getName() + "] " + analysis + " : " + e.getMessage());
            return jarStorageEntity;
        }

        return jarStorageEntity;
    }

    /**
     * Install the jar, and return the list of runner detected in the jar.
     * Attention: runners are not stopped/restarted. The runnerFactory can't access the running runner (managed by jobRunnerFactory)
     *
     * @param jarFileName        jar file name
     * @param jarFileInputStream InputStream
     * @param defaultRelease     it may come from the definition, else it somewhere on the runner.
     * @return list of runners detected in the JAR
     */
    public List<RunnerLightDefinition> installJar(String jarFileName, InputStream jarFileInputStream, String defaultRelease) throws TechnicalException {
        try {

            File jarFile = jarManagementClassLoader.copyFromJarFile(jarFileName, jarFileInputStream);
            JarStorageEntity jarStorageEntity = saveJarFileToStorage(jarFile, jarFileName, defaultRelease, true);

            jarManagementClassLoader.loadJarInJavaMachine(jarFileName);
            List<RunnerLightDefinition> runners = inspectJar(jarFile, jarStorageEntity, defaultRelease);

            listLightRunners.addAll(runners);
            logger.info("RunnerUploadFactory jar[{}] installed, found {} runners", jarFileName, runners.size());
            logOperation.log(OperationEntity.Operation.LOADJAR, "UploadJar[" + jarFileName + "]");
            return runners;
        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR, "Can't UploadJar[" + jarFileName + "] : " + e.getMessage());
            throw new TechnicalException("Error install jar [" + jarFileName + "]", e);
        }

    }

    /**
     * Open the JAR file and detect all runners
     * Jar must be ALREADY BE UPLOAD in the Java Machine (see JarManagementClassLoader)
     * For each runner detected, an object is obtains via the  JarManagementClassLoader
     *
     * @param jarFile          jarFile to open
     * @param jarStorageEntity jarStorageEntity related to the JAR file - all runners will be attached to this one
     * @return the runner detected in the jar
     */
    private List<RunnerLightDefinition> inspectJar(File jarFile, JarStorageEntity jarStorageEntity, String defaultRelease) {

        List<RunnerLightDefinition> listRunnersDetected = new ArrayList<>();

        StringBuilder logLoadJar = new StringBuilder();
        StringBuilder errLogLoadJar = new StringBuilder();
        long beginOperation = System.currentTimeMillis();
        logger.info("---- Start Inspect Jar[{}]", jarFile.getPath());


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
                    if (!jarManagementClassLoader.detectRunnersInClass(className, jarFile.getName()))
                        continue;
                    logger.info("Runners detected in class [{}]", className);
                    // We need to instance it to get all runners: it may be multiples
                    Object instanceClass = jarManagementClassLoader.getInstance(className, jarFile.getName());

                    if (instanceClass == null) {
                        logger.error("Can't load class [{}] for connector [{}]", className, jarFile.getName());
                        continue;
                    }
                    List<AbstractRunner> listRunners = jarManagementClassLoader.detectRunnersInObject(instanceClass);

                    for (AbstractRunner runner : listRunners) {
                        // update the release
                        if (runner.getRelease() == null)
                            runner.setRelease(defaultRelease);
                        storageRunner.saveUploadRunner(runner, jarStorageEntity, defaultRelease);
                        listRunnersDetected.add(new RunnerLightDefinition(runner.getName(),
                                runner.getType(),
                                runner.getClass().getName(),
                                RunnerDefinitionEntity.Origin.JARFILE,
                                runner.getRelease() != null ? runner.getRelease() : defaultRelease));
                        nbRunners++;
                    }


                } catch (Error er) {
                    if (className.startsWith("io.camunda")) {
                        logger.info("ErrLoadClass [{}] : {} ", className, er.getMessage());
                        errLogLoadJar.append("ERROR, Class[");
                        errLogLoadJar.append(className);
                        errLogLoadJar.append("]:");
                        errLogLoadJar.append(er.getCause());
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

    /**
     * get All runners
     *
     * @return list of all runners knows byt the factory
     */
    protected List<RunnerLightDefinition> getAllRunners() {
        return listLightRunners;
    }

    /* ready to remove
    private RunnerLightDefinition getLightFromRunner(AbstractRunner runner) {
        return new RunnerLightDefinition(runner.getName(),
                runner.getType(),
                runner.getClass().getName(),
                RunnerDefinitionEntity.Origin.JARFILE,
                runner.getRelease());

    }
*/

    private RunnerLightDefinition getLightFromConnectorAnnotation(OutboundConnector connectorAnnotation, Class clazz, String release) {
        return new RunnerLightDefinition(connectorAnnotation.name(),
                connectorAnnotation.type(),
                clazz.getClass().getName(),
                RunnerDefinitionEntity.Origin.JARFILE,
                release);
    }


}
