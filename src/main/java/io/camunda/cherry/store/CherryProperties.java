package io.camunda.cherry.store;

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


    public static class Store {

        private List<String> downloadStartup = new ArrayList<>();
        private PrivateStore privateStore = new PrivateStore();
        private CamundaConnector camundaConnector = new CamundaConnector();
        private CommunityConnector communityConnector = new CommunityConnector();
        private MarketplaceConnector marketplaceConnector = new MarketplaceConnector();

        public List<String> getDownloadStartup() {
            return downloadStartup;
        }

        public void setDownloadStartup(List<String> downloadStartup) {
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


        public static class PrivateStore {

            private boolean access = false;
            private List<String> listStore = new ArrayList<>();

            public boolean isAccess() {
                return access;
            }

            public void setAccess(boolean access) {
                this.access = access;
            }

            public List<String> getListStore() {
                return listStore;
            }

            public void setListStore(List<String> listStore) {
                this.listStore = listStore;
            }
        }

        public static class CamundaConnector {

            private boolean access = false;
            private Startup startup = new Startup();

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

            public static class Startup {
                private boolean download = false;
                private String tag;
                private List<String> filter = new ArrayList<>();

                public boolean isDownload() {
                    return download;
                }

                public void setDownload(boolean download) {
                    this.download = download;
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
        }

        public static class CommunityConnector {

            private boolean access = false;
            private String filterProject = "connector-8-*";
            private Startup startup = new Startup();

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

            public static class Startup {
                private boolean download = false;
                private List<String> filter = new ArrayList<>();

                public boolean isDownload() {
                    return download;
                }

                public void setDownload(boolean download) {
                    this.download = download;
                }

                public List<String> getFilter() {
                    return filter;
                }

                public void setFilter(List<String> filter) {
                    this.filter = filter;
                }
            }
        }

        public static class MarketplaceConnector {

            private boolean access = true;
            private Startup startup = new Startup();

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

            public static class Startup {
                private boolean download = false;
                private List<String> filter = new ArrayList<>();

                public boolean isDownload() {
                    return download;
                }

                public void setDownload(boolean download) {
                    this.download = download;
                }

                public List<String> getFilter() {
                    return filter;
                }

                public void setFilter(List<String> filter) {
                    this.filter = filter;
                }
            }
        }


    }
}
