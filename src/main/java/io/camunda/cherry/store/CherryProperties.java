package io.camunda.cherry.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "cherry")
public class CherryProperties {

    private List<String> stores = new ArrayList<>();

    public List<String> getStores() {
        return stores;
    }

    public void setStores(List<String> stores) {
        this.stores = stores;
    }
}
