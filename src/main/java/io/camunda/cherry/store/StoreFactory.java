package io.camunda.cherry.store;

import io.camunda.cherry.exception.TechnicalException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreFactory {

    Logger logger = LoggerFactory.getLogger(StoreFactory.class.getName());

    public List<StoreAccess> listStoreAccess = new ArrayList<>();

    StoreFactory(GitHubAccess gitHubAccess, CherryProperties cherryProperties) {

        listStoreAccess.add(new StoreCamundaConnector(gitHubAccess));
        listStoreAccess.add(new StoreCamundaCommunity(gitHubAccess));
        listStoreAccess.add(new StoreMarketPlace());

        for (String repoUrl : cherryProperties.getStores()) {
            String name = extractName(repoUrl);
            listStoreAccess.add(new StorePrivateGithub(name, repoUrl, gitHubAccess));
        }
    }

    public List<String> getStoreNames() {
        return listStoreAccess.stream().map(StoreAccess::getName).toList();
    }

    public List<StoreAccess> getStores() {
        return listStoreAccess;
    }

    public StoreAccess getStoreByName(String name) {
        return listStoreAccess.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }






    /* ******************************************************************** */
    /*                                                                      */
    /*  Explore the connector world                                         */
    /*                                                                      */
    /* ******************************************************************** */

    private Map<StoreAccess, List<StoreAccess.ConnectorDefinition>> mapConnectors = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
        explore();
        // Now, check if some runner must be downloaded and started
    }


    public StoreAccess.ConnectorDefinition getConnectorDefinition(StoreAccess storeAccess, String connectorName) {
        List<StoreAccess.ConnectorDefinition> connectors = mapConnectors.get(storeAccess);
        for (StoreAccess.ConnectorDefinition connectorDefinition : connectors) {
            if (connectorDefinition.name.equals(connectorName)) {
                return connectorDefinition;
            }
        }
        return null;
    }

    public List<StoreAccess.ConnectorDefinition> getListConnectors(StoreAccess storeAccess) {
        return mapConnectors.get(storeAccess);
    }


    /**
     * A connector may have multiple store.
     * For example, connector from ConnectorStore are referenced in the marketplace. or connector in CamundaHub the same
     * The key is the type. Same type = same connector. Even the name may be different.
     *
     * First, the list of build from this source
     *  - CamundaStore : the most valuable
     *  - CamundaHub
     *  - Private Github
     *  - Marketplace
     * @return
     */
    public List<StoreAccess.ConnectorDefinition> getListConnectorsMergeSource(List<StoreAccess> filterStoreAccess) {
        List<StoreAccess> listStoreOrdered = new ArrayList<>();

        // 1. StoreCamundaConnector first
        listStoreAccess.stream()
                .filter(s -> s instanceof StoreCamundaConnector)
                .forEach(listStoreOrdered::add);

        // 2. StorePrivateGithub instances
        listStoreAccess.stream()
                .filter(s -> s instanceof StorePrivateGithub)
                .forEach(listStoreOrdered::add);

        // 3. Any other store type not yet added and not StoreMarketPlace
        listStoreAccess.stream()
                .filter(s -> !(s instanceof StoreMarketPlace) && !listStoreOrdered.contains(s))
                .forEach(listStoreOrdered::add);

        // 4. StoreMarketPlace last
        listStoreAccess.stream()
                .filter(s -> s instanceof StoreMarketPlace)
                .forEach(listStoreOrdered::add);

        if (!filterStoreAccess.isEmpty()) {
            listStoreOrdered.retainAll(filterStoreAccess);
        }

        // Merge: first occurrence of a connectorType wins
        Map<String, StoreAccess.ConnectorDefinition> merged = new java.util.LinkedHashMap<>();
        for (StoreAccess storeAccess : listStoreOrdered) {
            List<StoreAccess.ConnectorDefinition> connectors = mapConnectors.get(storeAccess);
            if (connectors == null)
                continue;
            for (StoreAccess.ConnectorDefinition connector : connectors) {
                String key = connector.connectorType != null ? connector.connectorType : connector.name;
                merged.putIfAbsent(key, connector);
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(c -> c.name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Explore all connectors
     */
    public void explore() {
        logger.info("---- Start exploration of connectors");
        long beginTime = System.currentTimeMillis();
        mapConnectors.clear();
        int nbConnectors = 0;
        for (StoreAccess storeAccess : listStoreAccess) {
            logger.info("Pass 1. Explore store[{}]", storeAccess.getName());
            List<StoreAccess.ConnectorDefinition> listConnectors = storeAccess.getListConnectors();
            for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {
                connectorDefinition.status = StoreAccess.EXPLORATION.INPROGRESS;
            }
            nbConnectors += listConnectors.size();
            mapConnectors.put(storeAccess, listConnectors);
        }
        logger.info("All connectors {} discovered in {} ms", nbConnectors, System.currentTimeMillis() - beginTime);


        // Ok, now replay all connectors and explore them
        for (Map.Entry<StoreAccess, List<StoreAccess.ConnectorDefinition>> entry : mapConnectors.entrySet()) {
            StoreAccess storeAccess = entry.getKey();
            logger.info("Pass 2 - Deep exploration store[{}]", storeAccess.getName());
            long startTimeDeep = System.currentTimeMillis();
            int nbFullyCorrects = 0;
            int nbIncorrect = 0;
            for (StoreAccess.ConnectorDefinition connectorDefinition : entry.getValue()) {
                // Explore this connection
                try {
                    storeAccess.exploreDetails(connectorDefinition);
                    connectorDefinition.status = connectorDefinition.urlElementTemplate != null && connectorDefinition.urlJarFile != null ?
                            StoreAccess.EXPLORATION.READY : StoreAccess.EXPLORATION.FAILED;
                    logger.info("Store[{}] connector [{}] type:[{}] release[{}] Status[{}] HasImplementation?{} url[{}] Description[{}] Gitname name[{}] path[{}] urlJarFile[{}] urlElementTemplate[{}]" ,
                            storeAccess.getName(),
                            connectorDefinition.name,
                            connectorDefinition.connectorType,
                            connectorDefinition.release,
                            connectorDefinition.hasImplementation,
                            connectorDefinition.status,
                            connectorDefinition.url,
                            connectorDefinition.description,
                            connectorDefinition.githubRepoName,
                            connectorDefinition.githubRepoPath,
                            connectorDefinition.urlJarFile,
                            connectorDefinition.urlElementTemplate
                    );

                } catch (TechnicalException e) {
                    logger.error("StoreFactory Store[{}] connector [{}] failed ", storeAccess.getName(), connectorDefinition.name, e);
                    connectorDefinition.status = StoreAccess.EXPLORATION.FAILED;
                }
                if (connectorDefinition.status == StoreAccess.EXPLORATION.READY)
                    nbFullyCorrects++;
                else
                    nbIncorrect++;
            }
            logger.info("Deep exploration finish on Store[{}] in {} ms on {} connectors, correct:{} Incorrect:{}",
                    storeAccess.getName(),
                    System.currentTimeMillis() - startTimeDeep,
                    nbFullyCorrects,
                    nbIncorrect);

        }

        logger.info("---- End exploration of all stores/connectors in {} ms", System.currentTimeMillis() - beginTime);


    }

    public StoreAccess.ConnectorDownload downloadConnector(String storeName, String connectorName, String release) {
        StoreAccess storeAccess = getFromName(storeName);
        if (storeAccess == null) {
            return null;
        }
        StoreAccess.ConnectorDefinition connectorDefinition = getConnectorDefinition(storeAccess, connectorName);
        if (connectorDefinition == null) {
            return null;
        }

        return storeAccess.downloadConnector(connectorDefinition);
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  private                                                             */
    /*                                                                      */
    /* ******************************************************************** */


    private StoreAccess getFromName(String storeName) {
        return listStoreAccess.stream().filter(s -> s.getName().equals(storeName)).findFirst().orElse(null);
    }


    private String extractName(String url) {
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }
}
