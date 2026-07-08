package io.camunda.cherry.store;

import java.util.List;

public class StoreDownloadInitialisation {

    private final StoreFactory storeFactory;
    public StoreDownloadInitialisation(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public void downloadAndStart(List<String> listConnectorsToDownload) {

    }
}
