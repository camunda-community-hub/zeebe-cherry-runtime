/* ******************************************************************** */
/*                                                                      */
/*  Supervisor                                                          */
/*                                                                      */
/*  Supervise all main operation. Supervise the startup and             */
/* all operations which needs other factory like StoreFactory JobRunner */
/* ******************************************************************** */
package io.camunda.cherry.supervisor;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.repository.JarStorageEntityRepository;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.*;
import io.camunda.cherry.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class Supervisor {


    private final static int BASE_ADVANCE_PHASE1 = 40;
    private final static int BASE_ADVANCE_PHASE2 = 40;
    private final static int BASE_ADVANCE_PHASE3 = 20;
    private final CherryProperties cherryProperties;
    private final GitHubAccess gitHubAccess;
    private final StoreFactory storeFactory;
    private final RunnerFactory runnerFactory;
    private final JobRunnerFactory jobRunnerFactory;
    private final LogOperation logOperation;
    private final Installer installer;
    private final JarManagementClassLoader jarManagementClassLoader;
    private final RunnerUploadFactory runnerUploadFactory;
    private final JarStorageEntityRepository jarStorageEntityRepository;
    private final Map<StoreAccess, List<StoreAccess.ConnectorDefinition>> mapConnectors = new HashMap<>();
    private final String MARKER_NAME_IS_A_DIRECT_URL = "http";
    Logger logger = LoggerFactory.getLogger(Supervisor.class.getName());
    private final StoreFactory.EXPLORATIONCONNECTOR exploration = StoreFactory.EXPLORATIONCONNECTOR.NONE;
    private final int percentageExploration = 0;
    private final Map<String, DownloadInProgress> downloadStatus = new HashMap<>();


    Supervisor(GitHubAccess gitHubAccess,
               CherryProperties cherryProperties,
               StoreFactory storeFactory,
               RunnerFactory runnerFactory,
               JobRunnerFactory jobRunnerFactory,
               Installer installer,
               LogOperation logOperation, JarManagementClassLoader jarManagementClassLoader, RunnerUploadFactory runnerUploadFactory,
               JarStorageEntityRepository jarStorageEntityRepository) {
        this.gitHubAccess = gitHubAccess;
        this.cherryProperties = cherryProperties;
        this.storeFactory = storeFactory;
        this.runnerFactory = runnerFactory;
        this.jobRunnerFactory = jobRunnerFactory;
        this.installer = installer;
        this.logOperation = logOperation;
        this.jarManagementClassLoader = jarManagementClassLoader;
        this.runnerUploadFactory = runnerUploadFactory;
        this.jarStorageEntityRepository = jarStorageEntityRepository;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Download at startup                                                 */
    /*                                                                      */
    /* ******************************************************************** */

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {

                // Clear the class Loader for a fresh restart
                jarManagementClassLoader.clearClassLoaderFolder();

                // -------------check all library connector
                logger.info("----- Supervisor.1-begin: JarUploadPath");
                List<File> listJarFile = runnerUploadFactory.detectJarFromUploadPath();
                String logInfo = "";
                for (File jarFile : listJarFile) {
                    logInfo += jarFile.getName() + ";";
                    String release = detectReleaseFromName(jarFile.getName());
                    installer.installStartJar(jarFile, release);
                }
                logger.info("----- Supervisor.1-end: Loaded JarUploadPath [{}]", logInfo);

                // ------------- load all from the database
                logger.info("----- Supervisor.2-begin: Loaded from database");
                List<JarStorageEntity> listJarStorage = jarStorageEntityRepository.getAll();
                String logInfoDb = "";
                for (JarStorageEntity jarStorageEntity : listJarStorage) {
                    logInfoDb += jarStorageEntity.name + ";";
                    InputStream jarFileInputStream = readJarContent(jarStorageEntity);
                    installer.installStartJar(jarStorageEntity.name, jarFileInputStream, jarStorageEntity.release);
                }
                logger.info("----- Supervisor.2-end: Loaded from database [{}]", logInfoDb);

                // Explore store
                boolean exploreStores = false;
                boolean downloadStartupNeedExploration = !cherryProperties.getStore().getDownloadStartup().isEmpty();
                if (downloadStartupNeedExploration)
                    exploreStores = true;
                if (! storeFactory.getListStores().isEmpty())
                    exploreStores = true;


                // Now do the job
                logger.info("----- Supervisor.3-begin: Exploration at startup? {} (details: required? {} InitialDownloadByName? {} AskConnectorRuntime? {} askCommunityConnector? {}",
                        exploreStores,
                        downloadStartupNeedExploration,
                        cherryProperties.getStore().getCamundaConnector().isAccess(),
                        cherryProperties.getStore().getCommunityConnector().isAccess());

                // Explore all stores
                if (exploreStores)
                    storeFactory.explore();
                logger.info("----- Supervisor.3-end: Exploration at startup? {} (details: required? {} InitialDownloadByName? {} AskConnectorRuntime? {} askCommunityConnector? {}",
                        exploreStores,
                        downloadStartupNeedExploration,
                        cherryProperties.getStore().getCamundaConnector().isAccess(),
                        cherryProperties.getStore().getCommunityConnector().isAccess());

                // Initial all download
                logger.info("----- Supervisor.4-begin: Initial download");
                initialDownload();

                logger.info("----- Supervisor.4-end: Downloaded");

                jobRunnerFactory.startAll();

            } catch (Exception e) {
                logger.error("StoreFactory exploration failed on startup", e);
            }
        });
    }

    /**
     * JarStorageEntity may hold its content either as a byte[] (jarfileByte, used on Postgres)
     * or as a Blob (jarfileBlob, used on H2) — see the entity's own documentation.
     */
    private InputStream readJarContent(JarStorageEntity jarStorageEntity) throws TechnicalException {
        try {
            if (jarStorageEntity.jarfileByte != null)
                return new ByteArrayInputStream(jarStorageEntity.jarfileByte);
            if (jarStorageEntity.jarfileBlob != null)
                return jarStorageEntity.jarfileBlob.getBinaryStream();
            throw new TechnicalException("JarStorageEntity[" + jarStorageEntity.name + "] has no content (neither jarfileByte nor jarfileBlob)");
        } catch (java.sql.SQLException e) {
            throw new TechnicalException("Cannot read JAR content for [" + jarStorageEntity.name + "]", e);
        }
    }


    public Map<String, DownloadInProgress> getDownloadStatus() {
        return downloadStatus;
    }

    private static final String STARTUP_INITIAL_KEY = "INITIAL";

    /**
     * Manage the initialConnector download
     */
    private void initialDownload() {
        downloadStatus.clear();
        List<StoreAccess> listStoreAccess = storeFactory.getListStores();

        for (StoreAccess storeAccess : listStoreAccess) {
            if (! CherryProperties.DownloadPolicy.NEVER.equals(storeAccess.getStartup().getDownloadPolicy())) {
                String key = storeAccess.getSignature();
                downloadStatus.put(key, new DownloadInProgress(key));
            }
        }
        if (!cherryProperties.getStore().getDownloadStartup().isEmpty()) {
            downloadStatus.put(STARTUP_INITIAL_KEY, new DownloadInProgress(STARTUP_INITIAL_KEY));
        }

        // Now do the job: the ad-hoc "downloadStartup" list of direct URLs / connector names
        if (!cherryProperties.getStore().getDownloadStartup().isEmpty()) {
            DownloadInProgress downloadInProgress = downloadStatus.get(STARTUP_INITIAL_KEY);
            downloadInProgress.total = cherryProperties.getStore().getDownloadStartup().size();
            StoreUrl storeUrl = new StoreUrl(storeFactory, gitHubAccess);
            for (String item : cherryProperties.getStore().getDownloadStartup()) {
                if (item == null)
                    continue;
                StoreAccess.ConnectorDownload connectorDownload = null;
                downloadInProgress.currentDownloadName = item;
                if (item.startsWith(MARKER_NAME_IS_A_DIRECT_URL)) {
                    logger.info("Download And start Downloading URL [{}]", item);
                    // Not possible to call the storeFactory. Call directly the storeUrl to get the connector
                    connectorDownload = storeUrl.downloadConnectorFromUrl(item);
                } else {
                    // search the connector in the list based on name or type, no matter the implementation
                    StoreAccess.ConnectorDefinition connectorDefinition = storeFactory.searchConnector(new StoreFactory.Filter().name(item).type(item));
                    if (connectorDefinition != null) {
                        connectorDownload = installer.downloadInstallStart(connectorDefinition);
                    } else {
                        logger.info("Search connector name[{}]: NOT FOUND", item);
                    }
                }
                downloadInProgress.count++;
            }
        }

        // Generic per-store startup download, driven entirely by each store's own Startup config
        for (StoreAccess storeAccess : listStoreAccess) {
            if (storeAccess.getStartup().getDownloadPolicy().equals(CherryProperties.DownloadPolicy.NEVER))
                continue;

            DownloadInProgress downloadInProgress = downloadStatus.get(storeAccess.getSignature());
            List<String> filter = storeAccess.getStartup().getFilter();
            List<StoreAccess.ConnectorDefinition> listConnectors = sortImplementationFirst(storeFactory.getListConnectors(storeAccess));
            logger.info("Supervisor: start download Store[{}] filter[{}] tag[{}] ConnectorsInTheStore[{}]",
                    storeAccess.getName(), filter, storeAccess.getStartup().getTag(), listConnectors.size());

            downloadInProgress.total = listConnectors.size();
            downloadInProgress.count = 0;
            for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {
                if (matchListFilter(connectorDefinition.name, filter)) {

                    // We apply here the download policy
                    // the connectorDefinition has a release, and identify a JAR. Search the jar version
                    boolean doTheInstallation = checkDownloadPolicy(connectorDefinition, storeAccess.getStartup().getDownloadPolicy());
                    if (doTheInstallation) {
                        downloadInProgress.currentDownloadName = connectorDefinition.name;
                        installer.downloadInstallStart(connectorDefinition);
                    }
                } else {
                    logger.info("Supervisor: ignore connector [{}] due to filter [{}] on download", connectorDefinition.name, filter.stream().collect(Collectors.joining(",")));
                }
                downloadInProgress.count++;
            }
        }

        downloadStatus.clear();
    }

    private boolean matchListFilter(String name, List<String> filters) {
        for (String filter : filters) {
            // Convert wildcard pattern to regex:
            // "." becomes "\." (escape literal dots)
            // "*" becomes ".*" (wildcard to regex)
            String regex = filter.replace(".", "\\.").replace("*", ".*");

            // Test if name matches the pattern
            if (name.matches(regex))
                return true;
        }
        return false;
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  Operations                                                          */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * Download (do not install)
     *
     * @param storeName     store name
     * @param connectorName connector name
     * @param release       release
     * @return the download information
     * @throws TechnicalException any error
     */
    public StoreAccess.ConnectorDownload download(String storeName,
                                                  String connectorName,
                                                  String release) throws TechnicalException {
        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();

        logger.info("Download connector[{}] from store[{}] release[{}]", connectorName, storeName, release);
        // The connector may reference an implementation that is already present

        StoreAccess.ConnectorDefinition connectorDefinition = storeFactory.getConnectorDefinition(storeName, connectorName, release);
        if (connectorDefinition == null) {
            logger.info("Download connector name[{}]: NOT FOUND", connectorName);
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWCONNECTOR;
            return connectorDownload;
        }
        return installer.download(connectorDefinition);
    }

    /**
     * Download (do not install)
     *
     * @param storeName     store name
     * @param connectorName connector name
     * @param release       release
     * @return the download object
     * @throws TechnicalException any error
     */
    public StoreAccess.ConnectorDownload downloadAndInstall(String storeName,
                                                            String connectorName,
                                                            String release) throws TechnicalException {

        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();

        logger.info("downloadAndInstall connector[{}] from store[{}] release[{}]", connectorName, storeName, release);
        // The connector may reference an implementation that is already present

        StoreAccess.ConnectorDefinition connectorDefinition = storeFactory.getConnectorDefinition(storeName, connectorName, release);
        if (connectorDefinition == null) {
            logger.info("downloadAndInstall connector name[{}]: NOT FOUND", connectorName);
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWCONNECTOR;
            return connectorDownload;
        }
        return installer.downloadInstallStart(connectorDefinition);
    }

    /**
     * Instance and start Connector from a JAR
     *
     * @param connectorDownload connector download, install and start it
     * @return the download object
     */
    public StoreAccess.ConnectorDownload startConnector(StoreAccess.ConnectorDownload connectorDownload) {
        try {
            logger.info("Start install jar [{}] ", connectorDownload.jarName);

            // Now install it
            try {
                connectorDownload.runners = installer.installStartJar(connectorDownload.jarName, connectorDownload.jarContent, null);
            } catch (TechnicalException e) {
                connectorDownload.status = StoreAccess.STATUSDOWNLOAD.FAILED;
                connectorDownload.explanation = e.getMessage();
                return connectorDownload;
            }

            for (RunnerLightDefinition runner : connectorDownload.runners) {
                try {
                    jobRunnerFactory.stopRunner(runner.getType());
                } catch (OperationException e) {
                    // do nothing: for a first installation, this is expected
                }

                try {
                    jobRunnerFactory.startRunner(runner.getType());
                    logger.info("start runner[{}] from connector JarName[{}]",
                            runner.getName(), connectorDownload.jarName);
                } catch (
                        Exception e) {
                    logger.error("install : exception ", e);

                    connectorDownload.status = StoreAccess.STATUSDOWNLOAD.FAILED;
                    connectorDownload.explanation = e.getMessage();
                    logOperation.log(OperationEntity.Operation.ERROR,
                            "Can't start connector[" + runner.getName() + "] from DownloadConnector[" + connectorDownload.jarName + "] : " + e.
                                    getMessage());
                }
            }
            // do not return the JAR file
            return connectorDownload;
        } catch (TechnicalException e) {
            throw e;
        } catch (Exception e) {
            logger.error("install : exception ", e);
        }
        return connectorDownload;
    }

    private List<StoreAccess.ConnectorDefinition> sortImplementationFirst(List<StoreAccess.ConnectorDefinition> listConnectors) {
        return listConnectors.stream()
                .sorted((a, b) -> {
                    if (a.hasImplementation && !b.hasImplementation) return -1;
                    if (!a.hasImplementation && b.hasImplementation) return 1;
                    return 0;
                })
                .toList();
    }

    private String detectReleaseFromName(String fileName) {
        Pattern pattern = Pattern.compile("\\b(\\d+)\\.(\\d+)\\.(\\d+)\\b");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }


    private boolean checkDownloadPolicy(StoreAccess.ConnectorDefinition connectorDefinition, CherryProperties.DownloadPolicy downloadPolicy) {
        if (downloadPolicy.equals(CherryProperties.DownloadPolicy.ALWAYS))
            return true;

        return true;
    }

    public enum EXPLORATIONCONNECTOR {NONE, INPROGRESS, COMPLETED}


    public static class DownloadInProgress {
        public String name;
        public int count;
        public int total;
        public String currentDownloadName;
        public String tag;

        protected DownloadInProgress(String name) {
            this.name = name;
        }

    }

}
