package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreMarketPlace implements StoreAccess {

    private static final String MARKETPLACE_BASE_URL = "https://marketplace.camunda.com";
    private static final String LISTING_API = MARKETPLACE_BASE_URL + "/api/internal/storefront/v1/listingPage?page=";
    private static final String APP_BASE_URL = MARKETPLACE_BASE_URL + "/en-US/apps";

    Logger logger = LoggerFactory.getLogger(StoreMarketPlace.class.getName());

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() {
        return "Marketplace";
    }

    @Override
    public String getUrl() {
        return MARKETPLACE_BASE_URL;
    }

    @Override
    public String getType() {
        return "marketplace";
    }

    @Override
    public List<ConnectorDefinition> getListConnectors() {
        List<ConnectorDefinition> result = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = LISTING_API + page;
            logger.info("Explore MarketPlace page {} url[{}]", page, url);
            try {
                String json = restTemplate.getForObject(url, String.class);
                JsonNode root = JsonUtils.toJsonNode(json);
                JsonNode items = root.path("listingProducts").path("items");
                if (!items.isArray() || items.isEmpty()) {
                    break;
                }
                for (JsonNode item : items) {
                    String name = item.path("name").asText("");
                    String itemUrl = APP_BASE_URL + item.path("url").asText("");
                    String logo = item.path("iconUrl").asText("");
                    String description = item.path("overview").asText("");
                    result.add( ConnectorDefinition.getInstance(this, name, itemUrl, ""));
                }
                page++;
            } catch (RestClientException | IOException e) {
                logger.error("StoreMarketPlace.getListConnectors error on page {}: {}", page, e.getMessage());
                break;
            }
        }
        logger.info("End Explore MarketPlace found {} ", page, result.size());
        return result;
    }

    @Override
    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {
        return null;
    }

    @Override
    public void exploreDetails(ConnectorDefinition connectorDefinition) {

    }


}
