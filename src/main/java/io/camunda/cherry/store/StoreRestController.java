/* ******************************************************************** */
/*                                                                      */
/*  StoreRestController                                                 */
/*                                                                      */
/*  Rest controller to access the Store Service                         */
/* ******************************************************************** */
package io.camunda.cherry.store;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.rest.RestAttribute;
import io.camunda.cherry.runner.LogOperation;
import io.camunda.cherry.runner.RunnerCompare;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runner.StorageRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class StoreRestController {

    Logger logger = LoggerFactory.getLogger(StoreRestController.class.getName());

    @Autowired
    StoreFactory storeFactory;

    @Autowired
    RunnerFactory runnerFactory;

    @Autowired
    LogOperation logOperation;


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
        logger.info("list stores: return {} stores", listStores.size());
        return listStores;
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  connectors store operation                                          */
    /*                                                                      */
    /* ******************************************************************** */

    @GetMapping(value = "/api/store/connectors/list", produces = "application/json")
    public List<Map<String, Object>> listConnectorInStore(
            @RequestParam(name = "stores", required = false) List<String> stores,
            @RequestParam(name = "orderBy", required = false, defaultValue = "nameAsc") String orderBy) {
        try {
            logger.info("Start listConnectorInStore {}", stores);
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
                mapConnector.put(RestAttribute.URL_ELEMENT_TEMPLATE, connectorDefinition.urlElementTemplate);
                mapConnector.put(RestAttribute.URL_JAR_FILE, connectorDefinition.urlJarFile);
                mapConnector.put(RestAttribute.URL_MAVEN, connectorDefinition.urlMaven);
                mapConnector.put(RestAttribute.CONNECTOR_TYPE, connectorDefinition.connectorType);
                mapConnector.put(RestAttribute.HAS_IMPLEMENTATION, connectorDefinition.hasImplementation);
                listAllConnectors.add(mapConnector);

                RunnerDefinitionEntity runnerEntity = mapRunnersByName.get(connectorDefinition.name);
                if (!connectorDefinition.hasImplementation) {
                    runnerEntity = mapRunnersByType.get(connectorDefinition.connectorType);
                }

                if (runnerEntity == null) {
                    mapConnector.put(RestAttribute.STATUS, "NOT-INSTALLED");
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

            logger.info("End listConnectorInStore in {} ms", System.currentTimeMillis() - beginTime);
            return listAllConnectors;

        } catch (
                TechnicalException e) {
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
            if (connector == null || connector.urlElementTemplate == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Element template not available for: " + connectorName);
            byte[] content = fetchUrl(connector.urlElementTemplate);
            String filename = connectorName + "-" + release + ".json";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
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

    @GetMapping(value = "/api/store/connectors/download", produces = "application/json")
    public Map<String, Object> download(@RequestParam(name = "storename", required = false) String storeName,
                                        @RequestParam(name = "connectorname", required = false) String connectorName,
                                        @RequestParam(name = "release", required = false) String release) {
        try {
            Map<String, Object> connectorDownloaded = new HashMap<>();
            StoreAccess.ConnectorDownload connectorDownload = storeFactory.downloadConnector(storeName, connectorName, release);
            return connectorDownloaded;
        } catch (TechnicalException e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't download connector[" + connectorName + "] " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
