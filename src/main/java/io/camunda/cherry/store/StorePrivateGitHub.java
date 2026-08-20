package io.camunda.cherry.store;

import io.camunda.cherry.runtime.CherryProperties;

public class StorePrivateGitHub extends StoreGithub {

    public StorePrivateGitHub(String name, String url, GitHubAccess gitHubAccess, CherryProperties.Startup startup) {
        super(name, url, null, gitHubAccess, startup);
    }

    /**
     * @param name              store name
     * @param url               GitHub profile/org URL
     * @param filterProjectName optional filter applied to repository names when listing (e.g. "camunda-8-connector")
     * @param gitHubAccess      GitHub access
     * @param startup           startup-download configuration for this store
     */
    public StorePrivateGitHub(String name, String url, String filterProjectName, GitHubAccess gitHubAccess, CherryProperties.Startup startup) {
        super(name, url, filterProjectName, gitHubAccess, startup);
    }

    public String getType() {
        return "GitHub";
    }

    @Override
    protected String getTypeRepo() {
        return "owner";
    }

}
