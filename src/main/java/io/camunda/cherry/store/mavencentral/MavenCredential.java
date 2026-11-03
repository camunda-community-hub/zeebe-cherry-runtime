package io.camunda.cherry.store.mavencentral;

public class MavenCredential {

    public String name;
    public String url;
    public String token;

    public static MavenCredential getInstance(String name, String url) {
        MavenCredential mavenCredential = new MavenCredential();
        mavenCredential.name = name;
        mavenCredential.url = url;
        return mavenCredential;
    }
}
