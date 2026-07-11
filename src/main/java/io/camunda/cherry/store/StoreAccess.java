package io.camunda.cherry.store;

import io.camunda.cherry.runner.RunnerLightDefinition;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface StoreAccess {

    String getName();

    String getUrl();

    String getType();

    /**
     * Get the lisf of connector. Each connectorDefition is "light", with name.
     * Then the exploration will continue one by one via the exploreDetails
     * status is set to INPROGRESS
     *
     * @return
     */
    List<ConnectorDefinition> getListConnectors();

    /**
     * Explore details. At the end, the connectorDefition is complete
     * - name
     * - release
     * - url to get the element template
     * - url to get the JAR file
     * - logo ir downloaded
     * status is moved to READY or FAILED
     *
     * @param connectorDefinition
     * @return true if the connector is valid, false if not and must be deleted
     */
    boolean exploreDetails(ConnectorDefinition connectorDefinition);


    ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition);


    enum EXPLORATION {READY, INPROGRESS, INCOMPLETE}

    enum STATUSDOWNLOAD {UNKNOWCONNECTOR, UNKNOWNSTORE, UNKNOWNRELEASE, OK}

    class ConnectorDefinition {
        /**
         * If the connector is store in GitHub, save the Github repository something like
         */
        public String githubRepoName;
        public String githubRepoPath;
        public int version;
        public String release;
        public String icon;
        /**
         * The connector may not have a direct implementation, and use a other comp
         */
        public boolean hasImplementation = true;
        public boolean isInstallable = true;
        StoreAccess storeAccess;
        String name;
        String url;
        String description;
        String creator;
        EXPLORATION status;
        String documentationRef;
        /*
        Url to download the element Template
         */
        List<String> urlElementTemplate;
        /**
         * Url to download the Jarfile - attentioon, this file maybe a Zip, and may be need to be explored
         */
        String urlJarFile;
        /**
         * The jar file may be stored in a Maven repository
         */
        String urlMaven;
        /**
         * the connector type
         */
        String connectorType;
        /**
         * Source URL (marketplace page, GitHub page, etc.)
         */
        public String sourceUrl;

        public static ConnectorDefinition getInstance(StoreAccess storeAccess, String name, String url, String release) {
            ConnectorDefinition connectorDefinition = new ConnectorDefinition();
            connectorDefinition.storeAccess = storeAccess;
            connectorDefinition.name = name;
            connectorDefinition.url = url;
            connectorDefinition.release = release;
            return connectorDefinition;
        }
    }

    class ConnectorDownload {
        public STATUSDOWNLOAD status;
        public String explanation;
        public String elementTemplate;
        public ByteArrayInputStream jarContent;
        public String jarName;
        public List<RunnerLightDefinition> runners;
        public List<ConnectorDetail> connectorDetails;
    }

    class ConnectorDetail {
        public String className;
        public List<String> fetchVariables;
        public String name;
        public String type;
    }


}
