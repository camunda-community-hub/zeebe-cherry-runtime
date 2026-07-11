package io.camunda.cherry.store;

public class StoreCamundaCommunity extends StorePrivateGithub {

    // - https://github.com/camunda-community-hub/
    public StoreCamundaCommunity(GitHubAccess gitHubAccess) {
        super("Camunda Community Hub", "https://github.com/orgs/camunda-community-hub", "camunda-8-connector", gitHubAccess);
    }

    public String getType() {
        return "CamundaCommunityHub";
    }

    private String getTypeRepo() {
        return "owner";
    }

}
