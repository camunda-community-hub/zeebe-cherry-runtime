package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runtime.CherryProperties;
import io.camunda.cherry.store.git.GitHubAccess;
import io.camunda.cherry.store.mavencentral.MavenCentralDownload;
import io.camunda.cherry.store.mavencentral.MavenCredential;
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

public abstract class StoreGithub extends StoreAccess {
    private static final List<String> IGNORE = List.of("README.md", ".github", "docs");
    private final String name;
    private final String url;
    private final String filterProjectName;
    private final RestTemplate restTemplate;
    private final GitHubAccess gitHubAccess;
    private final MavenCentralDownload mavenCentralDownload = new MavenCentralDownload();
    private final String REPOS_PER_PAGE = "50";
    Logger logger = LoggerFactory.getLogger(StoreGithub.class.getName());

    public StoreGithub(String name, String url, GitHubAccess gitHubAccess, CherryProperties.Startup startup) {
        this(name, url, null, gitHubAccess, startup);
    }


    /**
     * @param name              store name
     * @param url               GitHub profile/org URL
     * @param filterProjectName optional filter applied to repository names when listing (e.g. "camunda-8-connector")
     * @param gitHubAccess      GitHub access
     * @param startup           startup-download configuration for this store
     */
    public StoreGithub(String name, String url, String filterProjectName, GitHubAccess gitHubAccess, CherryProperties.Startup startup) {
        super(startup);
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


    /* ******************************************************************** */
    /*                                                                      */
    /*  getListConnectors                                                   */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public List<ConnectorDefinition> exploreListConnectors() {
        long startTime = System.currentTimeMillis();
        logger.info("Store[{}] startExploreListConnectors ", getName());
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
                String repoPath = "https://api.github.com/repos/" + fullName + "/contents";
                String htmlUrl = "https://github.com/" + fullName;

                GitHubAccess.GithubConnectorStatus gitHubConnectorStatus = gitHubAccess.qualifyRepository(repoPath, null, true, true);

                // To be qualify, the repository must have an elementTemplate OR gitRelease OR pom.xml
                if (!(gitHubConnectorStatus.elementTemplates || gitHubConnectorStatus.existGitReleases || gitHubConnectorStatus.existPomXml)) {
                    continue;
                }
                ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, shortName, htmlUrl, null);
                connectorDefinition.githubRepoName = fullName;
                connectorDefinition.githubRepoPath = "";
                connectorDefinition.release = gitHubConnectorStatus.gitLastRelease;


                //--------------- identify the Jar file
                // First, priority to a release directly attached to the Git repository
                if (connectorDefinition.release != null && !connectorDefinition.release.isEmpty()) {
                    try {
                        connectorDefinition.urlJarFile = gitHubAccess.getGitHubRelease(repoPath).getJarFromRelease(connectorDefinition.release);// identify the Jar in the release
                    } catch (Exception e) {
                        logger.error("Store[{}] error while getting release for repo[{}]", getName(), fullName, e);
                        connectorDefinition.urlJarFile = null;
                    }
                }

                if (connectorDefinition.urlJarFile == null) {
                    // If no release are detected, or no jar in the release, we need to go to a second pass.
                    // search a pom.xml
                    // Open it and get groupId, artefactId, groupId
                    // then search in maven central and/or JFrog is an implementation exist
                    // if yes, then point the jar file to that URL
                    if (gitHubConnectorStatus.existPomXml)
                        connectorDefinition = searchMavenRepoExistence(gitHubConnectorStatus, connectorDefinition);
                }
                connectorDefinition.hasImplementation = connectorDefinition.urlJarFile != null;
                logger.info("Store[{}] Detect connector[{}] in url[{}] Implementation[{}] jarFile[{}]",
                        getName(),
                        shortName,
                        htmlUrl,
                        connectorDefinition.hasImplementation,
                        connectorDefinition.urlJarFile);

                result.add(connectorDefinition);
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
        if (connectorDefinition.mavenProvider != null) {
            logger.info("Store[{}] Start downloading connector[{}] from Maven provider[{}]...", getName(), connectorDefinition.name, connectorDefinition.mavenProvider);
            return mavenCentralDownload.downloadConnector(connectorDefinition, connectorDefinition.mavenProvider);
        }
        logger.info("Store[{}] Start downloading connector[{}] from url[{}]...", getName(), connectorDefinition.name, connectorDefinition.urlJarFile);
        ConnectorDownload connectorDownload = new ConnectorDownload();
        if (connectorDefinition.urlJarFile == null || connectorDefinition.urlJarFile.isEmpty()) {
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "Store [" + connectorDefinition.storeAccess.getName() + "] No JAR file URL available for connector [" + connectorDefinition.name + "]";
            return connectorDownload;
        }
        try {
            long startTime = System.currentTimeMillis();
            connectorDownload.jarName = connectorDefinition.getJarName();
            byte[] jarBytes = restTemplate.getForObject(connectorDefinition.urlJarFile, byte[].class);
            if (jarBytes == null) {
                connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
                connectorDownload.explanation = "Store [" + connectorDefinition.storeAccess.getName() + "] Empty response from [" + connectorDefinition.urlJarFile + "]";
                return connectorDownload;
            }
            connectorDownload.jarContent = new java.io.ByteArrayInputStream(jarBytes);
            connectorDownload.status = STATUSDOWNLOAD.OK;
            connectorDownload.release = connectorDefinition.release;
            connectorDownload.explanation = "Store [" + connectorDefinition.storeAccess.getName() + "] Downloaded [" + connectorDefinition.urlJarFile + "] "
                    + jarBytes.length + " bytes in "
                    + (System.currentTimeMillis() - startTime) + " ms";
            logger.info("Store[{}] Download connector[{}] from url[{}] length {} in ms", getName(), connectorDefinition.name, connectorDefinition.urlJarFile,
                    jarBytes.length,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Store[{}] connector[{}] error downloading JAR from [{}]: {}",
                    getName(), connectorDefinition.name, connectorDefinition.urlJarFile, e.getMessage());
            connectorDownload.status = STATUSDOWNLOAD.UNKNOWNRELEASE;
            connectorDownload.explanation = "Store [" + connectorDefinition.storeAccess.getName() + "] Error downloading JAR: " + e.getMessage();
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

    protected abstract String getTypeRepo();


    private ConnectorDefinition searchMavenRepoExistence(GitHubAccess.GithubConnectorStatus gitHubStatus, ConnectorDefinition connectorDefinition) {
        if (gitHubStatus.pomContent == null) {
            logger.error("Store[{}] Connector [{}], pom is null", getName(), connectorDefinition.name);
            return connectorDefinition;
        }
        MavenCentralDownload.PomDescription pomDescription = mavenCentralDownload.decodePom(gitHubStatus.pomContent);
        if (pomDescription == null) {
            logger.error("Store[{}] can't parse pom.xml for connector [{}], can't search a Maven provider", getName(), connectorDefinition.name);
            return connectorDefinition;
        }
        // Search in all providers if the object exist
        for (MavenCredential provider : getMavenProviders()) {
            String url = mavenCentralDownload.buildMavenUrl(provider, pomDescription.groupId, pomDescription.artifactId, pomDescription.version);
            logger.info("Store[{}] search in Maven provider [{}] for connector [{}] with URL [{}]", getName(), provider, connectorDefinition.name, url);
            try {
                if (mavenCentralDownload.checkIfExist(url)) {
                    logger.info("Store[{}] connector [{}] found in Maven provider [{}] with URL [{}]", getName(), connectorDefinition.name, provider, url);
                    connectorDefinition.urlJarFile = url;
                    connectorDefinition.mavenProvider = provider.name;
                    connectorDefinition.hasImplementation = true;
                    return connectorDefinition;
                } else {
                    connectorDefinition.description+="Url["+url+"] return nothing;";
                }
            } catch (Exception e) {
                logger.error("Store[{}] error searching in Maven provider [{}] for connector [{}] with URL [{}]: {}", getName(), provider, connectorDefinition.name, url, e.getMessage());
            }
        }
        return connectorDefinition;
    }


    public List<MavenCredential> getMavenProviders() {
        return List.of(MavenCredential.getInstance("maven", "https://repo1.maven.org/maven2/"));
    }
}
