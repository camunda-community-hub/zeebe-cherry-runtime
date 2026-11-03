/* ******************************************************************** */
/*                                                                      */
/*  StoreCamundaCommunity                                               */
/*                                                                      */
/* Access the Store Service - download new connectors                   */
/* Camunda community can store the JAR (and in the priority):           */
/* - in the maven central                                               */
/*      https://mvnrepository.com/artifact/org.camunda.connectors       */
/* - in the jfrog (camunda repository)                                  */
/*     https://artifacts.camunda.com/ui/packages?name=org.camunda.connector&type=packages*/
/* - as a Release                                                       */
/*                                                                      */
/* the point is the two first repository contains only the minimum jar, */
/* without any dependencies, so the mechanism should explore the pom.xml*/
/* and download all the dependency as separate jar                      */
/*                                                                      */
/* ******************************************************************** */

package io.camunda.cherry.store;

import io.camunda.cherry.runtime.CherryProperties;
import io.camunda.cherry.store.git.GitHubAccess;
import io.camunda.cherry.store.mavencentral.MavenCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class StoreCamundaCommunity extends StoreGithub {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreCamundaCommunity.class);
    private static final String CAMUNDA_ARTIFACTORY_REPOSITORY = "https://artifacts.camunda.com/artifactory/connectors";

    // - https://github.com/camunda-community-hub/
    public StoreCamundaCommunity(GitHubAccess gitHubAccess, CherryProperties.Startup startup) {
        super("Camunda Community Hub", "https://github.com/orgs/camunda-community-hub", "camunda-8-connector", gitHubAccess, startup);
    }

    public String getType() {
        return "CamundaCommunityHub";
    }

    @Override
    protected String getTypeRepo() {
        return "owner";
    }

    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {

        // The JAR maybe in the release, or in maven central, or in jfrog
        if (connectorDefinition.urlJarFile != null && !connectorDefinition.urlJarFile.isEmpty()) {
            LOGGER.info("CamundaCommunity: Downloading connector {} from {}", connectorDefinition.name, connectorDefinition.urlJarFile);
            return super.downloadConnector(connectorDefinition);
        } else {
            ConnectorDownload connectorDownload = new ConnectorDownload();
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "Store [CamundaCommunity] No JAR file URL available for connector [" + connectorDefinition.name + "]";
            return connectorDownload;
        }

    }

    public List<ConnectorDefinition> exploreListConnectors() {
        return super.exploreListConnectors();
    }


    /**
     * Return the list of provider where the JAR can be stored
     * @return list of provider with its credential
     */
    public List<MavenCredential> getMavenProviders() {
        List<MavenCredential> listMaven = new ArrayList(super.getMavenProviders());
        listMaven.add( MavenCredential.getInstance("jfrog", CAMUNDA_ARTIFACTORY_REPOSITORY));
        return listMaven;
    }
}
