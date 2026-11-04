/* ******************************************************************** */
/*                                                                      */
/*  StorageService                                                       */
/*                                                                      */
/* This class manage the Storage Service                                */
/* A connector can be provided from the upload Path, or uploader, or    */
/* come from the marketplace. It will be saved in the Storage.          */
/* ******************************************************************** */
package io.camunda.cherry.db;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarStorageEntityRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.connector.SdkRunnerConnector;
import io.camunda.cherry.definition.connector.SdkRunnerWorker;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runtime.LogOperation;
import jakarta.annotation.PostConstruct;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class StorageService {

    private final RunnerDefinitionRepository runnerDefinitionRepository;
    private final JarStorageEntityRepository jarStorageEntityRepository;
    private final SessionFactory sessionFactory;
    private final DataSource dataSource;
    private final LogOperation logOperation;
    Logger logger = LoggerFactory.getLogger(StorageService.class.getName());

    public StorageService(RunnerDefinitionRepository runnerDefinitionRepository,
                          JarStorageEntityRepository jarStorageEntityRepository,
                          SessionFactory sessionFactory,
                          DataSource dataSource,
                          LogOperation logOperation) {
        this.runnerDefinitionRepository = runnerDefinitionRepository;
        this.jarStorageEntityRepository = jarStorageEntityRepository;
        this.sessionFactory = sessionFactory;
        this.dataSource = dataSource;
        this.logOperation = logOperation;
    }

    /**
     * Hibernate (ddl-auto: update) generates a Postgres CHECK constraint on ch_operation.operationname
     * from OperationEntity.Operation's values the first time the table is created, but never widens
     * it when the enum later gains a new value - so a database created before e.g. DETECTRUNNER was
     * added rejects any row using it. Drop the stale constraint at startup so an existing database
     * ends up accepting the same values a freshly-created one would; a no-op once it's already gone.
     */
    @PostConstruct
    public void dropStaleOperationNameCheckConstraint() {
        try (Connection con = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(con.getMetaData().getDatabaseProductName()))
                return;
            try (Statement statement = con.createStatement()) {
                statement.execute("ALTER TABLE ch_operation DROP CONSTRAINT IF EXISTS ch_operation_operationname_check");
            }
        } catch (SQLException e) {
            logger.error("StorageService.dropStaleOperationNameCheckConstraint: {}", e.getMessage());
        }
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  JarStorageEntity function                                           */
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
    public JarStorageEntity updateJarRunner(JarStorageEntity jarStorageEntity, String jarName, File jarFile, String release, String sourceJar)
            throws TechnicalException {
        try (FileInputStream fis = new FileInputStream(jarFile);
             Session session = sessionFactory.openSession();
             Connection con = dataSource.getConnection()) {

            if (jarStorageEntity == null) {
                jarStorageEntity = new JarStorageEntity();
            }
            jarStorageEntity.sourceJar = sourceJar;
            jarStorageEntity.name = jarName;
            jarStorageEntity.release = release;
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
            jarStorageEntityRepository.save(jarStorageEntity);
            return jarStorageEntity;
        } catch (Exception e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "StorageService.StorageService: Can't load jarFile[" + jarFile.getAbsolutePath() + "]" + e.getMessage());
            throw new TechnicalException(e);
        }
    }

    /**
     * Save the connector to the storage The JAR file may contain multiple runner.
     *
     * @param jarFile jarfile to save
     * @throws IOException error during saving
     */
    public JarStorageEntity saveJarRunner(String jarName, File jarFile, String release, String sourceJar) throws TechnicalException {
        String connectorName = jarFile.getName();
        logger.info("StorageService.saveJarRunner: file[{}] connectorName[{}] release[{}] sourceJar[{}]",
                jarFile.getPath(), connectorName, release, sourceJar);

        JarStorageEntity jarStorageEntity = jarStorageEntityRepository.findByName(connectorName);
        if (jarStorageEntity != null)
            return jarStorageEntity;

        jarStorageEntity = updateJarRunner(null, jarName, jarFile, release, sourceJar);
        jarStorageEntity.sourceJar = sourceJar;
        return jarStorageEntityRepository.save(jarStorageEntity);
    }

    /**
     * Update the JarStorage
     *
     * @param jarStorageEntity entity to update
     */
    public void updateJarStorage(JarStorageEntity jarStorageEntity) {
        jarStorageEntityRepository.save(jarStorageEntity);
    }

    /**
     * Save a dependency jar (downloaded alongside a connector's main jar) as its own JarStorageEntity
     * row, linked to its parent by name.
     *
     * @param jarName       name of the dependency jar
     * @param jarFile       file content to save
     * @param parentJarName name of the main jar this one is a dependency of
     * @return the saved entity
     * @throws TechnicalException for any error
     */
    public JarStorageEntity saveDependencyJar(String jarName, File jarFile, String parentJarName) throws TechnicalException {
        JarStorageEntity jarStorageEntity = jarStorageEntityRepository.findByName(jarName);
        jarStorageEntity = updateJarRunner(jarStorageEntity, jarName, jarFile, null, "Dependency of [" + parentJarName + "]");
        jarStorageEntity.parent = parentJarName;
        return jarStorageEntityRepository.save(jarStorageEntity);
    }

    /**
     * @param parentJarName name of the main jar
     * @return the dependency jars saved for that main jar
     */
    public List<JarStorageEntity> getDependenciesOf(String parentJarName) {
        return jarStorageEntityRepository.selectByParent(parentJarName);
    }

    public void delete(JarStorageEntity jarStorageEntity) {
        jarStorageEntityRepository.delete(jarStorageEntity);
    }


    /**
     * Upload just the loadLog
     *
     * @param jarStorageEntity jarEntity to update
     * @param loadLog          load loag
     */
    public void uploadLoadLog(JarStorageEntity jarStorageEntity, String loadLog) {
        jarStorageEntityRepository.updateLog(jarStorageEntity.id, loadLog);
        jarStorageEntity.loadLog = loadLog;
    }

    /**
     * get the JarStorageEntity
     *
     * @param jarFileName jar file
     * @return JarStorageEntity, null if not exist
     */
    public JarStorageEntity findJarStorageByName(String jarFileName) {
        return jarStorageEntityRepository.findByName(jarFileName);
    }

    public Optional<JarStorageEntity> findJarStorageById(Long storageEntityId) {
        return jarStorageEntityRepository.findById(storageEntityId);
    }

    /**
     * Return all JAR available
     *
     * @return all StorageEntity in the database
     */
    public List<JarStorageEntity> getAll() {
        return jarStorageEntityRepository.getAll();
    }

    /**
     * check if the JarStorageEntity exist
     *
     * @param jarFileName jar file
     * @return true if exist
     */
    public boolean existJarDefinition(String jarFileName) {
        return jarStorageEntityRepository.findByName(jarFileName) != null;
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
        jarStorageEntity.jarfileBlob = Hibernate.getLobHelper().createBlob(inputStream, length);
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  RunnerDefinitionEntity function                                     */
    /*                                                                      */
    /*  Manipulate runner definition                                        */
    /* ******************************************************************** */

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
                                                   JarStorageEntity jarDefinition,
                                                   String release) {
        RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByName(name);
        if (runnerDefinition == null) {
            runnerDefinition = new RunnerDefinitionEntity();
        }
        // now update it (new or update)
        runnerDefinition.name = name;
        runnerDefinition.classname = clazz.getCanonicalName();
        runnerDefinition.type = type;
        runnerDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;
        runnerDefinition.release = release;
        runnerDefinition.jar = jarDefinition;
        // (re)upload always brings the runner back to ACTIVE, even if it was STOPPED or NOIMPLEMENTATION
        runnerDefinition.state = RunnerDefinitionEntity.State.ACTIVE;
        logger.info("StorageService.saveUploadRunner: Save Upload runner name[{}] type[{}] state[{}]",
                runnerDefinition.name, runnerDefinition.type, runnerDefinition.state);

        return runnerDefinitionRepository.save(runnerDefinition);
    }

    /**
     * Save a RunnerDefinitionEntity in for the Runner and the JarEntity
     *
     * @param runner        Runner to use to create the DefinitionEntity
     * @param jarDefinition Entity which contain the Jar File
     * @return a RunnerDefinitionEntity, saved.
     */
    public RunnerDefinitionEntity saveUploadRunner(AbstractRunner runner, JarStorageEntity jarDefinition, String defaultRelease) {
        RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByType(runner.getType());
        if (runnerDefinition == null) {
            runnerDefinition = new RunnerDefinitionEntity();
        }
        runnerDefinition.name = runner.getName();
        if (runner instanceof SdkRunnerConnector runnerSkkConnector) {
            // we want to save the transported class name
            runnerDefinition.classname = runnerSkkConnector.getTransportedClassName();
        } else if (runner instanceof SdkRunnerWorker sdkRunnerWorker) {
            runnerDefinition.classname = sdkRunnerWorker.getTransportedObject().getClass().getCanonicalName();
        } else {
            runnerDefinition.classname = runner.getClass().getCanonicalName();
        }
        runnerDefinition.jar = jarDefinition;
        runnerDefinition.type = runner.getType();
        runnerDefinition.collectionName = runner.getCollectionName();
        runnerDefinition.origin = RunnerDefinitionEntity.Origin.JARFILE;
        runnerDefinition.release = runner.getRelease() == null || runner.getRelease().trim().isEmpty() ? defaultRelease : runner.getRelease();
        // (re)upload always brings the runner back to ACTIVE, even if it was STOPPED or NOIMPLEMENTATION
        runnerDefinition.state = RunnerDefinitionEntity.State.ACTIVE;
        logger.info("StorageService.saveUploadRunner: Save Upload runner name[{}] type[{}] state[{}]",
                runnerDefinition.name, runnerDefinition.type, runnerDefinition.state);

        return runnerDefinitionRepository.save(runnerDefinition);
    }


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
            // start it by default; an existing entity keeps its persisted state (ACTIVE/STOPPED) across restarts
            runnerDefinition.state = RunnerDefinitionEntity.State.ACTIVE;
        }
        runnerDefinition.name = runner.getName();
        runnerDefinition.classname = runner.getClass().getCanonicalName();
        runnerDefinition.type = runner.getType();
        runnerDefinition.collectionName = runner.getCollectionName();
        runnerDefinition.origin = RunnerDefinitionEntity.Origin.EMBEDDED;
        logger.info("StorageService.saveEmbeddedRunner: Save Embedded runner name[{}] type[{}] state[{}]",
                runnerDefinition.name, runnerDefinition.type, runnerDefinition.state);
        return runnerDefinitionRepository.save(runnerDefinition);
    }


    public List<RunnerDefinitionEntity> selectAllRunnerDefinitionByJarNotNull() {
        return runnerDefinitionRepository.selectAllByJarNotNull();
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Access runner, whatever the origin                                  */
    /*                                                                      */
    /* ******************************************************************** */

    public List<RunnerDefinitionEntity> getRunnersFromJarName(String jarName) {
        return getRunners(new StorageService.Filter().jarFileName(jarName));
    }

    /**
     * Return runner which march the filter
     *
     * @param filter to select runners
     * @return list of Runner Entity
     */
    public List<RunnerDefinitionEntity> getRunners(Filter filter) {
        List<RunnerDefinitionEntity> listRunnerEntity = runnerDefinitionRepository.findAll();
        return listRunnerEntity.stream() // stream to apply each filter
                .filter(t -> {
                    if (filter.filterState == null) {
                        return true;
                    } else {
                        // legacy rows saved before the state column existed default to ACTIVE
                        RunnerDefinitionEntity.State effectiveState = t.state == null ? RunnerDefinitionEntity.State.ACTIVE : t.state;
                        return effectiveState == filter.filterState;
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
     * Persist the state of a runner, so a runner stopped by the user does not restart the next
     * time the application boots.
     *
     * @param runnerType type of the runner
     * @param state      the new state to persist
     */
    public void setRunnerState(String runnerType, RunnerDefinitionEntity.State state) {
        RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByType(runnerType);
        if (runnerDefinition == null) {
            logger.error("StorageService.setRunnerState: runner type[{}] not found, can't persist state[{}]",
                    runnerType, state);
            return;
        }
        runnerDefinition.state = state;
        runnerDefinitionRepository.save(runnerDefinition);
        logger.info("StorageService.setRunnerState: runner type[{}] state[{}]", runnerType, state);
    }

    /**
     * @param runnerType type of the runner
     * @return the persisted state, defaulting to ACTIVE for legacy rows saved before this column existed
     */
    public RunnerDefinitionEntity.State getRunnerState(String runnerType) {
        RunnerDefinitionEntity runnerDefinition = runnerDefinitionRepository.selectByType(runnerType);
        if (runnerDefinition == null || runnerDefinition.state == null)
            return RunnerDefinitionEntity.State.ACTIVE;
        return runnerDefinition.state;
    }

    /**
     * Called when the jar backing this runner is deleted: keep the runner definition (and its
     * execution history) but mark it NOIMPLEMENTATION and detach the jar reference, instead of
     * hard-deleting the definition.
     *
     * @param runnerDefinition entity whose jar was just removed
     */
    public void markRunnerNoImplementation(RunnerDefinitionEntity runnerDefinition) {
        runnerDefinition.state = RunnerDefinitionEntity.State.NOIMPLEMENTATION;
        runnerDefinition.jar = null;
        runnerDefinitionRepository.save(runnerDefinition);
        logger.info("StorageService.markRunnerNoImplementation: runner type[{}] name[{}] marked NOIMPLEMENTATION (jar removed)",
                runnerDefinition.type, runnerDefinition.name);
    }

    /**
     * Remove an entity - does not remove the history of execution
     *
     * @param runnerDefinition entity to remove
     */

    public void deleteRunnerDefinition(RunnerDefinitionEntity runnerDefinition) {
        logger.info("StorageService.removeEntity: Remove runner name[{}] type[{}] state[{}]", runnerDefinition.name,
                runnerDefinition.type, runnerDefinition.state);
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
         * Null: all runners, else only runners with this exact state
         */
        RunnerDefinitionEntity.State filterState;

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

        public Filter state(RunnerDefinitionEntity.State state) {
            this.filterState = state;
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
