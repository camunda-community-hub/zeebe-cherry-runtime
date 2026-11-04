package io.camunda.cherry.store;

import io.camunda.cherry.runtime.CherryProperties;
import io.camunda.cherry.runtime.LogOperation;
import io.camunda.cherry.store.git.GitHubAccess;

public class StorePrivateGitHub extends StoreGithub {





    /**
     * @param name              store name
     * @param url               GitHub profile/org URL
     * @param filterProjectName optional filter applied to repository names when listing (e.g. "camunda-8-connector")
     * @param gitHubAccess      GitHub access
     * @param startup           startup-download configuration for this store
     * @param mavenToken        this store's own credential for its "maven" provider (one per private store)
     * @param mavenApikey       this store's own credential for its "maven" provider (one per private store)
     * @param tokenHeaderName   optional override of MavenProvider's default token header name, blank to keep the default
     * @param tokenPrefix       optional override of MavenProvider's default token prefix, blank to keep the default
     * @param apikeyHeaderName  optional override of MavenProvider's default apikey header name, blank to keep the default
     */
    public StorePrivateGitHub(String name, String url, String filterProjectName, GitHubAccess gitHubAccess, CherryProperties.Startup startup,
                              String mavenToken, String mavenApikey, String tokenHeaderName, String tokenPrefix, String apikeyHeaderName, LogOperation logOperation) {
        super(name, url, filterProjectName, gitHubAccess, startup, mavenToken, mavenApikey, tokenHeaderName, tokenPrefix, apikeyHeaderName, logOperation);
    }

    public String getType() {
        return "GitHub";
    }

    @Override
    protected String getTypeRepo() {
        return "owner";
    }

}
