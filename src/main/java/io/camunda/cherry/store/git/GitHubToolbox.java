package io.camunda.cherry.store.git;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.util.JsonUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;

/**
 * Low-level HTTP/JSON helpers for talking to GitHub. Package-private: only GitHubAccess (which
 * creates it) and GitHubRelease (same package) use it directly; cross-package callers go through
 * GitHubAccess.getJsonNode()/extractTaskDefinitionType(). Not a Spring bean - the token is captured
 * once at construction (handed to it by GitHubAccess, the only place that ever reads it from
 * configuration) rather than passed on every call.
 */
class GitHubToolbox {

    private static final Duration GITHUB_TIMEOUT = Duration.ofSeconds(15);
    private final RestTemplate restTemplate = buildRestTemplate();
    private final String githubToken;

    GitHubToolbox(String githubToken) {
        this.githubToken = githubToken;
    }

    /**
     * Every call to GitHub goes through {@link #get(String)} or {@link #getJsonNode(String)},
     * both backed by this single RestTemplate — a bad/slow connection to GitHub (e.g. a network
     * blip) would otherwise hang close to indefinitely instead of failing fast.
     */
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(GITHUB_TIMEOUT);
        factory.setReadTimeout(GITHUB_TIMEOUT);
        return new RestTemplate(factory);
    }

    protected JsonNode getJsonNode(String url) throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        byte[] body = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();
        if (body == null) return JsonUtils.toJsonNode("null");
        return JsonUtils.toJsonNode(new ByteArrayInputStream(body));
    }

    String get(String url) throws IOException {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (githubToken != null && !githubToken.isBlank()) {
            headers.set("Authorization", "Bearer " + githubToken);
        }
        return headers;
    }

    /**
     * Extracts the connector type from an element-template JSON.
     * Handles two binding formats:
     * Legacy: { "type": "zeebe:taskDefinition", "property": "type" }
     * Compact: { "type": "zeebe:taskDefinition:type" }
     */
    String extractTaskDefinitionType(JsonNode elementTemplate) {
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
     * Decodes the base64 {@code content} field returned by the GitHub contents API for a single file.
     */
    String decodeGithubFileContent(JsonNode fileNode) {
        String content = fileNode.path("content").asText(null);
        if (content == null)
            return null;
        String encoding = fileNode.path("encoding").asText("base64");
        if (!"base64".equals(encoding))
            return content;
        try {
            byte[] decoded = java.util.Base64.getMimeDecoder().decode(content);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
