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

    /**
     * Kept for backward compatibility — delegates to cherry.store.stores.
     */
    public List<String> getStores() {
        return store.getStores();
    }

    public static class Store {

        private List<String> stores = new ArrayList<>();
        private List<String> download = new ArrayList<>();
        private Startup startup = new Startup();

        public List<String> getStores() {
            return stores;
        }

        public void setStores(List<String> stores) {
            this.stores = stores;
        }

        public List<String> getDownload() {
            return download;
        }

        public void setDownload(List<String> download) {
            this.download = download;
        }

        public Startup getStartup() {
            return startup;
        }

        public void setStartup(Startup startup) {
            this.startup = startup;
        }

        public static class Startup {

            private boolean explore = false;
            private ConnectorRuntime connectorRuntime = new ConnectorRuntime();
            private CommunityConnector communityConnector = new CommunityConnector();

            public boolean isExplore() {
                return explore;
            }

            public void setExplore(boolean explore) {
                this.explore = explore;
            }

            public ConnectorRuntime getConnectorRuntime() {
                return connectorRuntime;
            }

            public void setConnectorRuntime(ConnectorRuntime connectorRuntime) {
                this.connectorRuntime = connectorRuntime;
            }

            public CommunityConnector getCommunityConnector() {
                return communityConnector;
            }

            public void setCommunityConnector(CommunityConnector communityConnector) {
                this.communityConnector = communityConnector;
            }

            public static class ConnectorRuntime {

                private boolean download = false;
                private String tag;

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
            }

            public static class CommunityConnector {

                private boolean download = false;
                private String filter;

                public boolean isDownload() {
                    return download;
                }

                public void setDownload(boolean download) {
                    this.download = download;
                }

                public String getFilter() {
                    return filter;
                }

                public void setFilter(String filter) {
                    this.filter = filter;
                }
            }
        }
    }
}
