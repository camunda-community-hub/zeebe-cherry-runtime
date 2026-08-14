package io.camunda.cherry.store;

import io.camunda.cherry.exception.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreFactory {

    private final CherryProperties cherryProperties;
    private final GitHubAccess gitHubAccess;
    private final Map<StoreAccess, List<StoreAccess.ConnectorDefinition>> mapConnectors = new HashMap<>();
    private final int BASE_ADVANCE_PHASE1 = 40;
    private final int BASE_ADVANCE_PHASE2 = 40;
    private final int BASE_ADVANCE_PHASE3 = 20;
    public List<StoreAccess> listStoreAccess = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(StoreFactory.class.getName());
    private EXPLORATIONCONNECTOR exploration = EXPLORATIONCONNECTOR.NONE;
    private int percentageExploration = 0;
    StoreFactory(GitHubAccess gitHubAccess, CherryProperties cherryProperties) {

        if (cherryProperties.getStore().getCamundaConnector().isAccess())
            listStoreAccess.add(new StoreCamundaConnector(gitHubAccess));

        if (cherryProperties.getStore().getCommunityConnector().isAccess())
            listStoreAccess.add(new StoreCamundaCommunity(gitHubAccess));

        if (cherryProperties.getStore().getMarketplaceConnector().isAccess())
            listStoreAccess.add(new StoreMarketPlace(gitHubAccess));

        if (cherryProperties.getStore().getPrivateStore().isAccess()) {
            for (String repoUrl : cherryProperties.getStore().getPrivateStore().getListStore()) {
                String name = extractName(repoUrl);
                listStoreAccess.add(new StorePrivateGithub(name, repoUrl, gitHubAccess));
            }
        }
        this.cherryProperties = cherryProperties;
        this.gitHubAccess = gitHubAccess;
    }

    public StoreCamundaConnector getStoreCamundaConnector() {
        for (StoreAccess storeAccess : listStoreAccess) {
            if (storeAccess instanceof StoreCamundaConnector storeAccessConnector) {
                return storeAccessConnector;
            }
        }
        return null;
    }

    public StoreCamundaCommunity getStoreCommunity() {
        for (StoreAccess storeAccess : listStoreAccess) {
            if (storeAccess instanceof StoreCamundaCommunity storeAccessCommunity) {
                return storeAccessCommunity;
            }
        }
        return null;
    }

    public List<String> getStoreNames() {
        return listStoreAccess.stream().map(StoreAccess::getName).toList();
    }

    public List<StoreAccess> getStores() {
        return listStoreAccess;
    }

    public EXPLORATIONCONNECTOR getExploration() {
        return exploration;
    }

    public int getPercentageExploration() {
        return percentageExploration;
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
     * @return the connector definition
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

        // Merge: first occurrence of a connectorType wins, based on name : multiple connectors may have different name but same type (like restCall)
        Map<String, StoreAccess.ConnectorDefinition> merged = new java.util.LinkedHashMap<>();
        for (StoreAccess storeAccess : listStoreOrdered) {
            List<StoreAccess.ConnectorDefinition> connectors = mapConnectors.get(storeAccess);
            if (connectors == null)
                continue;
            for (StoreAccess.ConnectorDefinition connector : connectors) {
                // The connector is identified to be on marketplace and have a origin from Hub or Camunda Connector: ignore it.
                if (connector.connectorSource != StoreAccess.CONNECTORSOURCE.NONE)
                    continue;

                String key = connector.name;
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
        exploration = EXPLORATIONCONNECTOR.INPROGRESS;
        percentageExploration = 0;
        long beginTime = System.currentTimeMillis();
        mapConnectors.clear();
        int nbConnectors = 0;
        int countStore = 0;
        for (StoreAccess storeAccess : listStoreAccess) {
            percentageExploration = (int) ((((double) countStore) / listStoreAccess.size()) * BASE_ADVANCE_PHASE1);
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
        percentageExploration = BASE_ADVANCE_PHASE1;

        // Ok, now replay all connectors and explore them
        countStore = 0;
        int totalStores = mapConnectors.size();
        for (Map.Entry<StoreAccess, List<StoreAccess.ConnectorDefinition>> entry : mapConnectors.entrySet()) {
            countStore++;
            StoreAccess storeAccess = entry.getKey();
            logger.info("~~~~~ Pass 2.({}/{}) - Deep exploration store[{}]", countStore, listStoreAccess.size(), storeAccess.getName());
            long startTimeDeep = System.currentTimeMillis();
            int nbFullyCorrects = 0;
            int nbIncorrect = 0;
            int countConnectors = 0;
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
                        logger.info("StoreFactory: connector[{}] Store [{}] is not valid", connectorDefinition.name, connectorDefinition.storeAccess.getName());
                        connectorsToRemove.add(connectorDefinition);
                        continue;
                    }
                    connectorDefinition.status = !connectorDefinition.listEltTemplate.isEmpty() ?
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
                            connectorDefinition.listAnnotations.stream().map(c -> c.name).collect(Collectors.joining(","))
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
        exploration = EXPLORATIONCONNECTOR.COMPLETED;

    }

    public StoreAccess.ConnectorDefinition getConnectorDefinition(String storeName, String connectorName, String release) throws TechnicalException {
        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();
        StoreAccess storeAccess = getFromName(storeName);
        if (storeAccess == null) {
            throw new TechnicalException("Store[" + storeName + "] not found");
        }
        // Soon: get the connectorDefinition for the asking release
        StoreAccess.ConnectorDefinition connectorDefinition = getConnectorDefinition(storeAccess, connectorName);
        return connectorDefinition;
    }

    /**
     * The connectorDefintion may not have an implementation, and rely on one another connector.
     * For example, a lot of connector rely on the connector HTTP
     *
     * @param connectorDefinition definition
     * @return any parent definition, if the connector has a parent
     * @throws TechnicalException any error
     */
    public StoreAccess.ConnectorDefinition getParentConnectorDefinition(StoreAccess.ConnectorDefinition connectorDefinition) throws TechnicalException {
        if (connectorDefinition.hasImplementation)
            return connectorDefinition;

        // Search if a connector with an implementation exist
        StoreAccess.ConnectorDefinition parentConnectorDefinition = searchConnector(new Filter().type(connectorDefinition.connectorType).hasImplementation(Boolean.TRUE));
        logger.info("StoreFactory: connector [{} type[{}] has a parent connector:[{}]", connectorDefinition.name, connectorDefinition.connectorType,
                parentConnectorDefinition == null ? "No parent" : parentConnectorDefinition.name);
        return parentConnectorDefinition;
    }

    public StoreAccess.ConnectorDownload downloadConnector(StoreAccess.ConnectorDefinition connectorDefinition) {
        return connectorDefinition.storeAccess.downloadConnector(connectorDefinition);

    }

    public StoreAccess getFromName(String storeName) {
        return listStoreAccess.stream().filter(s -> s.getName().equals(storeName)).findFirst().orElse(null);
    }

    /**
     * Search by the filter.
     * Note: filter works in a mix of AND/OR.
     * - hasImplementation : if set, this is mandatory.
     * - name / type : any of this criteria when they are trye, it matches the connector
     * @param filter
     * @return
     */
    public StoreAccess.ConnectorDefinition searchConnector(Filter filter) {
        for (List<StoreAccess.ConnectorDefinition> listConnectorDefinitions : mapConnectors.values()) {
            for (StoreAccess.ConnectorDefinition connectorDefinition : listConnectorDefinitions) {
                // do not keep a definition with no implementation if we filter on it
                if (filter.hasImplementation != null && !connectorDefinition.hasImplementation)
                    continue;
                if (connectorDefinition.name.equals(filter.name)) {
                    logger.info("searchConnector: connectorDefinition Filter[{}} found {}", filter.name, connectorDefinition);
                    return connectorDefinition;
                }
                if (connectorDefinition.connectorType.equals(filter.type)) {
                    logger.info("searchConnector: connectorDefinitionType[{}} found {}", filter.type, connectorDefinition.toString());
                    return connectorDefinition;
                }
            }
        }
        logger.info("searchConnector: connectorDefinition FilterName[{}] FilterType[{}] HasImplementation [{}]not found",
                filter.name,
                filter.type,
                filter.hasImplementation);
        return null;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  searchConnectorDefinition                                           */
    /*                                                                      */
    /* ******************************************************************** */

    private String extractName(String url) {
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }

    public enum EXPLORATIONCONNECTOR {NONE, INPROGRESS, COMPLETED}


    /* ******************************************************************** */
    /*                                                                      */
    /*  private                                                             */
    /*                                                                      */
    /* ******************************************************************** */

    public static class Filter {
        public String name;
        public String type;
        public Boolean hasImplementation;

        public Filter name(String name) {
            this.name = name;
            return this;
        }

        public Filter type(String type) {
            this.type = type;
            return this;
        }

        public Filter hasImplementation(Boolean hasImplementation) {
            this.hasImplementation = hasImplementation;
            return this;
        }

    }
/** TO REMOVE
 private boolean matchesFilter(String connectorName) {
 List<String> filter = cherryProperties.getStore().getStartup().getCommunityConnector().getFilter();
 if (filter == null || filter.isEmpty()) {
 return true;
 }
 for (String oneFilter : filter) {
 String regex = oneFilter.replace(".", "\\.").replace("*", ".*");
 if (connectorName.matches(regex)) {
 return true;
 }
 }
 return false;
 }
 */
}
