package io.camunda.cherry.store.git;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
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
    // GitHub's secondary/abuse-detection rate limit (HTTP 429) is a short burst limiter, separate
    // from the hourly 60/5000 quota - typically clears within well under a minute. Worth a couple
    // of retries instead of failing the whole page/qualify-check outright.
    private static final int MAX_RETRIES_ON_RATE_LIMIT = 2;
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 5;
    private static final long MAX_RETRY_AFTER_SECONDS = 60;
    private final RestTemplate restTemplate = buildRestTemplate();
    private final GitHubAccess gitHubAccess;
    private final Logger logger = LoggerFactory.getLogger(GitHubToolbox.class.getName());

    GitHubToolbox(GitHubAccess gitHubAccess) {
        this.gitHubAccess = gitHubAccess;
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
        byte[] body = exchangeWithRetry(url, byte[].class);
        if (body == null) return JsonUtils.toJsonNode("null");
        return JsonUtils.toJsonNode(new ByteArrayInputStream(body));
    }

    String get(String url) throws IOException {
        return exchangeWithRetry(url, String.class);
    }

    /**
     * Retries once/twice on an HTTP 429 (GitHub's secondary/abuse-detection rate limit), honouring
     * the "Retry-After" header when present. Any other status/exception is rethrown immediately.
     */
    private <T> T exchangeWithRetry(String url, Class<T> responseType) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        int attempt = 0;
        while (true) {
            try {
                return restTemplate.exchange(url, HttpMethod.GET, entity, responseType).getBody();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() != 429 || attempt >= MAX_RETRIES_ON_RATE_LIMIT) {
                    throw e;
                }
                long waitSeconds = retryAfterSeconds(e);
                attempt++;
                logger.warn("GitHub secondary rate limit (429) on [{}] (authenticated={}), retrying in {}s (attempt {}/{})",
                        url, gitHubAccess.hasGithubToken(), waitSeconds, attempt, MAX_RETRIES_ON_RATE_LIMIT);
                sleep(waitSeconds);
            }
        }
    }

    private long retryAfterSeconds(HttpClientErrorException e) {
        String retryAfter = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
        if (retryAfter != null) {
            try {
                return Math.min(Long.parseLong(retryAfter.trim()), MAX_RETRY_AFTER_SECONDS);
            } catch (NumberFormatException ignored) {
                // fall through to the default below
            }
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (gitHubAccess.hasGithubToken()) {
            headers.set("Authorization", "Bearer " + gitHubAccess.getGitHubToken());
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
