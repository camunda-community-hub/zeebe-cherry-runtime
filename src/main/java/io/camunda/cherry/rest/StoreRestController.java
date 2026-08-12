/* ******************************************************************** */
/*                                                                      */
/*  StoreRestController                                                 */
/*                                                                      */
/*  Rest controller to access the Store Service                         */
/* ******************************************************************** */
package io.camunda.cherry.rest;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.*;
import io.camunda.cherry.store.StoreAccess;
import io.camunda.cherry.store.StoreFactory;
import io.camunda.cherry.supervisor.Supervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class StoreRestController {
    private final StoreFactory storeFactory;
    private final RunnerFactory runnerFactory;
    private final LogOperation logOperation;
    private final JobRunnerFactory jobRunnerFactory;
    private final Supervisor supervisor;
    Logger logger = LoggerFactory.getLogger(StoreRestController.class.getName());


    public StoreRestController(
            StoreFactory storeFactory,
            Supervisor supervisor,
            RunnerFactory runnerFactory,
            JobRunnerFactory jobRunnerFactory,
            LogOperation logOperation) {
        this.storeFactory = storeFactory;
        this.supervisor = supervisor;
        this.runnerFactory = runnerFactory;
        this.logOperation = logOperation;
        this.jobRunnerFactory = jobRunnerFactory;
    }
    /* ******************************************************************** */
    /*                                                                      */
    /*  Store operation                                                     */
    /*                                                                      */
    /* ******************************************************************** */

    @GetMapping(value = "/api/store/list", produces = "application/json")
    public List<Map<String, String>> getStores() {
        List<Map<String, String>> listStores = storeFactory.getStores().stream()
                .map(s -> Map.of(RestAttribute.NAME, s.getName(), RestAttribute.URL, s.getUrl(), RestAttribute.TYPE, s.getType()))
                .toList();
        logger.info("list stores: return {} stores: [{}]", listStores.size(), listStores.stream().map(c -> c.get(RestAttribute.NAME)).collect(Collectors.joining(",")));
        return listStores;
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  connectors store operation                                          */
    /*                                                                      */
    /* ******************************************************************** */
    @GetMapping(value = "/api/store/connectors/explore", produces = "application/json")
    public Map<String, Object> exploreConnectorInStore() {
        if (storeFactory.getExploration() == StoreFactory.EXPLORATIONCONNECTOR.NONE) {
            Executors.newSingleThreadExecutor().execute(() -> storeFactory.explore());
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return listConnectorInStore(null, "nameAsc");
    }

    @GetMapping(value = "/api/store/startup", produces = "application/json")
    public Map<String, Object> getStartupStatus() {
        return populateAdvancement();
    }

    /**
     * @param stores  stores to filter the result
     * @param orderBy order by
     * @return list of connectors, stores
     */
    @GetMapping(value = "/api/store/connectors/list", produces = "application/json")
    public Map<String, Object> listConnectorInStore(
            @RequestParam(name = "stores", required = false) List<String> stores,
            @RequestParam(name = "orderBy", required = false, defaultValue = "nameAsc") String orderBy) {
        try {
            logger.debug("Start listConnectorInStore {}", stores);
            List<Map<String, Object>> listAllConnectors = new ArrayList<>();

            long beginTime = System.currentTimeMillis();
            List<RunnerDefinitionEntity> listRunnersEntity = runnerFactory.getAllRunnersEntity(new StorageRunner.Filter());
            Map<String, RunnerDefinitionEntity> mapRunnersByName = listRunnersEntity.stream()
                    .collect(Collectors.toMap(x -> x.name, x -> x));
            Map<String, RunnerDefinitionEntity> mapRunnersByType = listRunnersEntity.stream()
                    .collect(Collectors.toMap(x -> x.type, x -> x));

            List<StoreAccess> listStoreAccess = new ArrayList<>();
            if (stores != null && !stores.isEmpty()) {
                for (String store : stores) {
                    if (storeFactory.getStoreByName(store) != null) {
                        listStoreAccess.add(storeFactory.getStoreByName(store));
                    }
                }
            }
            List<StoreAccess.ConnectorDefinition> listConnectors = storeFactory.getListConnectorsMergeSource(listStoreAccess);

            for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {

                Map<String, Object> mapConnector = new HashMap<>();
                mapConnector.put(RestAttribute.NAME, connectorDefinition.name);
                mapConnector.put(RestAttribute.STORE_RELEASE, connectorDefinition.release);
                mapConnector.put(RestAttribute.STORE, connectorDefinition.storeAccess.getName());
                mapConnector.put(RestAttribute.URL, connectorDefinition.url);
                mapConnector.put(RestAttribute.GITHUB_REPO_NAME, connectorDefinition.githubRepoName);
                mapConnector.put(RestAttribute.GITHUB_REPO_PATH, connectorDefinition.githubRepoPath);
                mapConnector.put(RestAttribute.ICON, connectorDefinition.icon);
                mapConnector.put(RestAttribute.DESCRIPTION, connectorDefinition.description);
                mapConnector.put(RestAttribute.EXPLORATION_STATUS, connectorDefinition.status);
                mapConnector.put(RestAttribute.DOCUMENTATION_REF, connectorDefinition.documentationRef);
                mapConnector.put(RestAttribute.URL_JAR_FILE, connectorDefinition.urlJarFile);
                mapConnector.put(RestAttribute.URL_MAVEN, connectorDefinition.urlMaven);
                mapConnector.put(RestAttribute.CONNECTOR_TYPE, connectorDefinition.connectorType);
                mapConnector.put(RestAttribute.HAS_IMPLEMENTATION, connectorDefinition.hasImplementation);
                mapConnector.put(RestAttribute.IS_INSTALLABLE, connectorDefinition.isInstallable);
                mapConnector.put(RestAttribute.CREATOR, connectorDefinition.creator);
                mapConnector.put(RestAttribute.ANNOTATIONS, connectorDefinition.listAnnotations);
                mapConnector.put(RestAttribute.ELEMENT_TEMPLATES, connectorDefinition.listEltTemplate);
                listAllConnectors.add(mapConnector);

                if (connectorDefinition.status == StoreAccess.EXPLORATION.INPROGRESS) {
                    mapConnector.put(RestAttribute.STATUS, "IN-PROGRESS");
                } else {
                    /** Let's find a runner for the connection. THe connectorDefinition as a name, but the code is maybe different */

                    RunnerDefinitionEntity runnerEntity = searchRunnerForConnector(connectorDefinition, mapRunnersByName);

                    if (!connectorDefinition.hasImplementation) {
                        runnerEntity = mapRunnersByType.get(connectorDefinition.connectorType);
                    }
                    if (connectorDefinition.connectorType == null) {
                        mapConnector.put(RestAttribute.STATUS, "NO_IMPLEMENTATION");
                    } else if (runnerEntity == null) {
                        mapConnector.put(RestAttribute.STATUS, connectorDefinition.hasImplementation ? "NOT-INSTALLED" : "PARENT-NOT-INSTALLED");
                    } else if (connectorDefinition.release == null) {
                        mapConnector.put(RestAttribute.STATUS, "NO-RELEASE");
                    } else if (runnerEntity.release == null) {
                        mapConnector.put(RestAttribute.STATUS, "NO-RELEASE");
                    } else if (connectorDefinition.release.equals(runnerEntity.release)) {
                        mapConnector.put(RestAttribute.STATUS, "UPDATED");
                    } else {
                        RunnerCompare.COMPARISON comparison = RunnerCompare.compare(connectorDefinition, runnerEntity);
                        switch (comparison) {
                            case RunnerCompare.COMPARISON.ENTITY_NEW:
                                mapConnector.put(RestAttribute.STATUS, "UPDATED");
                                break;
                            case RunnerCompare.COMPARISON.ENTITY_OLD:
                                mapConnector.put(RestAttribute.STATUS, "OLD");
                                break;
                            case RunnerCompare.COMPARISON.EQUALS:
                                mapConnector.put(RestAttribute.STATUS, "UPDATED");
                                break;
                        }
                    }
                    mapConnector.put(RestAttribute.CURRENT_RELEASE, runnerEntity == null ? "" : runnerEntity.release);
                }
            }

            listAllConnectors.sort((a, b) -> {
                switch (orderBy) {
                    case "nameDesc":
                        return String.valueOf(b.get(RestAttribute.NAME)).compareToIgnoreCase(String.valueOf(a.get(RestAttribute.NAME)));
                    case "marketplaceAsc":
                        return String.valueOf(a.get(RestAttribute.STORE)).compareToIgnoreCase(String.valueOf(b.get(RestAttribute.STORE)));
                    case "marketplaceDesc":
                        return String.valueOf(b.get(RestAttribute.STORE)).compareToIgnoreCase(String.valueOf(a.get(RestAttribute.STORE)));
                    default: // nameAsc
                        return String.valueOf(a.get(RestAttribute.NAME)).compareToIgnoreCase(String.valueOf(b.get(RestAttribute.NAME)));
                }
            });

            logger.info("StoreRestController[/api/store/connectors/list}: End listConnectorInStore found {} connectors ({}) in {} ms",
                    listConnectors.size(),
                    listConnectors.stream().map(c -> c.name).collect(Collectors.joining(",")),
                    System.currentTimeMillis() - beginTime);
            List<Map<String, String>> listStores = storeFactory.getStores().stream()
                    .map(s -> Map.of(RestAttribute.NAME, s.getName(), RestAttribute.URL, s.getUrl(), RestAttribute.TYPE, s.getType()))
                    .toList();
            Map<String, Object> status = populateAdvancement();


            return Map.of("connectors", listAllConnectors, "stores", listStores, "status", status);

        } catch (TechnicalException e) {
            logger.error("can't access store list ", e);
            logOperation.log(OperationEntity.Operation.ERROR, "Can't access Store " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping(value = "/api/store/connectors/downloadElementTemplate")
    public ResponseEntity<byte[]> downloadElementTemplate(
            @RequestParam(name = "store") String storeName,
            @RequestParam(name = "connectorName") String connectorName,
            @RequestParam(name = "release") String release) {
        try {
            StoreAccess storeAccess = storeFactory.getStoreByName(storeName);
            if (storeAccess == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeName);
            StoreAccess.ConnectorDefinition connector = storeFactory.getConnectorDefinition(storeAccess, connectorName);
            if (connector == null || connector.listEltTemplate.isEmpty())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Element template not available for: " + connectorName);
            HttpHeaders headers = new HttpHeaders();
            byte[] content;
            if (connector.listEltTemplate.size() == 1) {
                content = fetchUrl(connector.listEltTemplate.get(0).url);
                String filename = connectorName + "-" + release + ".json";
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", filename);
            } else {
                content = createZipElementTemplate(connector.listEltTemplate.stream()
                        .map(c -> c.url)
                        .collect(Collectors.toList()));
                String filename = connectorName + "-" + release + "-templates.zip";
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", filename);
            }
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Can't download element template for {}", connectorName, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch element template: " + e.getMessage());
        }
    }

    @GetMapping(value = "/api/store/connectors/downloadJarFile")
    public ResponseEntity<byte[]> downloadJarFile(
            @RequestParam(name = "store") String storeName,
            @RequestParam(name = "connectorName") String connectorName,
            @RequestParam(name = "release") String release) {
        try {
            StoreAccess storeAccess = storeFactory.getStoreByName(storeName);
            if (storeAccess == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeName);
            StoreAccess.ConnectorDefinition connector = storeFactory.getConnectorDefinition(storeAccess, connectorName);
            if (connector == null || connector.urlJarFile == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "JAR file not available for: " + connectorName);
            byte[] content = fetchUrl(connector.urlJarFile);
            String filename = connectorName + "-" + release + ".jar";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Can't download JAR for {}", connectorName, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch JAR file: " + e.getMessage());
        }
    }

    private byte[] fetchUrl(String urlString) throws IOException {
        try (InputStream in = new URL(urlString).openStream()) {
            return in.readAllBytes();
        }
    }

    private byte[] createZipElementTemplate(List<String> urls) throws IOException {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            for (String urlString : urls) {
                String entryName = urlString.substring(urlString.lastIndexOf('/') + 1);
                byte[] content = fetchUrl(urlString);
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        }
    }

    /**
     * Download the Jar file,
     *
     * @param storeName     store where the connector is
     * @param connectorName connector name
     * @param release       release
     * @return the jar file
     */
    @GetMapping(value = "/api/store/connectors/download")
    public ResponseEntity<byte[]> download(@RequestParam(name = "store", required = false) String storeName,
                                           @RequestParam(name = "connectorname", required = false) String connectorName,
                                           @RequestParam(name = "release", required = false) String release) {
        try {
            StoreAccess.ConnectorDownload connectorDownload = supervisor.download(storeName, connectorName, release);
            if (connectorDownload.jarContent == null)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read JAR content");

            byte[] jarBytes = connectorDownload.jarContent.readAllBytes();
            String filename = connectorName + (release != null ? "-" + release : "") + ".jar";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/java-archive"))
                    .contentLength(jarBytes.length)
                    .body(jarBytes);
        } catch (TechnicalException e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't download connector[" + connectorName + "] " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        }
    }

    /**
     * download the Jar file, install it and start all connectors in the JAR
     *
     * @param storeName     store where the connector is
     * @param connectorName connector name
     * @param release       release
     * @return
     */
    @GetMapping(value = "/api/store/connectors/install", produces = "application/json")
    public StoreAccess.ConnectorDownload install(@RequestParam(name = "store", required = false) String storeName,
                                                 @RequestParam(name = "connectorname", required = false) String connectorName,
                                                 @RequestParam(name = "release", required = false) String release) {
        try {
            logger.info("Start install jar [{}] from store[{}] release[{}]", connectorName, storeName, release);
            StoreAccess.ConnectorDownload connectorDownload = supervisor.downloadAndInstall(storeName, connectorName, release);
            // do not return the content
            connectorDownload.jarContent = null;
            return connectorDownload;

        } catch (TechnicalException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private Map<String, Object> populateAdvancement() {
        Map<String, Object> status = new HashMap<>();
        Map<Supervisor.STARTUPDOWNLOAD, Supervisor.DownloadInProgress> downloadStatus = supervisor.getDownloadStatus();
        List<Map<String, Object>> listDownloadStartup = downloadStatus.values().stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put(RestAttribute.DOWNLOAD_STARTUP_NAME, t.name);
                    map.put(RestAttribute.DOWNLOAD_STARTUP_TOTAL, t.total);
                    map.put(RestAttribute.DOWNLOAD_STARTUP_COUNT, t.count);
                    map.put(RestAttribute.DOWNLOAD_STARTUP_PERCENTAGE, t.total > 0 ? 100 * t.count / t.total : 0);
                    map.put(RestAttribute.DOWNLOAD_STARTUP_CURRENT_NAME, t.currentDownloadName);
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        status.put(RestAttribute.DOWNLOAD_STARTUP, listDownloadStartup);

        StoreFactory.EXPLORATIONCONNECTOR explorationStatus = storeFactory.getExploration();
        status.put("exploration", Map.of("percent", storeFactory.getPercentageExploration(),
                "status", explorationStatus.name()));
        return status;
    }


    private RunnerDefinitionEntity searchRunnerForConnector(StoreAccess.ConnectorDefinition connectorDefinition, Map<String, RunnerDefinitionEntity> mapRunnersByName) {
        if (mapRunnersByName.containsKey(connectorDefinition.name))
            return mapRunnersByName.get(connectorDefinition.name);
        for (StoreAccess.AnnotationDescription annotationDescription : connectorDefinition.listAnnotations) {
            if (mapRunnersByName.containsKey(annotationDescription.name))
                return mapRunnersByName.get(annotationDescription.name);
        }
        return null;

    }
}
