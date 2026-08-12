package io.camunda.cherry.store;

import io.camunda.cherry.runner.RunnerLightDefinition;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
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
     * @return list of connectors discovered
     */
    List<ConnectorDefinition> exploreListConnectors();

    /**
     * Explore details. At the end, the connectorDefinition is complete
     * - name
     * - release
     * - url to get the element template
     * - url to get the JAR file
     * - logo ir downloaded
     * status is moved to READY or FAILED
     *
     * @param connectorDefinition connector definition to explore
     * @return true if the connector is valid, false if not and must be deleted
     */
    boolean exploreDetails(ConnectorDefinition connectorDefinition);


    ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition);


    enum EXPLORATION {READY, INPROGRESS, INCOMPLETE}

    enum STATUSDOWNLOAD {UNKNOWCONNECTOR, UNKNOWNSTORE, UNKNOWNRELEASE, OK, FAILED, NOURLJARFILE, NOIMPLEMENTATION}

    enum CONNECTORSOURCE {NONE, CAMUNDAHUB, CAMUNDACONNECTOR}

    class ElementTemplateDescription {
        public String name;
        public String description;
        public String url;
        public String version;
        public String connectorType;

        public ElementTemplateDescription(String url) {
            this.url = url;
        }
    }

    class AnnotationDescription {
        public String name;
        public String type;

        public AnnotationDescription(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    class ConnectorDefinition {
        /**
         * Name of the connector. can be the folder name
         * There are multiple name in a connector:
         * - "name" the folder name, first identification. It saved under "name"
         * - "codename": there is name in the code. In a JAR, multiple class/methods with different name, types. In the annotation. This is the real name of the code, @OutboundConnect( name="xxxx"). The codeName is find only when the code is read
         * - "listAnnotations" in the elementTemplate, a different name may be declared. Worst: it's possible to have multiple element-template...
         * <p>
         * To add on the complexity: in a JAR, it may have multiple connectors... with multiple names...
         */
        public String name;

        /**
         * In case the JAR contains multiple
         */
        public List<AnnotationDescription> listAnnotations = new ArrayList<>();

        /**
         * If the connector is store in GitHub, save the Github repository something like
         * the repoName is something like "pierre-yves.monnet/myconnector"
         */
        public String githubRepoName;
        /**
         * The repopath is the path INSIDE the repoName. A repo name may contains multiple connector, like we have in the connectorStore
         */
        public String githubRepoPath = "";
        public int version;
        public String release;
        public String icon;
        /**
         * The connector may not have a direct implementation, and use a other comp
         */
        public boolean hasImplementation = true;
        public boolean isInstallable = true;
        public StoreAccess storeAccess;

        /**
         * name come the URL templates. Multiple element templates can be detected, each with a different name
         */
        public List<ElementTemplateDescription> listEltTemplate = new ArrayList<>();
        public String url;
        public String description;
        public String creator;
        public EXPLORATION status;
        public String documentationRef;
        /**
         * Url to download the Jarfile - attentioon, this file maybe a Zip, and may be need to be explored
         */
        public String urlJarFile;
        /**
         * The jar file may be stored in a Maven repository
         */
        public String urlMaven;
        /**
         * the connector type
         */
        public String connectorType;

        /**
         * Set only if the connector come from marketplace, and reference a Hub or Connector source
         */
        public CONNECTORSOURCE connectorSource = CONNECTORSOURCE.NONE;

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
        public ByteArrayInputStream jarContent;
        public String jarName;
        public List<RunnerLightDefinition> runners;
        public List<ConnectorDetail> connectorDetails;
        String release;
    }

    class ConnectorDetail {
        public String className;
        public List<String> fetchVariables;
        public String name;
        public String type;
    }


}
