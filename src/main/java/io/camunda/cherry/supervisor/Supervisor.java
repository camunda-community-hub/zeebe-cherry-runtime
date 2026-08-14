/* ******************************************************************** */
/*                                                                      */
/*  Supervisor                                                          */
/*                                                                      */
/*  Supervise all main operation. Supervise the startup and             */
/* all operations which needs other factory like StoreFactory JobRunner */
/* ******************************************************************** */
package io.camunda.cherry.supervisor;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.*;
import io.camunda.cherry.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<StoreAccess, List<StoreAccess.ConnectorDefinition>> mapConnectors = new HashMap<>();
    private final String MARKER_NAME_IS_A_DIRECT_URL = "http";
    Logger logger = LoggerFactory.getLogger(Supervisor.class.getName());
    private final StoreFactory.EXPLORATIONCONNECTOR exploration = StoreFactory.EXPLORATIONCONNECTOR.NONE;
    private final int percentageExploration = 0;
    private final Map<STARTUPDOWNLOAD, DownloadInProgress> downloadStatus = new HashMap<>();


    Supervisor(GitHubAccess gitHubAccess,
               CherryProperties cherryProperties,
               StoreFactory storeFactory,
               RunnerFactory runnerFactory,
               JobRunnerFactory jobRunnerFactory,
               Installer installer,
               LogOperation logOperation, JarManagementClassLoader jarManagementClassLoader, RunnerUploadFactory runnerUploadFactory) {
        this.gitHubAccess = gitHubAccess;
        this.cherryProperties = cherryProperties;
        this.storeFactory = storeFactory;
        this.runnerFactory = runnerFactory;
        this.jobRunnerFactory = jobRunnerFactory;
        this.installer = installer;
        this.logOperation = logOperation;
        this.jarManagementClassLoader = jarManagementClassLoader;
        this.runnerUploadFactory = runnerUploadFactory;
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

                // second, check all library connector
                logger.info("----- Supervisor.1 Load from UploadPath ");
                List<File> listJarFile = runnerUploadFactory.detectJarFromUploadPath();
                String logInfo = "";
                for (File jarFile : listJarFile) {
                    logInfo += jarFile.getName() + ";";
                    installer.installStartJar(jarFile, null);
                }


                logger.info("Supervisor.2: Load JarUploadPath [{}]", logInfo);


                boolean exploreStores = false;
                boolean downloadStartupNeedExploration = !cherryProperties.getStore().getDownloadStartup().isEmpty();
                if (downloadStartupNeedExploration)
                    exploreStores = true;
                // some other mechanism may force the exploration
                if (cherryProperties.getStore().getCamundaConnector().isAccess())
                    exploreStores = true;
                if (cherryProperties.getStore().getCommunityConnector().isAccess())
                    exploreStores = true;

                // Now do the job
                logger.info("----- Supervisor.3: Exploration at startup? {} (details: required? {} InitialDownloadByName? {} AskConnectorRuntime? {} askCommunityConnector? {}",
                        exploreStores,
                        downloadStartupNeedExploration,
                        cherryProperties.getStore().getCamundaConnector().isAccess(),
                        cherryProperties.getStore().getCommunityConnector().isAccess());

                // Explore all stores
                if (exploreStores)
                    storeFactory.explore();

                // Initial all download
                logger.info("----- Supervisor.4: Download");
                initialDownload();

                jobRunnerFactory.startAll();

            } catch (Exception e) {
                logger.error("StoreFactory exploration failed on startup", e);
            }
        });
    }


    public Map<STARTUPDOWNLOAD, DownloadInProgress> getDownloadStatus() {
        return downloadStatus;
    }

    /**
     * Manage the initialConnector download
     */
    private void initialDownload() {
        downloadStatus.clear();
        if (!cherryProperties.getStore().getDownloadStartup().isEmpty()) {
            downloadStatus.put(STARTUPDOWNLOAD.INITIAL,
                    new DownloadInProgress(STARTUPDOWNLOAD.INITIAL));
        }
        if (cherryProperties.getStore().getCamundaConnector().getStartup().isDownload()) {
            downloadStatus.put(STARTUPDOWNLOAD.CAMUNDA_CONNECTOR,
                    new DownloadInProgress(STARTUPDOWNLOAD.CAMUNDA_CONNECTOR));
        }
        if (cherryProperties.getStore().getCommunityConnector().getStartup().isDownload()) {
            downloadStatus.put(STARTUPDOWNLOAD.COMMUNITY_CONNECTOR, new DownloadInProgress(STARTUPDOWNLOAD.COMMUNITY_CONNECTOR));
        }

        // Now do the job
        if (!cherryProperties.getStore().getDownloadStartup().isEmpty()) {
            DownloadInProgress downloadInProgress = downloadStatus.get(STARTUPDOWNLOAD.INITIAL);
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
        if (cherryProperties.getStore().getCamundaConnector().getStartup().isDownload()) {
            logger.info("Supervisor: start download Camunda Connectors release[{}]",
                    cherryProperties.getStore().getCamundaConnector().getStartup().getTag());
            DownloadInProgress downloadInProgress = downloadStatus.get(STARTUPDOWNLOAD.CAMUNDA_CONNECTOR);
            StoreAccess storeAccess = storeFactory.getStoreCamundaConnector();
            if (storeAccess != null) {
                List<StoreAccess.ConnectorDefinition> listConnectors = sortImplementationFirst(storeFactory.getListConnectors(storeAccess));
                downloadInProgress.total = listConnectors.size();
                downloadInProgress.count = 0;
                List<String> filter = cherryProperties.getStore().getCamundaConnector().getStartup().getFilter();
                if (filter != null && !filter.isEmpty()) {
                    logger.info("Supervisor: Camunda Connector Filter[{}]", filter);
                }
                for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {
                    if (matchListFilter(connectorDefinition.name, filter)) {
                        downloadInProgress.currentDownloadName = connectorDefinition.name;
                        installer.downloadInstallStart(connectorDefinition);
                    } else {
                        logger.info("Supervisor: ignore connector [{}] due to filter [{}] on download", connectorDefinition.name, filter.stream().collect(Collectors.joining(",")));
                    }
                    downloadInProgress.count++;
                }
            }
        }


        if (cherryProperties.getStore().getCommunityConnector().getStartup().isDownload()) {
            logger.info("StoreFactory: start download Community Connector filter[{}]",
                    cherryProperties.getStore().getCommunityConnector().getStartup().getFilter());
            DownloadInProgress downloadInProgress = downloadStatus.get(STARTUPDOWNLOAD.COMMUNITY_CONNECTOR);


            List<String> filter = cherryProperties.getStore().getCommunityConnector().getStartup().getFilter();
            StoreAccess storeAccess = storeFactory.getStoreCommunity();
            if (storeAccess != null) {
                List<StoreAccess.ConnectorDefinition> listConnectors = sortImplementationFirst(storeFactory.getListConnectors(storeAccess));
                downloadInProgress.total = listConnectors.size();
                downloadInProgress.count = 0;
                for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {
                    // download this connector
                    if (matchListFilter(connectorDefinition.name, filter)) {
                        downloadInProgress.currentDownloadName = connectorDefinition.name;
                        installer.downloadInstallStart(connectorDefinition);
                    } else {
                        logger.info("Supervisor: ignore connector [{}] due to filter [{}] on download", connectorDefinition.name, filter.stream().collect(Collectors.joining(",")));
                    }
                    // we count +1 here
                    downloadInProgress.count++;
                }
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

    public enum EXPLORATIONCONNECTOR {NONE, INPROGRESS, COMPLETED}

    public enum STARTUPDOWNLOAD {INITIAL, CAMUNDA_CONNECTOR, COMMUNITY_CONNECTOR}

    public static class DownloadInProgress {
        public STARTUPDOWNLOAD name;
        public int count;
        public int total;
        public String currentDownloadName;
        public String tag;

        protected DownloadInProgress(STARTUPDOWNLOAD name) {
            this.name = name;
        }

    }

}
