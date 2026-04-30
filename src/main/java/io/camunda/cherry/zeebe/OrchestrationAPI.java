package io.camunda.cherry.zeebe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.spring.properties.CamundaClientAuthProperties;
import io.camunda.client.spring.properties.CamundaClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This call is used to connect the Orchestration API
 */
@Component
public class OrchestrationAPI {
    public static final String ACCESS_TOKEN = "\"access_token\":\"";
    private final CamundaClientProperties camundaClientProperties;
    Logger logger = LoggerFactory.getLogger(OrchestrationAPI.class.getName());

    public OrchestrationAPI(CamundaClientProperties camundaClientProperties) {
        this.camundaClientProperties = camundaClientProperties;
    }

    public List<TenantInformation> getListTenants() throws RuntimeException {
        List<TenantInformation> listTenants = new ArrayList<>();

        HttpResponse<String> stringHttpResponse = callPostHttp("/v2/tenants/search", "{}");

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(stringHttpResponse.body());
            JsonNode itemsNode = root.get("items");

            for (JsonNode item : itemsNode) {
                TenantInformation tenantInformation = new TenantInformation();
                tenantInformation.name = item.get("name").asText();
                tenantInformation.tenantId = item.get("tenantId").asText();
                tenantInformation.description = item.get("description").asText();
                listTenants.add(tenantInformation);
            }
            return listTenants;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> callPostHttp(String url, String body) {

        StringBuilder logBuilder = new StringBuilder();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(camundaClientProperties.getRestAddress() + url));
            logBuilder.append("RESTAddress[").append(camundaClientProperties.getRestAddress()).append("] Url[").append(url).append("]");

            if (camundaClientProperties.getAuth() != null &&
                    CamundaClientAuthProperties.AuthMethod.oidc.equals(camundaClientProperties.getAuth().getMethod())) {
                String accessToken = getAccessToken();
                builder = builder.header("Authorization", "Bearer " + accessToken);
                logBuilder.append(" AuthMethod.oidc token[").append(accessToken).append("]");
            }
            if (camundaClientProperties.getAuth() != null &&
                    CamundaClientAuthProperties.AuthMethod.basic.equals(camundaClientProperties.getAuth().getMethod())) {
                String basic = Base64.getEncoder()
                        .encodeToString((camundaClientProperties.getAuth().getUsername() + ":" + camundaClientProperties.getAuth().getPassword()).getBytes(StandardCharsets.UTF_8));
                builder = builder.header("Authorization", "Basic " + basic);
                logBuilder.append(" AuthMethod.basic username[").append(camundaClientProperties.getAuth().getUsername()).append("]");
            }
            builder = builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            logBuilder.append("body[").append(body).append("]");

            HttpRequest request = builder.build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            logger.error("Error during post HTTP request Auth{} Url{} Log{}",
                    camundaClientProperties.getAuth() == null ? "null" : camundaClientProperties.getAuth().toString(),
                    url,
                    logBuilder,
                    e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Get the accesstoken, according the type of connection.
     * @return the access token
     * @throws Exception if any error
     */
    private String getAccessToken() throws Exception {

        String audience = camundaClientProperties.getAuth().getAudience();
        if (CamundaClientProperties.ClientMode.saas.equals(camundaClientProperties.getMode())
                && (audience==null || audience.isEmpty())) {
            audience= "zeebe.camunda.io";
        }
        String body = "grant_type=client_credentials"
                + "&client_id=" + camundaClientProperties.getAuth().getClientId()
                + "&client_secret=" + camundaClientProperties.getAuth().getClientSecret()
                + "&audience=" + audience;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(camundaClientProperties.getAuth().getTokenUrl())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();
            if (json.isEmpty())
                throw new Exception("Can't get access token: body is empty.");
            if (! json.contains(ACCESS_TOKEN))
                throw new Exception("Can't get access token: " + json);
            // extract access_token (quick & dirty)
            return json.split(ACCESS_TOKEN)[1].split("\"")[0];
        } catch (Exception e) {
            logger.error("Can't get access token: " + e.getMessage());
            throw new Exception(e);
        }
    }

    /**
     * Returns the number of jobs currently waiting (state=CREATED) for the given job type.
     * Uses POST /v2/jobs/search — state-based, not time-windowed, so it captures
     * jobs regardless of when the process instance started.
     */
    public long getJobCount(String jobType) {
        String body = "{\"filter\":{\"type\":\"" + jobType + "\",\"state\":\"CREATED\"},\"page\":{\"limit\":1}}";
        try {
            HttpResponse<String> response = callPostHttp("/v2/jobs/search", body);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode totalItems = root.path("page").path("totalItems");
            if (totalItems.isMissingNode()) {
                logger.warn("getJobCount: no page.totalItems in response for jobType[{}]", jobType);
                return 0;
            }
            return totalItems.asLong();
        } catch (JsonProcessingException e) {
            logger.error("getJobCount: failed to parse response for jobType[{}]: {}", jobType, e.getMessage());
            return 0;
        }
    }

    public HttpResponse<String> callGetHttp(String url) {
        StringBuilder logBuilder = new StringBuilder();
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(camundaClientProperties.getRestAddress() + url));
            logBuilder.append("RESTAddress[").append(camundaClientProperties.getRestAddress()).append("] Url[").append(url).append("]");

            if (camundaClientProperties.getAuth() != null &&
                    CamundaClientAuthProperties.AuthMethod.oidc.equals(camundaClientProperties.getAuth().getMethod())) {
                String accessToken = getAccessToken();
                builder = builder.header("Authorization", "Bearer " + accessToken);
            }
            if (camundaClientProperties.getAuth() != null &&
                    CamundaClientAuthProperties.AuthMethod.basic.equals(camundaClientProperties.getAuth().getMethod())) {
                String basic = Base64.getEncoder()
                        .encodeToString((camundaClientProperties.getAuth().getUsername() + ":" + camundaClientProperties.getAuth().getPassword()).getBytes(StandardCharsets.UTF_8));
                builder = builder.header("Authorization", "Basic " + basic);
            }
            builder = builder.GET();

            HttpRequest request = builder.build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            logger.error("Error during GET HTTP request Url{} Log{}", url, logBuilder, e);
            throw new RuntimeException(e);
        }
    }

    public static class TenantInformation {
        public String tenantId;
        public String name;
        public String description;
        public boolean active = false;
    }


}
