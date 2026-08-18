/* ******************************************************************** */
/*                                                                      */
/*  Installer                                                           */
/*                                                                      */
/* In charge to supervise install operation: load jar, start runners    */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.supervisor;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.*;
import io.camunda.cherry.store.StoreAccess;
import io.camunda.cherry.store.StoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Component
public class Installer {

    private final RunnerUploadFactory runnerUploadFactory;
    private final StoreFactory storeFactory;
    private final RunnerFactory runnerFactory;
    private final JobRunnerFactory jobRunnerFactory;
    private final LogOperation logOperation;
    private final JarManagementClassLoader jarManagementClassLoader;
    private final StorageRunner storeRunner;
    Logger logger = LoggerFactory.getLogger(Supervisor.class.getName());

    Installer(StoreFactory storeFactory,
              RunnerFactory runnerFactory,
              JobRunnerFactory jobRunnerFactory,
              JarManagementClassLoader jarManagementClassLoader,
              StorageRunner storeRunner,
              LogOperation logOperation, RunnerUploadFactory runnerUploadFactory) {
        this.storeFactory = storeFactory;
        this.runnerFactory = runnerFactory;
        this.jobRunnerFactory = jobRunnerFactory;
        this.jarManagementClassLoader = jarManagementClassLoader;
        this.storeRunner = storeRunner;
        this.logOperation = logOperation;
        this.runnerUploadFactory = runnerUploadFactory;
    }


    public StoreAccess.ConnectorDownload download(StoreAccess.ConnectorDefinition connectorDefinition) throws TechnicalException {
        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();

        logger.info("Download connector [{}] from store[{}] release[{}]", connectorDefinition.name, connectorDefinition.storeAccess.getName(), connectorDefinition.release);
        // The connector may reference an implementation that is already present

        StoreAccess.ConnectorDefinition parentConnector = storeFactory.getParentConnectorDefinition(connectorDefinition);
        if (parentConnector == null) {
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.FAILED;
            connectorDownload.explanation = "Connector [" + connectorDefinition.name
                    + "] name, type[" + connectorDefinition.connectorType
                    + "] has not implementation, no parentConnector for this type is found";
            return connectorDownload;
        }

        return connectorDownload = storeFactory.downloadConnector(parentConnector);
    }


    /**
     * Download and install the jar.
     * Note: the connector is maybe already started: so we will stop all relative runners
     *
     * @param connectorDefinition
     * @return
     * @throws TechnicalException
     */
    public StoreAccess.ConnectorDownload downloadInstallStart(StoreAccess.ConnectorDefinition connectorDefinition) throws TechnicalException {
        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();
        try {
            // This connector may have no implementation, and rely on a different connector
            StoreAccess.ConnectorDefinition parentConnector = storeFactory.getParentConnectorDefinition(connectorDefinition);
            if (parentConnector == null) {
                logger.error("Installer: connector[{}] from store[{}] does not have a parentConnector", connectorDefinition.name, connectorDefinition.storeAccess.getName());
                connectorDownload.status = StoreAccess.STATUSDOWNLOAD.FAILED;
                connectorDownload.explanation = "Connector [" + connectorDefinition.name
                        + "] name, type[" + connectorDefinition.connectorType
                        + "] has not implementation, no parentConnector for this type is found";
                return connectorDownload;
            }
            // Maybe this connector is already loaded actually?

            if (connectorDefinition.urlJarFile == null) {
                // No way to donwload this connector - it should be referencing a non-already present connector OR no jar has provided
                logOperation.log(OperationEntity.Operation.ERROR,
                        "Can't download connector[" + connectorDefinition.name + "] type[" + connectorDefinition.connectorType + "] hasImplementation? " + connectorDefinition.hasImplementation + " no urlJarFile");
                connectorDownload = new StoreAccess.ConnectorDownload();
                if (connectorDefinition.hasImplementation)
                    connectorDownload.status = StoreAccess.STATUSDOWNLOAD.NOURLJARFILE;
                else
                    connectorDownload.status = StoreAccess.STATUSDOWNLOAD.NOIMPLEMENTATION;
                return connectorDownload;
            }
            logger.info("Installer: downloadAndInstall connector[{}] from store[{}] conectorType[{}] hasImplementation[{}] Download from [{}]",
                    connectorDefinition.name,
                    connectorDefinition.storeAccess.getName(),
                    parentConnector.connectorType,
                    connectorDefinition.hasImplementation,
                    connectorDefinition.urlJarFile);

            // download and install
            connectorDownload = storeFactory.downloadConnector(connectorDefinition);
            connectorDownload.runners = runnerUploadFactory.installJar(connectorDownload.jarName, connectorDownload.jarContent, connectorDefinition.release);


            // Synchronize the runnerFactory now, to take into account all new runner loaded
            runnerFactory.synchronize();

            for (RunnerLightDefinition runner : connectorDownload.runners) {
                connectorDefinition.listAnnotations.add(new StoreAccess.AnnotationDescription(runner.getName(), runner.getType()));
                logger.info("Will start runner[{}] from connector [{}] installed and started  install jar [{}] from store[{}] release[{}]",
                        runner.getName(), connectorDefinition.name, connectorDefinition.storeAccess.getName(), connectorDefinition.release);
            }
            connectorDownload.status = startRunner(connectorDownload.runners, connectorDefinition.name);
            return connectorDownload;

        } catch (TechnicalException e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't download connector[" + connectorDefinition.name + "] " + e.getMessage());
            connectorDownload = new StoreAccess.ConnectorDownload();
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.FAILED;
            return connectorDownload;

        }
    }

    public List<RunnerLightDefinition> installStartJar(String jarFileName, InputStream jarFileInputStream, String defaultRelease) throws TechnicalException {
        try {

            List<RunnerLightDefinition> runnerLightDefinitions = runnerUploadFactory.installJar(jarFileName, jarFileInputStream, defaultRelease);
            for (RunnerLightDefinition runner : runnerLightDefinitions) {
                logger.info("Will start runner[{}] from Jar [{}] release[{}]",
                        runner.getName(), jarFileName, defaultRelease);
            }
            startRunner(runnerLightDefinitions, jarFileName);
            return runnerLightDefinitions;
        } catch (Exception e) {
            throw new TechnicalException("Failed to install JAR [" + jarFileName + "]: " + e.getMessage(), e);
        }


    }

    public List<RunnerLightDefinition> installStartJar(File jarFile, String defaultRelease) throws TechnicalException {
        try {
            byte[] jarBytes = Files.readAllBytes(jarFile.toPath());
            ByteArrayInputStream jarFileInputStream = new ByteArrayInputStream(jarBytes);
            return installStartJar(jarFile.getName(), jarFileInputStream, defaultRelease);
        } catch (Exception e) {
            throw new TechnicalException("Failed to install JAR [" + jarFile.getName() + "]: " + e.getMessage(), e);
        }
    }


    public void stopRunner(List<RunnerLightDefinition> runners, String containerName) {
        for (RunnerLightDefinition runner : runners) {
            try {
                jobRunnerFactory.stopRunner(runner.getType());
            } catch (OperationException e) {
                // do nothing: for a first installation, this is expected
            }
        }
    }

    /**
     * Start runners on the list after the installation
     *
     * @param runners
     * @param containerName
     * @return
     */
    public StoreAccess.STATUSDOWNLOAD startRunner(List<RunnerLightDefinition> runners, String containerName) {
        // Update the connectorDefinition with all runners discovered

        StoreAccess.STATUSDOWNLOAD status = StoreAccess.STATUSDOWNLOAD.OK;
        for (RunnerLightDefinition runner : runners) {
            try {
                jobRunnerFactory.stopRunner(runner.getType());
            } catch (OperationException e) {
                // do nothing: for a first installation, this is expected
            }

            try {
                jobRunnerFactory.startRunner(runner.getType());
                logger.info("start runner[{}] from connector [{}] installed and started  install jar [{}] from store[{}] release[{}]",
                        runner.getName(), containerName);
            } catch (Exception e) {
                status = StoreAccess.STATUSDOWNLOAD.FAILED;
                logOperation.log(OperationEntity.Operation.ERROR,
                        "Can't start connector[" + runner.getName() + "] from DownloadConnector[" + containerName + "] : " + e.getMessage());
            }
        }
        return status;
    }

}
