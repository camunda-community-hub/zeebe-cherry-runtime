package io.camunda.cherry.store;

import io.camunda.cherry.runtime.CherryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class StoreUrl extends StoreAccess {

    private final StoreFactory storeFactory;
    private final RestTemplate restTemplate;
    private final GitHubAccess gitHubAccess;
    Logger logger = LoggerFactory.getLogger(StoreUrl.class.getName());

    public StoreUrl(StoreFactory storeFactory, GitHubAccess gitHubAccess) {
        // No per-store startup config exists for this store: it only serves ad-hoc direct-URL
        // downloads triggered from cherry.store.downloadStartup, not a filtered/scheduled download.
        super(new CherryProperties.Startup());
        this.storeFactory = storeFactory;
        this.gitHubAccess = gitHubAccess;
        restTemplate = new RestTemplate();
        if (gitHubAccess.isToken()) {
            ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubAccess.getGithubToken());
                return execution.execute(request, body);
            };
            restTemplate.setInterceptors(List.of(authInterceptor));
        }
    }


    @Override
    public String getName() {
        return "DownloadAtStartup";
    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public List<ConnectorDefinition> exploreListConnectors() {
        return List.of();
    }

    @Override
    public boolean exploreDetails(ConnectorDefinition connectorDefinition) {
        return false;
    }

    @Override
    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {
        return null;
    }

    public ConnectorDownload downloadConnectorFromUrl(String urlJarFile) {
        logger.info("Store[{}] Start downloading from url[{}]...", getName(), urlJarFile);
        ConnectorDownload connectorDownload = new ConnectorDownload();
        if (urlJarFile == null || urlJarFile.isEmpty()) {
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "No JAR file URL available";
            return connectorDownload;
        }
        try {
            long startTime = System.currentTimeMillis();
            connectorDownload.jarName = urlJarFile.substring(urlJarFile.lastIndexOf('/') + 1);
            byte[] jarBytes = restTemplate.getForObject(urlJarFile, byte[].class);
            if (jarBytes == null) {
                connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
                connectorDownload.explanation = "Empty response from [" + urlJarFile + "]";
                return connectorDownload;
            }
            connectorDownload.jarContent = new java.io.ByteArrayInputStream(jarBytes);
            connectorDownload.status = STATUSDOWNLOAD.OK;
            connectorDownload.explanation = "Downloaded " + jarBytes.length + " bytes from [" + urlJarFile + "]";
            logger.info("Store[{}] Download connector from url[{}] length {} in ms", getName(), urlJarFile,
                    jarBytes.length,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Store[{}] connector error downloading JAR from [{}]: {}",
                    getName(), urlJarFile, e.getMessage());
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "Error downloading JAR: " + e.getMessage();
        }
        return connectorDownload;
    }
}
