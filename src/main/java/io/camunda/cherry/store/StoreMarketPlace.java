package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreMarketPlace implements StoreAccess {

    private static final String MARKETPLACE_BASE_URL = "https://marketplace.camunda.com";
    private static final String LISTING_API = MARKETPLACE_BASE_URL + "/api/internal/storefront/v1/listingPage?page=";
    private static final String APP_BASE_URL = MARKETPLACE_BASE_URL + "/en-US/apps";

    // Github can be found in these buttons:
    // <a href="..." id="1-github-repo-compile-yourself" ...>Host Yourself</a>
    // <a href="..." id="2-template-only-for-sm" ...>For SM</a>

    private static final Pattern GITHUB_REPO_PATTERN = Pattern.compile(
            "<a\\s[^>]*href=\"(https://github\\.com/[^\"]+)\"[^>]*id=\"[^\"]*(?:github-repo|template-only-for-sm)[^\"]*\"");
    private final RestTemplate restTemplate = new RestTemplate();
    private final GitHubAccess gitHubAccess;
    Logger logger = LoggerFactory.getLogger(StoreMarketPlace.class.getName());

    public StoreMarketPlace(GitHubAccess gitHubAccess) {
        this.gitHubAccess = gitHubAccess;
    }

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
                    String itemUrl = MARKETPLACE_BASE_URL + item.path("url").asText("");
                    ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, name, itemUrl, "");
                    connectorDefinition.icon = fetchIconAsDataUri(item.path("iconUrl").asText(""));
                    connectorDefinition.description = item.path("overview").asText("");
                    connectorDefinition.creator = item.path("vendorName").asText("");
                    connectorDefinition.sourceUrl= itemUrl;
                    logger.info("Store[{}] Detect connector[{}] in url[{}]", getName(), name, itemUrl);
                    result.add(connectorDefinition);
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
    public boolean exploreDetails(ConnectorDefinition connectorDefinition) {
        logger.debug("Store[{}] Deep explore market place connector [{}] creator [{}]", getName(), connectorDefinition.name, connectorDefinition.creator);
        try {
            connectorDefinition.hasImplementation = false;
            if (connectorDefinition.url != null && !connectorDefinition.url.isEmpty()) {
                String repoUrl = searchGithubRepository(connectorDefinition.url);
                if (repoUrl != null) {
                    if (repoUrl.contains("https://github.com/orgs/camunda-community-hub")) {
                        logger.info("Store[{}] Connector[{}] : it's a CommunityHub connector: [{}], remove it", getName(), connectorDefinition.name, repoUrl);
                        return false;
                    }

                    if (repoUrl.contains("https://github.com/camunda/connectors")) {
                        logger.info("Store[{}] Connector[{}] : it's a Camunda connector[{}], remove it", getName(), connectorDefinition.name, repoUrl);
                        return false;
                    }

                    // extract "owner/repo" from the full GitHub URL
                    connectorDefinition.githubRepoName = repoUrl;
                    // connectorDefinition.githubRepoName = repoUrl.replaceFirst("https://github\\.com/", "");
                    // Search jar and element-template in the repository
                    connectorDefinition = gitHubAccess.fillJarDownload(getName(), connectorDefinition);
                    connectorDefinition = gitHubAccess.fillElementTemplate(getName(), connectorDefinition);
                    logger.debug("Store[{}] Connector[{}] Repo GithubRepository found [{}] creator[{}]", getName(), connectorDefinition.name, repoUrl, connectorDefinition.creator);

                }
            } else {
                logger.debug("Store[{}] No Github repository found [{}]", getName(), connectorDefinition.name);
            }
            return true;
        }catch(Exception e) {
            logger.error("Store[{}] connector[{}] url[{}] exception ",getName(), connectorDefinition.name,connectorDefinition.url, e);
            return true;
        }
    }

    /**
     * Fetches the marketplace page at the given URL, parses the HTML, and looks for an anchor
     * whose id contains "github-repo" (e.g. id="1-github-repo-compile-yourself").
     * Returns the href value (the GitHub repository URL) or null if not found.
     */
    public String searchGithubRepository(String url) {
        try {
            String html = restTemplate.getForObject(url, String.class);
            if (html == null) return null;
            Matcher matcher = GITHUB_REPO_PATTERN.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (RestClientException e) {
            logger.warn("StoreMarketPlace.searchGithubRepository: error fetching [{}]: {}", url, e.getMessage());
        }
        return null;
    }

    @Override
    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {
        return null;
    }

    private String fetchIconAsDataUri(String iconUrl) {
        if (iconUrl == null || iconUrl.isEmpty()) return "";
        try (InputStream in = new URL(iconUrl).openStream()) {
            byte[] bytes = in.readAllBytes();
            String mimeType = iconUrl.endsWith(".svg") ? "image/svg+xml" : "image/png";
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            logger.warn("StoreMarketPlace: failed to fetch icon [{}]: {}", iconUrl, e.getMessage());
            return iconUrl;
        }
    }
}
