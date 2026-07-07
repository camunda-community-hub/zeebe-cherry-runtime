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
    Logger logger = LoggerFactory.getLogger(GitHubAccess.class.getName());

    @Value("${cherry.github.token:}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();

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
            return JsonUtils.toJsonNode(get(url));
    }

    public String getLatestRelease(String repo) {
        try {
            String url = "https://api.github.com/repos/" + repo + "/releases?per_page=1000";
            JsonNode releases = JsonUtils.toJsonNode(get(url));
            if (!releases.isArray()) return null;

            List<JsonNode> releaseList = new ArrayList<>();
            releases.forEach(releaseList::add);

            return releaseList.stream()
                    .filter(r -> OFFICIAL_RELEASE.matcher(r.path("tag_name").asText("")).matches())
                    .max(Comparator.comparingLong(r -> versionWeight(r.path("tag_name").asText(""))))
                    .map(r -> r.path("tag_name").asText())
                    .orElse(null);
        } catch (IOException e) {
            throw new RuntimeException("Error fetching releases for repo " + repo, e);
        }
    }


    public class GithubConnectorStatus {
    public boolean elementTemplates=false;
    public boolean pomXml=false;
    public boolean gitReleases=false;

    }
    /**
     *  a Github connector must have:
     *  * an element-template
     *  * a pom.xml (option)
     *  * the JAR file present as a release (option
     * @param repoPath
     * @param release
     * @return
     */
    public GithubConnectorStatus isGithubConnector( String repoPath, String release, boolean checkPomxml, boolean checkGitRelease) {
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
                githubConnectorStatus.pomXml=type.equals("file");
            }
            if (checkGitRelease) {
                // Extract repo path (owner/repo) from the API URL
                // repoPath is like https://api.github.com/repos/owner/repo/contents/...
                String releasesUrl = repoPath.replaceAll("(https://api\\.github\\.com/repos/[^/]+/[^/]+)/.*", "$1") + "/releases?per_page=1";
                try {
                    JsonNode releases = getJsonNode(releasesUrl);
                    githubConnectorStatus.gitReleases = releases.isArray() && releases.size() > 0;
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
     * Explores the element-templates directory of a GitHub repo and returns the raw URL
     * of the first *.json file found.
     *
     * @param repo     "owner/repo"
     * @param repoPath path inside the repo to the connector root (may be empty/null)
     * @param ref      release tag (e.g. "8.6.1") or "HEAD" for the default branch
     * @return raw URL of the first .json element-template, or null if none found
     */
    public String exploreElementTemplate(String repo, String repoPath, String ref) throws Exception {
        String subPath = (repoPath != null && !repoPath.isBlank())
                ? repoPath + "/element-templates"
                : "element-templates";

        String apiUrl = "https://api.github.com/repos/" + repo + "/contents/" + subPath;
        if (ref != null && !ref.isBlank()) {
            apiUrl += "?ref=" + ref;
        }

        JsonNode items = getJsonNode(apiUrl);
        if (!items.isArray()) {
            return null;
        }

        for (JsonNode item : items) {
            String fileName = item.path("name").asText("");
            if (fileName.endsWith(".json")) {
                return "https://raw.githubusercontent.com/" + repo + "/" + ref
                        + "/" + subPath + "/" + fileName;
            }
        }
        return null;
    }

    /**
     * Extracts the connector type from an element-template JSON.
     * Handles two binding formats:
     *   Legacy: { "type": "zeebe:taskDefinition", "property": "type" }
     *   Compact: { "type": "zeebe:taskDefinition:type" }
     */
    public String extractTaskDefinitionType(JsonNode elementTemplate) {
        JsonNode properties = elementTemplate.path("properties");
        if (!properties.isArray()) return null;
        for (JsonNode prop : properties) {
            JsonNode binding = prop.path("binding");
            String bindingType = binding.path("type").asText("");
            if ("zeebe:taskDefinition:type".equals(bindingType)
                    || ("zeebe:taskDefinition".equals(bindingType)
                        && "type".equals(binding.path("property").asText()))) {
                return prop.path("value").asText(null);
            }
        }
        return null;
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
}
