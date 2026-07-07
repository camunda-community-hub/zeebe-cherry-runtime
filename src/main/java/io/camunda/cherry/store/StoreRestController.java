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
import io.camunda.cherry.runner.LogOperation;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runner.StorageRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
                .map(s -> Map.of("name", s.getName(), "url", s.getUrl(), "type", s.getType()))
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
            @RequestParam(name = "stores", required = false) List<String> stores) {
        try {
            logger.info("Start listConnectorInStore {}", stores);
            List<Map<String, Object>> listAllConnectors = new ArrayList<>();

            long beginTime = System.currentTimeMillis();
            List<RunnerDefinitionEntity> listRunnersEntity = runnerFactory.getAllRunnersEntity(new StorageRunner.Filter().isStore(true));
            Map<String, RunnerDefinitionEntity> mapRunnersByName = listRunnersEntity.stream()
                    .collect(Collectors.toMap(x -> x.name, x -> {
                        return x;
                    }));
            Map<String, RunnerDefinitionEntity> mapRunnersByType = listRunnersEntity.stream()
                    .collect(Collectors.toMap(x -> x.type, x -> {
                        return x;
                    }));

            List<StoreAccess> listStoreAccess= new ArrayList<>();
            if (stores != null && !stores.isEmpty()) {
                for (String store : stores) {
                    if (storeFactory.getStoreByName(store) != null) {
                        listStoreAccess.add(storeFactory.getStoreByName(store));
                    }
                }
            }
            List<StoreAccess.ConnectorDefinition> listConnectors = storeFactory.getListConnectorsMergeSource(listStoreAccess);

            // Ok, one connector may have multiple source
            for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {

                Map<String, Object> mapConnector = new HashMap<>();
                mapConnector.put("name", connectorDefinition.name);
                mapConnector.put("storerelease", connectorDefinition.release);
                mapConnector.put("store", connectorDefinition.storeAccess.getName());
                mapConnector.put("url", connectorDefinition.url);
                mapConnector.put("githubRepoName", connectorDefinition.githubRepoName);
                mapConnector.put("githubRepoPath", connectorDefinition.githubRepoPath);
                mapConnector.put("icon", connectorDefinition.icon);
                mapConnector.put("description", connectorDefinition.description);
                mapConnector.put("explorationStatus", connectorDefinition.status);
                mapConnector.put("documentationRef", connectorDefinition.documentationRef);
                mapConnector.put("urlElementTemplate", connectorDefinition.urlElementTemplate);
                mapConnector.put("urlJarFile", connectorDefinition.urlJarFile);
                mapConnector.put("urlMaven", connectorDefinition.urlMaven);
                mapConnector.put("connectorType", connectorDefinition.connectorType);
                mapConnector.put("hasImplementation", connectorDefinition.hasImplementation);
                listAllConnectors.add(mapConnector);
                // get the runnerEntity if present
                RunnerDefinitionEntity runnerEntity = mapRunnersByName.get(connectorDefinition.name);
                if (!connectorDefinition.hasImplementation) {
                    // search if the connector is already present
                    runnerEntity = mapRunnersByType.get(connectorDefinition.connectorType);

                }
                if (runnerEntity == null) {
                    mapConnector.put("status", "NEW");
                } else if (connectorDefinition.release.equals(runnerEntity.release)) {
                    mapConnector.put("status", "UPDATED");
                } else {
                    mapConnector.put("status", "OLD");
                }
                mapConnector.put("currentrelease", runnerEntity == null ? "" : runnerEntity.release);
            }


            logger.info("End listConnectorInStore in {} ms", System.currentTimeMillis() - beginTime);
            return listAllConnectors;

        } catch (TechnicalException e) {
            logger.error("can't access store list ", e);
            logOperation.log(OperationEntity.Operation.ERROR, "Can't access Store " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping(value = "/api/store/connectors/download", produces = "application/json")
    public Map<String, Object> download(@RequestParam(name = "storename", required = false) String storeName,
                                        @RequestParam(name = "connectorname", required = false) String connectorName,
                                        @RequestParam(name = "release", required = false) String release) {
        try {
            Map<String, Object> connectorDownloaded = new HashMap<>();

            StoreAccess.ConnectorDownload connectorDownload = storeFactory.downloadConnector(storeName, connectorName, release);
            // Now, save this new connector

            return connectorDownloaded;
        } catch (TechnicalException e) {
            logOperation.log(OperationEntity.Operation.ERROR,
                    "Can't download connector[" + connectorName + "] " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
