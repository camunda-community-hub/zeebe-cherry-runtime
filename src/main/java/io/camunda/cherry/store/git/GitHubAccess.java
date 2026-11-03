package io.camunda.cherry.store.git;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.store.StoreAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubAccess {

    /**
     * Explores the element-templates directory of a GitHub repo and returns the raw URL
     * of the first *.json file found.
     */
    private static final List<String> ELEMENT_TEMPLATE_DIRS = List.of(
            "element-templates", "connector-template", "connector-templates");
    /**
     * package-private: GitHubRelease (same package) accesses it directly. Cross-package callers
     * (StoreGithub, StoreCamundaConnector) go through getJsonNode()/extractTaskDefinitionType() below.
     */
    final GitHubToolbox gitHubToolbox;
    Logger logger = LoggerFactory.getLogger(GitHubAccess.class.getName());
    private final String githubToken;

    public GitHubAccess(@Value("${cherry.github.token:}") String githubToken) {
        // Built here, not via @Value field injection: field injection runs AFTER the constructor,
        // so the token would still be null if GitHubToolbox were created from a field value.
        this.githubToken = githubToken;
        this.gitHubToolbox = new GitHubToolbox(githubToken);
    }

    public String getGithubToken() {
        return githubToken;
    }

    public boolean isToken() {
        return githubToken != null && !githubToken.isBlank();
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
    public GithubConnectorStatus qualifyRepository(String repoPath, String release, boolean checkPomXml, boolean checkGitRelease) {
        GithubConnectorStatus githubConnectorStatus = new GithubConnectorStatus();
        String ref = (release != null && !release.isBlank()) ? release : "HEAD";
        try {
            // check element template
            JsonNode items = getJsonNode(repoPath + "/element-templates?ref=" + ref);
            githubConnectorStatus.elementTemplates=items.isArray() && items.size() > 0;

            // check pom.xml — most repos simply don't have one; that's an expected, silent "no" answer
            if (checkPomXml) {
                try {
                    JsonNode itemsPom =  getJsonNode(repoPath + "/pom.xml?ref=" + ref);
                    String type = itemsPom.path("type").asText();
                    githubConnectorStatus.existPomXml = type.equals("file");
                    githubConnectorStatus.pomContent = gitHubToolbox.decodeGithubFileContent(itemsPom);
                } catch (Exception ex) {
                    githubConnectorStatus.existPomXml = false;
                }
            }
            if (checkGitRelease) {
                GitHubRelease gitHubRelease = new GitHubRelease(repoPath, this);
                // Extract repo path (owner/repo) from the API URL
                // repoPath is like https://api.github.com/repos/owner/repo/contents/...
                try {
                    githubConnectorStatus.gitLastRelease = gitHubRelease.getLatestRelease() ;
                    githubConnectorStatus.existGitReleases = githubConnectorStatus.gitLastRelease != null;

                } catch (Exception ex) {
                    githubConnectorStatus.existGitReleases = false;
                }
            }

            return githubConnectorStatus;
        } catch (Exception e) {
            return githubConnectorStatus;
        }
    }

    public GitHubRelease getGitHubRelease(String repoPath) {
        return new GitHubRelease(repoPath, this);
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
                JsonNode items =  getJsonNode(apiUrl);
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
        return gitHubToolbox.extractTaskDefinitionType(elementTemplate);
    }

    /**
     * Finds the latest official release of a GitHub repo (e.g. "camunda/connectors").
     *
     * @param repoPath repo to check, as "owner/repo"
     * @return the release tag, or null if none was found
     */
    public String getLatestRelease(String repoPath) {
        return new GitHubRelease(repoPath, this).getLatestRelease();
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
        GitHubRelease gitHubRelease = new GitHubRelease(repo, this);
        String release = gitHubRelease.getLatestRelease();
        if (release == null) {
            logger.warn("Store[{}] connector[{}] has no official release", storeName, connectorDefinition.name);

            // Maybe the jar exist in maven central?
            // mavenCentralDow

            connectorDefinition.hasImplementation = false;
            return connectorDefinition;
        }
        connectorDefinition.release = release;
        try {
        connectorDefinition.urlJarFile = gitHubRelease.getJarFromRelease(release);

        connectorDefinition.hasImplementation = connectorDefinition.urlJarFile!=null;
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
        JsonNode jsonNode =  getJsonNode(urlTemplate);
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


    /**
     * Kept here only because StoreGithub/StoreCamundaConnector (a different package) need it;
     * GitHubRelease (same package as GitHubAccess) calls gitHubToolbox.getJsonNode(url) directly instead.
     */
    public JsonNode getJsonNode(String url) throws Exception {
        return gitHubToolbox.getJsonNode(url);
    }

    public static class GithubConnectorStatus {
        public boolean elementTemplates = false;
        public boolean existPomXml = false;
        public boolean existGitReleases = false;
        public String gitLastRelease;
        public String pomContent;

    }
}
