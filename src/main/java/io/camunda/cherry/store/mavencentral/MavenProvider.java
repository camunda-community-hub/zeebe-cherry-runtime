package io.camunda.cherry.store.mavencentral;

import org.springframework.http.HttpHeaders;

import java.net.HttpURLConnection;

public class MavenProvider {

    public String name;
    public String url;
    public String token;
    public String apikey;
    /**
     * Header/prefix used to send the token - not every Maven provider necessarily agrees on this.
     * Defaults to the standard "Authorization: Bearer <token>" scheme, which both of today's providers
     * use (Maven Central sends no token at all; Camunda's JFrog Artifactory accepts a JFrog Identity
     * Token this way). Override per-provider instance if a future provider expects something else
     * (e.g. Basic auth, or a differently-named header).
     */
    public String tokenHeaderName = "Authorization";
    public String tokenPrefix = "Bearer ";
    /**
     * Header used to send the API key - defaults to JFrog Artifactory's own "X-JFrog-Art-Api" scheme.
     * Override per-provider instance if a future provider uses a different API-key header.
     */
    public String apikeyHeaderName = "X-JFrog-Art-Api";

    public static MavenProvider getInstance(String name, String url) {
        return getInstance(name, url, null, null);
    }

    public static MavenProvider getInstance(String name, String url, String token, String apikey) {
        MavenProvider mavenProvider = new MavenProvider();
        mavenProvider.name = name;
        mavenProvider.url = url;
        mavenProvider.token = token;
        mavenProvider.apikey = apikey;
        return mavenProvider;
    }

    /**
     * Attaches whatever credentials this provider carries - a bearer token and/or a JFrog API key -
     * so a gated repository (e.g. Camunda's Artifactory) can be probed/downloaded the same way a
     * public one (e.g. Maven Central, with no credentials configured) is. Add more attributes here
     * (and on CherryProperties.ProviderCredential) if another provider needs a different auth scheme.
     */
    public void applyAuth(HttpURLConnection connection) {
        if (token != null && !token.isBlank())
            connection.setRequestProperty(tokenHeaderName, tokenPrefix + token);
        if (apikey != null && !apikey.isBlank())
            connection.setRequestProperty(apikeyHeaderName, apikey);
    }

    public HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isBlank())
            headers.set(tokenHeaderName, tokenPrefix + token);
        if (apikey != null && !apikey.isBlank())
            headers.set(apikeyHeaderName, apikey);
        return headers;
    }
}
