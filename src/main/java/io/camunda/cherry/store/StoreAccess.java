package io.camunda.cherry.store;

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
     * @param connectorDefinition
     */
    void exploreDetails(ConnectorDefinition connectorDefinition);


    ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition);



    public enum EXPLORATION { READY, INPROGRESS, FAILED}
    public class ConnectorDefinition {
        StoreAccess storeAccess;
        String name;
        String url;
        /**
         * If the connector is store in GitHub, save the Github repository something like

         */
        String githubRepoName;
        String githubRepoPath;

        String release;
        String icon;
        String description;
        EXPLORATION status;
        String documentationRef;
        /*
        Url to download the element Template
         */
        String urlElementTemplate;
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
         * The connector may not have a direct implementation, and use a other comp
         */
        public boolean hasImplementation = true;
        public static ConnectorDefinition getInstance(StoreAccess storeAccess, String name, String url, String release) {
            ConnectorDefinition connectorDefinition = new ConnectorDefinition();
            connectorDefinition.storeAccess = storeAccess;
            connectorDefinition.name = name;
            connectorDefinition.url = url;
            connectorDefinition.release = release;
            return connectorDefinition;
        }
    }

    public class ConnectorDownload {
        public String elementTemplate;
        ByteArrayInputStream jarContent;
        List<ConnectorDetail> listConnectors;
    }

    public class ConnectorDetail {
        public String className;
        public List<String> fetchVariables;
        public String name;
        public String type;
    }

}
