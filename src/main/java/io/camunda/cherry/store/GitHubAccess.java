package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class GitHubAccess {

    private static final Pattern OFFICIAL_RELEASE = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    /**
     * Explores the element-templates directory of a GitHub repo and returns the raw URL
     * of the first *.json file found.
     */
    private static final List<String> ELEMENT_TEMPLATE_DIRS = List.of(
            "element-templates", "connector-template", "connector-templates");
    private final RestTemplate restTemplate = new RestTemplate();
    Logger logger = LoggerFactory.getLogger(GitHubAccess.class.getName());
    @Value("${cherry.github.token:}")
    private String githubToken;

    public String getGithubToken() {
        return githubToken;
    }

    public boolean isToken() {
        return githubToken != null && !githubToken.isBlank();
    }

    public String get(String url) throws IOException {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
    }

    public JsonNode getJsonNode(String url) throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        byte[] body = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();
        if (body == null) return JsonUtils.toJsonNode("null");
        return JsonUtils.toJsonNode(new java.io.ByteArrayInputStream(body));
    }

    public String getLatestRelease(String repo) {
        String url = null;
        try {
            if (repo == null || repo.isEmpty())
                return null;

            if (repo.contains("github.com")) {
                // e.g. "https://github.com/owner/repo" → "https://api.github.com/repos/owner/repo/releases?per_page=1000"
                url = repo.replaceFirst("https?://github\\.com/", "https://api.github.com/repos/")
                        + "/releases?per_page=1000";
            } else {
                url = "https://api.github.com/repos/" + repo + "/releases?per_page=1000";
            }

            JsonNode releases = JsonUtils.toJsonNode(get(url));
            if (!releases.isArray()) return null;

            List<JsonNode> releaseList = new ArrayList<>();
            releases.forEach(releaseList::add);

            String release = releaseList.stream()
                    .filter(r -> OFFICIAL_RELEASE.matcher(r.path("tag_name").asText("")).matches())
                    .max(Comparator.comparingLong(r -> versionWeight(r.path("tag_name").asText(""))))
                    .map(r -> r.path("tag_name").asText())
                    .orElse(null);
            // if no official release is find, then we keep the first release, according it is order by the inverse order of the date
            if ((release == null || release.isBlank()) && !releaseList.isEmpty()) {
                release = releaseList.get(0).path("tag_name").asText();
            }
            return release;
        } catch (Exception e) {
            // this acceptable: the repo does not contains any target, or the link point to a path INSIDE a repo
            if (e.getMessage().contains("404 Not Found"))
                return null;
            logger.error("getLastestRelease repo[{}] urlToGetRelease[{}] : {}", repo, url, e);
            return null;
        }
    }

    /**
     * a Github connector must have:
     * * an element-template
     * * a pom.xml (option)
     * * the JAR file present as a release (option
     *
     * @param repoPath path to explore
     * @param release  release for the path
     * @return status
     */
    public GithubConnectorStatus isGithubConnector(String repoPath, String release, boolean checkPomxml, boolean checkGitRelease) {
        GithubConnectorStatus githubConnectorStatus = new GithubConnectorStatus();
        String ref = (release != null && !release.isBlank()) ? release : "HEAD";
        try {
            // check element template
            JsonNode items = getJsonNode(repoPath + "/element-templates?ref=" + ref);
            if (!items.isArray())
                return githubConnectorStatus;
            githubConnectorStatus.elementTemplates = true;
            // check pom.xml
            if (checkPomxml) {
                JsonNode itemsPom = getJsonNode(repoPath + "/pom.xml?ref=" + ref);
                String type = itemsPom.path("type").asText();
                githubConnectorStatus.pomXml = type.equals("file");
            }
            if (checkGitRelease) {
                // Extract repo path (owner/repo) from the API URL
                // repoPath is like https://api.github.com/repos/owner/repo/contents/...
                String releasesUrl = repoPath.replaceAll("(https://api\\.github\\.com/repos/[^/]+/[^/]+)/.*", "$1") + "/releases?per_page=1";
                try {
                    JsonNode releases = getJsonNode(releasesUrl);
                    githubConnectorStatus.gitReleases = releases.isArray() && !releases.isEmpty();
                } catch (Exception ex) {
                    githubConnectorStatus.gitReleases = false;
                }
            }
            return githubConnectorStatus;
        } catch (Exception e) {
            return githubConnectorStatus;
        }
    }

    /**
     * Explore a path and return all *.json file, considering they are element-templates
     *
     * @param repo     repo to explore
     * @param repoPath path in the repo
     * @param ref      to add in the URL
     * @return list of file founds
     * @throws Exception any error
     */
    public List<String> fillOneElementTemplate(String repo, String repoPath, String ref) {
        List<String> listElementsTemplate = new ArrayList<>();
        String base = (repoPath != null && !repoPath.isBlank()) ? repoPath + "/" : "";
        for (String dir : ELEMENT_TEMPLATE_DIRS) {
            String subPath = base + dir;
            String apiUrl = "https://api.github.com/repos/" + repo + "/contents/" + subPath;
            if (ref != null && !ref.isBlank()) {
                apiUrl += "?ref=" + ref;
            }
            try {
                JsonNode items = getJsonNode(apiUrl);
                if (!items.isArray()) continue;
                for (JsonNode item : items) {
                    String fileName = item.path("name").asText("");
                    if (fileName.endsWith(".json")) {
                        listElementsTemplate.add("https://raw.githubusercontent.com/" + repo + "/" + ref
                                + "/" + subPath + "/" + fileName);
                    }
                }
            } catch (Exception e) {
                // directory does not exist, try next
            }
        }
        return listElementsTemplate;

    }

    /**
     * Extracts the connector type from an element-template JSON.
     * Handles two binding formats:
     * Legacy: { "type": "zeebe:taskDefinition", "property": "type" }
     * Compact: { "type": "zeebe:taskDefinition:type" }
     */
    public String extractTaskDefinitionType(JsonNode elementTemplate) {
        String connectorType = null;
        JsonNode properties = elementTemplate.path("properties");
        if (!properties.isArray())
            return null;
        for (JsonNode prop : properties) {
            JsonNode binding = prop.path("binding");
            String bindingType = binding.path("type").asText("");
            if ("zeebe:taskDefinition:type".equals(bindingType)
                    || ("zeebe:taskDefinition".equals(bindingType)
                    && "type".equals(binding.path("property").asText()))
                    || ("zeebe:property".equals(bindingType)
                    && "inbound.type".equals(binding.path("name").asText()))) {
                connectorType = prop.path("value").asText(null);
            }
        }
        // if the connectorType is actually a JSON?
        // A FEEL context expression starts with "=" and may contain a "baseName" attribute
        // e.g. "={\n  baseName: \"camunda::RPA-Task::\",\n  ...}.definitionType"
        if (connectorType != null && connectorType.startsWith("=")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("baseName\\s*:\\s*\"([^\"]+)\"")
                    .matcher(connectorType);
            if (matcher.find()) {
                connectorType = matcher.group(1);
            }
        }

        return connectorType;
    }

    /**
     * Finds the latest official release for the connector's GitHub repo,
     * saves it in connectorDefinition.release, then checks whether any
     * release asset is a JAR file.
     */
    public StoreAccess.ConnectorDefinition fillJarDownload(String storeName, StoreAccess.ConnectorDefinition connectorDefinition) {
        // Impossible to check the JAR if there are no repo
        if (connectorDefinition.githubRepoName == null || connectorDefinition.githubRepoName.isBlank())
            return connectorDefinition;
        String repo = connectorDefinition.githubRepoName;
        String release = getLatestRelease(repo);
        if (release == null) {
            logger.warn("Store[{}] connector[{}] has no official release", storeName, connectorDefinition.name);
            connectorDefinition.hasImplementation = false;
            return connectorDefinition;
        }
        connectorDefinition.release = release;
        try {
            String releaseUrl = "https://api.github.com/repos/" + repo + "/releases/tags/" + release;
            JsonNode releaseNode = getJsonNode(releaseUrl);
            JsonNode assets = releaseNode.path("assets");
            if (assets.isArray()) {
                for (JsonNode asset : assets) {
                    String assetName = asset.path("name").asText("");
                    if (assetName.endsWith(".jar")) {
                        connectorDefinition.urlJarFile = asset.path("browser_download_url").asText();
                        logger.debug("Store[{}] connector[{}] release[{}] jar[{}]",
                                storeName, connectorDefinition.name, release, connectorDefinition.urlJarFile);
                        return connectorDefinition;
                    }
                }
            }
            logger.warn("Store[{}] connector[{}] release[{}] has no JAR asset — marking hasImplementation=false",
                    storeName, connectorDefinition.name, release);
            connectorDefinition.hasImplementation = false;
        } catch (Exception e) {
            logger.error("Store[{}] connector[{}] error fetching release assets: {}",
                    storeName, connectorDefinition.name, e.getMessage());
            connectorDefinition.hasImplementation = false;
        }
        return connectorDefinition;
    }

    /**
     * Searches the repo's element-templates directory for the first *.json file
     * and reads description, documentationRef, icon and connectorType from it.
     */
    public StoreAccess.ConnectorDefinition fillAllElementTemplates(String storeName, StoreAccess.ConnectorDefinition connectorDefinition) {
        try {
            List<String> urlTemplates = fillOneElementTemplate(
                    connectorDefinition.githubRepoName,
                    connectorDefinition.githubRepoPath,
                    "HEAD");
            if (urlTemplates.isEmpty()) {
                logger.warn("Store[{}] connector[{}] no .json found in element-templates", storeName, connectorDefinition.name);
                return connectorDefinition;
            }
            for (String url : urlTemplates) {
                connectorDefinition = fillOneElementTemplate(url, connectorDefinition);
            }
        } catch (Exception e) {
            logger.error("Store[{}] connector[{}] error in fillElementTemplate: {}",
                    storeName, connectorDefinition.name, e.getMessage());
        }
        return connectorDefinition;
    }


    public StoreAccess.ConnectorDefinition fillOneElementTemplate(String urlTemplate, StoreAccess.ConnectorDefinition connectorDefinition) throws Exception {
        StoreAccess.ElementTemplateDescription elementTemplateDescription = new StoreAccess.ElementTemplateDescription(urlTemplate);
        connectorDefinition.listEltTemplate.add(elementTemplateDescription);
        JsonNode jsonNode = getJsonNode(urlTemplate);
        elementTemplateDescription.name = jsonNode.path("name").asText(connectorDefinition.name);
        elementTemplateDescription.description = jsonNode.path("description").asText(connectorDefinition.name);
        elementTemplateDescription.connectorType = extractTaskDefinitionType(jsonNode);
        elementTemplateDescription.version = jsonNode.path("version").asText(connectorDefinition.description);

        // Put at the top level this element-templae information
        connectorDefinition.name = elementTemplateDescription.name;
        connectorDefinition.description = elementTemplateDescription.description;
        connectorDefinition.documentationRef = jsonNode.path("documentationRef").asText(connectorDefinition.documentationRef);
        // Inbound connector does not have a type, so if the connector act as Inbound and Outbound, some element-template has a type
        if (connectorDefinition.connectorType == null) {
            connectorDefinition.connectorType = elementTemplateDescription.connectorType;
        }
        JsonNode iconNode = jsonNode.path("icon");
        if (!iconNode.isMissingNode() && !iconNode.isNull()) {
            connectorDefinition.icon = iconNode.path("contents").asText(iconNode.asText(""));
        }
        return connectorDefinition;
    }


    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (isToken()) {
            headers.set("Authorization", "Bearer " + githubToken);
        }
        return headers;
    }

    private long versionWeight(String version) {
        String[] parts = version.split("\\.");
        long x = Long.parseLong(parts[0]);
        long y = Long.parseLong(parts[1]);
        long z = Long.parseLong(parts[2]);
        return x * 10_000_000L + y * 10_000L + z;
    }

    public static class GithubConnectorStatus {
        public boolean elementTemplates = false;
        public boolean pomXml = false;
        public boolean gitReleases = false;

    }
}
