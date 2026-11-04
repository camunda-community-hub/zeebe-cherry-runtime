package io.camunda.cherry.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "cherry")
public class CherryProperties {

    private Store store = new Store();

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    /**
     * NEVER: do not download automatically at startup
     * ALWAYS: force the download everytime, same release or not
     * UPDATE: download only if a new release is available
     * FIXEDVERSION: download if you don't have it, but after that, do not download any more (no new version)
     */
    public enum DownloadPolicy {NEVER, ALWAYS, UPDATE, FIXEDVERSION}

    /**
     * Credentials for a Maven provider (Maven Central, Camunda's JFrog Artifactory, ...) used by
     * MavenCentralDownload/StoreCamundaConnector to authenticate requests - every store has its own,
     * each optional and left blank for a public/anonymous repository such as Maven Central.
     */
    public static class ProviderCredential {
        private String token = "";
        private String apikey = "";
        private boolean access = true;

        public boolean isAccess() {
            return access;
        }

        public void setAccess(boolean access) {
            this.access = access;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getApikey() {
            return apikey;
        }

        public void setApikey(String apikey) {
            this.apikey = apikey;
        }
    }

    /**
     * Shared startup-download configuration, used by every store (Camunda connector store,
     * community connector store, marketplace, private GitHub stores).
     */

    /**
     * GitHub credential for a store that needs its own token rather than sharing the unauthenticated
     * default GitHubAccess instance (e.g. communityConnector, which lists many repositories).
     */
    public static class GithubCredential {
        private String token = "";

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Startup {
        private DownloadPolicy downloadPolicy = DownloadPolicy.NEVER;
        private String tag;
        private List<String> filter = new ArrayList<>();

        public DownloadPolicy getDownloadPolicy() {
            return downloadPolicy;
        }

        public void setDownloadPolicy(DownloadPolicy downloadPolicy) {
            this.downloadPolicy = downloadPolicy;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public List<String> getFilter() {
            return filter;
        }

        public void setFilter(List<String> filter) {
            this.filter = filter;
        }
    }

    public static class Store {

        private List<DownloadStartupEntry> downloadStartup = new ArrayList<>();
        private PrivateStore privateStore = new PrivateStore();
        private CamundaConnector camundaConnector = new CamundaConnector();
        private CommunityConnector communityConnector = new CommunityConnector();
        private MarketplaceConnector marketplaceConnector = new MarketplaceConnector();

        public List<DownloadStartupEntry> getDownloadStartup() {
            return downloadStartup;
        }

        public void setDownloadStartup(List<DownloadStartupEntry> downloadStartup) {
            this.downloadStartup = downloadStartup;
        }

        public PrivateStore getPrivateStore() {
            return privateStore;
        }

        public void setPrivateStore(PrivateStore privateStore) {
            this.privateStore = privateStore;
        }

        public CamundaConnector getCamundaConnector() {
            return camundaConnector;
        }

        public void setCamundaConnector(CamundaConnector camundaConnector) {
            this.camundaConnector = camundaConnector;
        }

        public CommunityConnector getCommunityConnector() {
            return communityConnector;
        }

        public void setCommunityConnector(CommunityConnector communityConnector) {
            this.communityConnector = communityConnector;
        }

        public MarketplaceConnector getMarketplaceConnector() {
            return marketplaceConnector;
        }

        public void setMarketplaceConnector(MarketplaceConnector marketplaceConnector) {
            this.marketplaceConnector = marketplaceConnector;
        }

        /**
         * One ad-hoc "download this URL at startup" entry - the URL is downloaded using the
         * named store's own credentials (e.g. a private GitHub store's token), found via
         * StoreFactory.getStoreByName().
         */
        public static class DownloadStartupEntry {
            private String store;
            private String url;

            public String getStore() {
                return store;
            }

            public void setStore(String store) {
                this.store = store;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }

        public static class PrivateStore {

            private boolean access = false;
            private List<PrivateStoreEntry> listStores = new ArrayList<>();

            public boolean isAccess() {
                return access;
            }

            public void setAccess(boolean access) {
                this.access = access;
            }

            public List<PrivateStoreEntry> getListStores() {
                return listStores;
            }

            public void setListStores(List<PrivateStoreEntry> listStore) {
                this.listStores = listStore;
            }

            public static class PrivateStoreEntry {
                private String name;
                private String url;
                private String token = "";
                private String apikey = "";
                private Startup startup = new Startup();
                /**
                 * Optional overrides of MavenProvider's default header name/prefix for this store's
                 * "maven" provider - left blank to keep MavenProvider's own defaults.
                 */
                private String tokenHeaderName = "";
                private String tokenPrefix = "";
                private String apikeyHeaderName = "";


                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                /**
                 * Single token for this private store, used both for its GitHub access (listing
                 * repositories, releases, element-templates) and as this store's Maven provider
                 * credential (searching/downloading a JAR) - one value instead of two separate ones.
                 */
                public String getToken() {
                    return token;
                }

                public void setToken(String token) {
                    this.token = token;
                }

                public String getApikey() {
                    return apikey;
                }

                public void setApikey(String apikey) {
                    this.apikey = apikey;
                }

                public Startup getStartup() {
                    return startup;
                }

                public void setStartup(Startup startup) {
                    this.startup = startup;
                }

                public String getTokenHeaderName() {
                    return tokenHeaderName;
                }

                public void setTokenHeaderName(String tokenHeaderName) {
                    this.tokenHeaderName = tokenHeaderName;
                }

                public String getTokenPrefix() {
                    return tokenPrefix;
                }

                public void setTokenPrefix(String tokenPrefix) {
                    this.tokenPrefix = tokenPrefix;
                }

                public String getApikeyHeaderName() {
                    return apikeyHeaderName;
                }

                public void setApikeyHeaderName(String apikeyHeaderName) {
                    this.apikeyHeaderName = apikeyHeaderName;
                }
            }
        }

        public static class CamundaConnector {

            private boolean access = false;
            private Startup startup = new Startup();
            private ProviderCredential maven = new ProviderCredential();
            private GithubCredential github = new GithubCredential();

            public boolean isAccess() {
                return access;
            }

            public void setAccess(boolean access) {
                this.access = access;
            }

            public Startup getStartup() {
                return startup;
            }

            public void setStartup(Startup startup) {
                this.startup = startup;
            }

            public ProviderCredential getMaven() {
                return maven;
            }

            public void setMaven(ProviderCredential maven) {
                this.maven = maven;
            }

            public GithubCredential getGithub() {
                return github;
            }

            public void setGithub(GithubCredential github) {
                this.github = github;
            }
        }

        public static class CommunityConnector {

            private boolean access = false;
            private String filterProject = "connector-8-*";
            private Startup startup = new Startup();
            private ProviderCredential maven = new ProviderCredential();
            private GithubCredential github = new GithubCredential();
            private ProviderCredential jfrog = new ProviderCredential();

            public boolean isAccess() {
                return access;
            }

            public void setAccess(boolean access) {
                this.access = access;
            }

            public String getFilterProject() {
                return filterProject;
            }

            public void setFilterProject(String filterProject) {
                this.filterProject = filterProject;
            }

            public Startup getStartup() {
                return startup;
            }

            public void setStartup(Startup startup) {
                this.startup = startup;
            }

            public ProviderCredential getMaven() {
                return maven;
            }

            public void setMaven(ProviderCredential maven) {
                this.maven = maven;
            }

            public GithubCredential getGithub() {
                return github;
            }

            public void setGithub(GithubCredential github) {
                this.github = github;
            }

            public ProviderCredential getJfrog() {
                return jfrog;
            }

            public void setJfrog(ProviderCredential jfrog) {
                this.jfrog = jfrog;
            }
        }

        public static class MarketplaceConnector {

            private boolean access = true;
            private Startup startup = new Startup();
            private ProviderCredential maven = new ProviderCredential();
            private GithubCredential github = new GithubCredential();

            public boolean isAccess() {
                return access;
            }

            public void setAccess(boolean access) {
                this.access = access;
            }

            public Startup getStartup() {
                return startup;
            }

            public void setStartup(Startup startup) {
                this.startup = startup;
            }

            public ProviderCredential getMaven() {
                return maven;
            }

            public void setMaven(ProviderCredential maven) {
                this.maven = maven;
            }

            public GithubCredential getGithub() {
                return github;
            }

            public void setGithub(GithubCredential github) {
                this.github = github;
            }
        }


    }
}
