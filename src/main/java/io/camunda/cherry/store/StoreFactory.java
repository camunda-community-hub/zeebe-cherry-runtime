package io.camunda.cherry.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StoreFactory {

    public List<StoreAccess> listStoreAccess = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(StoreFactory.class.getName());
    private final Map<StoreAccess, List<StoreAccess.ConnectorDefinition>> mapConnectors = new HashMap<>();

    private boolean explorationInProcess = false;
    private int percentageExploration = 0;


    private int BASE_ADVANCE_PHASE1=40;
    private int BASE_ADVANCE_PHASE2=40;
    private int BASE_ADVANCE_PHASE3=20;

    StoreFactory(GitHubAccess gitHubAccess, CherryProperties cherryProperties) {

        listStoreAccess.add(new StoreCamundaConnector(gitHubAccess));
        listStoreAccess.add(new StoreCamundaCommunity(gitHubAccess));
        listStoreAccess.add(new StoreMarketPlace(gitHubAccess));

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


    public boolean isExplorationInProcess() {
        return explorationInProcess;
    }

    public int getPercentageExploration() {
        return percentageExploration;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Explore the connector world                                         */
    /*                                                                      */
    /* ******************************************************************** */

    public StoreAccess getStoreByName(String name) {
        return listStoreAccess.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                explore();
            } catch (Exception e) {
                logger.error("StoreFactory exploration failed on startup", e);
            }
        });
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
     * <p>
     * First, the list of build from this source
     * - CamundaStore : the most valuable
     * - CamundaHub
     * - Private GitHub
     * - Marketplace
     *
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

        // Merge: first occurrence of a connectorType wins, based on type
        Map<String, StoreAccess.ConnectorDefinition> merged = new java.util.LinkedHashMap<>();
        for (StoreAccess storeAccess : listStoreOrdered) {
            List<StoreAccess.ConnectorDefinition> connectors = mapConnectors.get(storeAccess);
            if (connectors == null)
                continue;
            for (StoreAccess.ConnectorDefinition connector : connectors) {
                // The connector is identified to be on marketplace and have a origin from Hub or Camunda Connector: ignore it.
                if (connector.connectorSource != StoreAccess.CONNECTORSOURCE.NONE)
                    continue;
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
    public synchronized void explore() {
        logger.info("---- Start exploration of connectors");
        explorationInProcess = true;
        percentageExploration=0;
        long beginTime = System.currentTimeMillis();
        mapConnectors.clear();
        int nbConnectors = 0;
        int countStore = 0;
        for (StoreAccess storeAccess : listStoreAccess) {
            percentageExploration =  (int) ( ((double) countStore)/listStoreAccess.size())*BASE_ADVANCE_PHASE1;
            countStore++;
            // first 40% on pass 1
            logger.info("~~~~~ Pass 1.({}/{}) Explore store[{}]", countStore, listStoreAccess.size(), storeAccess.getName());
            List<StoreAccess.ConnectorDefinition> listConnectors = storeAccess.exploreListConnectors();
            for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectors) {
                connectorDefinition.status = StoreAccess.EXPLORATION.INPROGRESS;
                logger.debug("Store[{}] connector[{}] in url[{}] Implementation[{}]", storeAccess.getName(), connectorDefinition.name, connectorDefinition.url, connectorDefinition.hasImplementation);
            }
            nbConnectors += listConnectors.size();
            mapConnectors.put(storeAccess, listConnectors);
        }
        logger.info("~~~~~ END Pass 1. {} connectors identified in {} ms", nbConnectors, System.currentTimeMillis() - beginTime);
        percentageExploration= BASE_ADVANCE_PHASE1;

        // Ok, now replay all connectors and explore them
        countStore = 0;
        int totalStores = mapConnectors.entrySet().size();
        for (Map.Entry<StoreAccess, List<StoreAccess.ConnectorDefinition>> entry : mapConnectors.entrySet()) {
            countStore++;
            StoreAccess storeAccess = entry.getKey();
            logger.info("~~~~~ Pass 2.({}/{}) - Deep exploration store[{}]", countStore, listStoreAccess.size(), storeAccess.getName());
            long startTimeDeep = System.currentTimeMillis();
            int nbFullyCorrects = 0;
            int nbIncorrect = 0;
            int countConnectors=0;
            int totalConnectors = entry.getValue().size();
            double storeSlice = (double) BASE_ADVANCE_PHASE2 / totalStores;
            List<StoreAccess.ConnectorDefinition> connectorsToRemove = new ArrayList<>();
            for (StoreAccess.ConnectorDefinition connectorDefinition : entry.getValue()) {
                countConnectors++;
                // Explore this connection
                double completedStores = (countStore - 1); // current store not yet finished
                double connectorProgress = totalConnectors > 0 ? (double) countConnectors / totalConnectors : 0;
                percentageExploration = (int) (BASE_ADVANCE_PHASE1 + completedStores * storeSlice + connectorProgress * storeSlice);
                try {
                    boolean isValid = storeAccess.exploreDetails(connectorDefinition);
                    if (!isValid) {
                        connectorsToRemove.add(connectorDefinition);
                        continue;
                    }
                    connectorDefinition.status = connectorDefinition.urlElementTemplate != null ?
                            StoreAccess.EXPLORATION.READY : StoreAccess.EXPLORATION.INCOMPLETE;
                    logger.info("Store[{}] connector[{}] type:[{}] release[{}] Status[{}] HasImplementation? {} url[{}] Description[{}] Gitname name[{}] path[{}] urlJarFile[{}] urlElementTemplate[{}]",
                            storeAccess.getName(),
                            connectorDefinition.name,
                            connectorDefinition.connectorType,
                            connectorDefinition.release,
                            connectorDefinition.status,
                            connectorDefinition.hasImplementation,
                            connectorDefinition.url,
                            connectorDefinition.description,
                            connectorDefinition.githubRepoName,
                            connectorDefinition.githubRepoPath,
                            connectorDefinition.urlJarFile,
                            connectorDefinition.urlElementTemplate
                    );

                } catch (Exception e) {
                    logger.error("StoreFactory Store[{}] connector [{}] failed ", storeAccess.getName(), connectorDefinition.name, e);
                    connectorDefinition.status = StoreAccess.EXPLORATION.INCOMPLETE;
                }

                if (connectorDefinition.status == StoreAccess.EXPLORATION.READY)
                    nbFullyCorrects++;
                else
                    nbIncorrect++;
            }
            entry.getValue().removeAll(connectorsToRemove);

            logger.info("~~~~~ END Pass 2.{} - Deep exploration finish on Store[{}] in {} ms on {} connectors, correct:{} Incorrect:{} connectorsToRemove:{} (referenced in CamundaStore or CamundaHub)",
                    countStore,
                    storeAccess.getName(),
                    System.currentTimeMillis() - startTimeDeep,
                    nbFullyCorrects,
                    nbIncorrect,
                    connectorsToRemove.size());

        }

        // Update the isInstallable
        // Build all connectorType
        logger.info("~~~~~ Pass 3. Update isInstallable");
        int isInstallable = 0;
        int totalConnectors = 0;
        Set<String> allConnectorTypes = new HashSet<>();

        for (Map.Entry<StoreAccess, List<StoreAccess.ConnectorDefinition>> entry : mapConnectors.entrySet()) {
            for (StoreAccess.ConnectorDefinition connectorDefinition : entry.getValue()) {
                if (connectorDefinition.connectorType != null && connectorDefinition.hasImplementation) {
                    allConnectorTypes.add(connectorDefinition.connectorType);
                }
            }
        }
        for (Map.Entry<StoreAccess, List<StoreAccess.ConnectorDefinition>> entry : mapConnectors.entrySet()) {
            for (StoreAccess.ConnectorDefinition connectorDefinition : entry.getValue()) {
                totalConnectors++;
                connectorDefinition.isInstallable = connectorDefinition.hasImplementation || allConnectorTypes.contains(connectorDefinition.connectorType);
                if (connectorDefinition.isInstallable) {
                    isInstallable++;
                }
            }
        }
        logger.info("~~~~~ END Pass 3. isInstallable : connectorImplementation {} connectorInstallable {} on {}", allConnectorTypes.size(), isInstallable, totalConnectors);
        logger.info("---- End exploration of all stores/connectors in {} ms", System.currentTimeMillis() - beginTime);
        explorationInProcess = false;

    }

    /**
     * Download the connector
     *
     * @param storeName     store where the connector must be downloaded
     * @param connectorName name of the connector
     * @param release       release (maybe null)
     * @return a connectorDownload status
     */
    public StoreAccess.ConnectorDownload downloadConnector(String storeName, String connectorName, String release) {
        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();
        StoreAccess storeAccess = getFromName(storeName);
        if (storeAccess == null) {
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWNSTORE;
            connectorDownload.explanation = "Store[" + storeName + "] unknown";
            return connectorDownload;
        }
        StoreAccess.ConnectorDefinition connectorDefinition = getConnectorDefinition(storeAccess, connectorName);
        if (connectorDefinition == null) {
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWCONNECTOR;
            return connectorDownload;
        }

        // check if the asked release is the correct one
        if (release != null && connectorDefinition.release != null && !connectorDefinition.release.equals(release)) {
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "Release asked[" + release + "] Release in store [" + connectorDefinition.release + "]";
            return connectorDownload;

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
