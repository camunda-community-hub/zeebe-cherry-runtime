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
