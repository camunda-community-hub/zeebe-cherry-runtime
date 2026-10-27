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

public class StoreCamundaCommunity extends StoreGithub {

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

}
