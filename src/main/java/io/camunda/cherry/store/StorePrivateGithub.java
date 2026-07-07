package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorePrivateGithub implements StoreAccess {

    private static final List<String> IGNORE = List.of("README.md", ".github", "docs");

    private final String name;
    private final String url;
    private final String filterProjectName;
    private final RestTemplate restTemplate;
    private final GitHubAccess gitHubAccess;

    Logger logger = LoggerFactory.getLogger(StorePrivateGithub.class.getName());

    public StorePrivateGithub(String name, String url, GitHubAccess gitHubAccess) {
        this.name = name;
        this.url = url;
        this.filterProjectName = null;
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

    public StorePrivateGithub(String name, String url, String filterProjectName, GitHubAccess gitHubAccess) {
        this.name = name;
        this.url = url;
        this.filterProjectName = filterProjectName;
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
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getType() {
        return "GitHub";
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  getListConnectors                                                   */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public List<ConnectorDefinition> getListConnectors() {
        long startTime = System.currentTimeMillis();
        List<ConnectorDefinition> result = new ArrayList<>();
        try {
            String apiReposUrl = buildReposApiUrl(url);
            if (apiReposUrl == null) {
                logger.error("Store[{}] cannot build repos API URL from url[{}]", getName(), url);
                return result;
            }
            List<String> listRepositories = getListRepositories(apiReposUrl);
            for (String fullName : listRepositories) {
                // fullName is "owner/repo", shortName is just "repo"
                String shortName = fullName.substring(fullName.lastIndexOf('/') + 1);
                String apiContentsUrl = "https://api.github.com/repos/" + fullName + "/contents";
                String htmlUrl = "https://github.com/" + fullName;

                GitHubAccess.GithubConnectorStatus status = gitHubAccess.isGithubConnector(apiContentsUrl, null, true, true);
                if (status.elementTemplates && status.pomXml && status.gitReleases) {
                    logger.info("Store[{}] Detect connector[{}] in url[{}]", getName(), shortName, htmlUrl);
                    ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, shortName, htmlUrl, null);
                    connectorDefinition.githubRepoName = fullName;
                    connectorDefinition.githubRepoPath = "";
                    result.add(connectorDefinition);

                } else if (status.elementTemplates) {
                    logger.info("Store[{}] Detect connector[{}] without implementation in url[{}]", getName(), shortName, htmlUrl);
                    ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, shortName, htmlUrl, null);
                    connectorDefinition.githubRepoName = fullName;
                    connectorDefinition.githubRepoPath = "";
                    connectorDefinition.hasImplementation = false;
                    result.add(connectorDefinition);
                }
            }
            logger.info("Store[{}] found {} connectors in {} ms", getName(), result.size(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            logger.error("Store[{}] error {}", getName(), e);
            return result;
        }
    }

    /**
     * Returns all repository full names ("owner/repo") by paginating through the GitHub
     * repos API (max 100 per page). Applies filterProjectName if set.
     * Note: GitHub silently caps per_page at 100, so pagination is required for large orgs.
     */
    private List<String> getListRepositories(String apiReposUrl) throws IOException {
        List<String> listRepositories = new ArrayList<>();
        int page = 1;
        int totalProjects = 0;

        while (true) {
            String pagedUrl = apiReposUrl + "&page=" + page;
            logger.info("Store[{}] listing repositories page {} from [{}]", getName(), page, pagedUrl);

            JsonNode repos;
            try {
                repos = gitHubAccess.getJsonNode(pagedUrl);
            } catch (Exception e) {
                logger.error("Store[{}] cannot list repositories from [{}]: {}", getName(), pagedUrl, e.getMessage());
                break;
            }

            if (!repos.isArray() || repos.size() == 0) {
                break;
            }

            for (JsonNode repo : repos) {
                totalProjects++;
                String repoName = repo.path("name").asText();
                if (filterProjectName != null
                        && !filterProjectName.isBlank()
                        && !repoName.contains(filterProjectName)) {
                    continue;
                }
                listRepositories.add(repo.path("full_name").asText());
            }

            page++;
        }

        logger.info("Store[{}] detected {} projects total, kept {} repositories (filter={})",
                getName(), totalProjects, listRepositories.size(), filterProjectName);
        return listRepositories;
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  ExploreDetails                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public void exploreDetails(ConnectorDefinition connectorDefinition) throws TechnicalException {
        if (connectorDefinition.hasImplementation) {
            connectorDefinition = fillJarDownload(connectorDefinition);
        }
        // Search the element template
        connectorDefinition = fillElementTemplate(connectorDefinition);
        connectorDefinition.status = EXPLORATION.READY;
    }


    /**
     * Finds the latest official release for the connector's GitHub repo,
     * saves it in connectorDefinition.release, then checks whether any
     * release asset is a JAR file.
     * If no JAR is found, connectorDefinition.hasImplementation is set to false.
     */
    private ConnectorDefinition fillJarDownload(ConnectorDefinition connectorDefinition) {
        // githubRepoName is "owner/repo" (e.g. "camunda-community-hub/my-connector")
        String repo = connectorDefinition.githubRepoName;

        // 1. Get the latest official release tag
        String release = gitHubAccess.getLatestRelease(repo);
        if (release == null) {
            logger.warn("Store[{}] connector[{}] has no official release", getName(), connectorDefinition.name);
            connectorDefinition.hasImplementation = false;
            return connectorDefinition;
        }
        connectorDefinition.release = release;

        // 2. List assets of that release and look for a JAR
        try {
            // GET /repos/{owner}/{repo}/releases/tags/{tag}
            String releaseUrl = "https://api.github.com/repos/" + repo + "/releases/tags/" + release;
            JsonNode releaseNode = gitHubAccess.getJsonNode(releaseUrl);
            JsonNode assets = releaseNode.path("assets");

            if (assets.isArray()) {
                for (JsonNode asset : assets) {
                    String assetName = asset.path("name").asText("");
                    if (assetName.endsWith(".jar")) {
                        connectorDefinition.urlJarFile = asset.path("browser_download_url").asText();
                        logger.info("Store[{}] connector[{}] release[{}] jar[{}]",
                                getName(), connectorDefinition.name, release, connectorDefinition.urlJarFile);
                        return connectorDefinition;
                    }
                }
            }
            // No JAR asset found
            logger.warn("Store[{}] connector[{}] release[{}] has no JAR asset — marking hasImplementation=false",
                    getName(), connectorDefinition.name, release);
            connectorDefinition.hasImplementation = false;
        } catch (Exception e) {
            logger.error("Store[{}] connector[{}] error fetching release assets: {}",
                    getName(), connectorDefinition.name, e.getMessage());
            connectorDefinition.hasImplementation = false;
        }
        return connectorDefinition;
    }

    /**
     * Searches the repo's element-templates directory for the first *.json file
     * using the default branch (HEAD), then reads description, documentationRef,
     * icon and connectorType from it.
     */
    private ConnectorDefinition fillElementTemplate(ConnectorDefinition connectorDefinition) {
        try {
            String rawUrl = gitHubAccess.exploreElementTemplate(
                    connectorDefinition.githubRepoName,
                    connectorDefinition.githubRepoPath,
                    "HEAD");

            if (rawUrl == null) {
                logger.warn("Store[{}] connector[{}] no .json found in element-templates",
                        getName(), connectorDefinition.name);
                return connectorDefinition;
            }
            connectorDefinition.urlElementTemplate = rawUrl;

            JsonNode jsonNode = gitHubAccess.getJsonNode(rawUrl);
            connectorDefinition.description = jsonNode.path("description").asText(connectorDefinition.description);
            connectorDefinition.documentationRef = jsonNode.path("documentationRef").asText(connectorDefinition.documentationRef);
            connectorDefinition.connectorType = extractTaskDefinitionType(jsonNode);
            JsonNode iconNode = jsonNode.path("icon");
            if (!iconNode.isMissingNode() && !iconNode.isNull()) {
                connectorDefinition.icon = iconNode.path("contents").asText(iconNode.asText(""));
            }
        } catch (Exception e) {
            logger.error("Store[{}] connector[{}] error in fillElementTemplate: {}",
                    getName(), connectorDefinition.name, e.getMessage());
        }
        return connectorDefinition;
    }

    private String extractTaskDefinitionType(JsonNode elementTemplate) {
        return gitHubAccess.extractTaskDefinitionType(elementTemplate);
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Download                                                            */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {
        return null;
    }


    private JsonNode get(String apiUrl) throws TechnicalException {
        try {
            return JsonUtils.toJsonNode(restTemplate.getForObject(apiUrl, String.class));
        } catch (RestClientException | IOException e) {
            throw new TechnicalException("Error reading " + apiUrl + " : " + e.getMessage(), e);
        }
    }

    /**
     * Builds the GitHub REST API URL to list repositories from a GitHub profile URL.
     * <p>
     * https://github.com/orgs/camunda-community-hub
     * -> https://api.github.com/orgs/camunda-community-hub/repos?per_page=100&type=all&sort=full_name
     * <p>
     * https://github.com/pierre-yves-monnet
     * -> https://api.github.com/users/pierre-yves-monnet/repos?per_page=100&type=all&sort=full_name
     */
    private String buildReposApiUrl(String githubUrl) {
        if (githubUrl == null) return null;
        String trimmed = githubUrl.endsWith("/") ? githubUrl.substring(0, githubUrl.length() - 1) : githubUrl;

        // org URL: github.com/orgs/{org}
        java.util.regex.Matcher orgMatcher = java.util.regex.Pattern
                .compile("github\\.com/orgs/([^/]+)$")
                .matcher(trimmed);
        if (orgMatcher.find()) {
            return "https://api.github.com/orgs/" + orgMatcher.group(1) + "/repos?per_page=100&type=all&sort=full_name";
        }

        // user URL: github.com/{user}
        java.util.regex.Matcher userMatcher = java.util.regex.Pattern
                .compile("github\\.com/([^/]+)$")
                .matcher(trimmed);
        if (userMatcher.find()) {
            return "https://api.github.com/users/" + userMatcher.group(1) + "/repos?per_page=100&type=all&sort=full_name";
        }

        return null;
    }
}
