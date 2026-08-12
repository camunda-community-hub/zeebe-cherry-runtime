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


    private final String REPOS_PER_PAGE = "50";

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
    public List<ConnectorDefinition> exploreListConnectors() {
        long startTime = System.currentTimeMillis();
        logger.info("Store[{}] startListDetector ", getName());
        List<ConnectorDefinition> result = new ArrayList<>();
        try {
            String apiReposUrl = buildReposApiUrl(url, getTypeRepo());
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
                if (status.elementTemplates) {
                    ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, shortName, htmlUrl, null);
                    connectorDefinition.githubRepoName = fullName;
                    connectorDefinition.githubRepoPath = "";
                    connectorDefinition.hasImplementation = status.pomXml && status.gitReleases;
                    logger.info("Store[{}] Detect connector[{}] in url[{}] Implementation[{}]", getName(), shortName, htmlUrl, connectorDefinition.hasImplementation);

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

        logger.debug("Store[{}] detected {} projects total, kept {} repositories (filter={})",
                getName(), totalProjects, listRepositories.size(), filterProjectName);
        return listRepositories;
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  ExploreDetails                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public boolean exploreDetails(ConnectorDefinition connectorDefinition) throws TechnicalException {
        if (connectorDefinition.hasImplementation) {
            connectorDefinition = gitHubAccess.fillJarDownload(getName(), connectorDefinition);
        }
        connectorDefinition = gitHubAccess.fillAllElementTemplates(getName(), connectorDefinition);
        return true;
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Download                                                            */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {
        logger.info("Store[{}] Start downloading connector[{}] from url[{}]...", getName(), connectorDefinition.name, connectorDefinition.urlJarFile);
        ConnectorDownload connectorDownload = new ConnectorDownload();
        if (connectorDefinition.urlJarFile == null || connectorDefinition.urlJarFile.isEmpty()) {
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "No JAR file URL available for connector [" + connectorDefinition.name + "]";
            return connectorDownload;
        }
        try {
            long startTime = System.currentTimeMillis();
            connectorDownload.jarName = connectorDefinition.urlJarFile.substring(
                    connectorDefinition.urlJarFile.lastIndexOf('/') + 1);
            byte[] jarBytes = restTemplate.getForObject(connectorDefinition.urlJarFile, byte[].class);
            if (jarBytes == null) {
                connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
                connectorDownload.explanation = "Empty response from [" + connectorDefinition.urlJarFile + "]";
                return connectorDownload;
            }
            connectorDownload.jarContent = new java.io.ByteArrayInputStream(jarBytes);
            connectorDownload.status = STATUSDOWNLOAD.OK;
            connectorDownload.release = connectorDefinition.release;
            connectorDownload.explanation = "Downloaded " + jarBytes.length + " bytes from [" + connectorDefinition.urlJarFile + "]";
            logger.info("Store[{}] Download connector[{}] from url[{}] length {} in ms", getName(), connectorDefinition.name, connectorDefinition.urlJarFile,
                    jarBytes.length,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Store[{}] connector[{}] error downloading JAR from [{}]: {}",
                    getName(), connectorDefinition.name, connectorDefinition.urlJarFile, e.getMessage());
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "Error downloading JAR: " + e.getMessage();
        }
        return connectorDownload;
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
     * -> https://api.github.com/users/pierre-yves-monnet/repos?per_page=100&type=owner&sort=full_name
     */
    private String buildReposApiUrl(String githubUrl, String type) {
        if (githubUrl == null) return null;
        String trimmed = githubUrl.endsWith("/") ? githubUrl.substring(0, githubUrl.length() - 1) : githubUrl;

        // org URL: github.com/orgs/{org}
        java.util.regex.Matcher orgMatcher = java.util.regex.Pattern
                .compile("github\\.com/orgs/([^/]+)$")
                .matcher(trimmed);
        if (orgMatcher.find()) {
            return "https://api.github.com/orgs/" + orgMatcher.group(1) + "/repos?per_page=" + REPOS_PER_PAGE + "&type=" + type + "&sort=full_name";
        }

        // user URL: github.com/{user}
        java.util.regex.Matcher userMatcher = java.util.regex.Pattern
                .compile("github\\.com/([^/]+)$")
                .matcher(trimmed);
        if (userMatcher.find()) {
            return "https://api.github.com/users/" + userMatcher.group(1) + "/repos?per_page=" + REPOS_PER_PAGE + "&type=owner&sort=full_name";
        }

        return null;
    }

    private String getTypeRepo() {
        return "owner";
    }
}
