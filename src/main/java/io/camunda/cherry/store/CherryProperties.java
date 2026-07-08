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
    }
}
