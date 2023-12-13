package io.camunda.cherry.zeebe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@Component
@PropertySource("classpath:application.yaml")
public class ZeebeConfiguration {

  @Value("${zeebe.client.broker.gateway-address:localhost:26500}")
  @Nullable
  private String gateway;

  @Value("${zeebe.client.security.plaintext:}")
  @Nullable
  private String plaintext;

  @Value("${zeebe.client.cloud.region:}")
  @Nullable
  private String region;

  @Value("${zeebe.client.cloud.clusterId:}")
  @Nullable
  private String clusterId;

  @Value("${zeebe.client.oauth.clientId:}")
  @Nullable
  private String clientId;

  @Value("${zeebe.client.oauth.clientSecret:}")
  @Nullable
  private String clientSecret;

  @Value("${zeebe.client.oauth.authorizationServerUrl:}")
  @Nullable
  private String authorizationServerUrl;


  /**
   * Not possible to use audience:
   * connectorcore embeded io.camunda.zeebe.spring.client.properties.OperateClientConfigurationProperties
   * This class does not have a setter for audience
   * SpringBoot refuse to start
   */
  @Value("${zeebe.client.oauth.clientAudience:}")
  @Nullable
  private String audience;

  @Value("${zeebe.client.worker.threads:1}")
  private int numberOfThreads;

  @Value("${zeebe.client.worker.maxJobsActive:1}")
  private int maxJobsActive;




  /* ******************************************************************** */
  /*                                                                      */
  /*  getter and setter                                                   */
  /*                                                                      */
  /*  Information come from the configuration or from the database        */
  /* ******************************************************************** */

  public void init() {
    // We load configuration from the database
    read();
  }

  public boolean isCloudConfiguration() {
    return clusterId != null && !clusterId.trim().isEmpty();
  }

  public boolean isOAuthConfiguration() {
    return clientId != null && !clientId.trim().isEmpty();
  }

  public String getGatewayAddress() {
    return gateway;
  }

  @Nullable
  public String getGateway() {
    return gateway;
  }

  public void setGateway(@Nullable String gateway) {
    this.gateway = gateway;
  }

  @Nullable
  public boolean isPlaintext() {
    return plaintext==null? true: "true".equals( plaintext);
  }

  public void setPlaintext(@Nullable String plaintext) {
    this.plaintext = plaintext;
  }

  @Nullable
  public String getRegion() {
    return region;
  }

  public void setRegion(@Nullable String region) {
    this.region = region;
  }

  @Nullable
  public String getClusterId() {
    return clusterId;
  }

  public void setClusterId(@Nullable String clusterId) {
    this.clusterId = clusterId;
  }

  @Nullable
  public String getClientId() {
    return clientId;
  }

  public void setClientId(@Nullable String clientId) {
    this.clientId = clientId;
  }

  @Nullable
  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(@Nullable String clientSecret) {
    this.clientSecret = clientSecret;
  }



  @Nullable
  public String getAuthorizationServerUrl() {
    return authorizationServerUrl;
  }

  public void setAuthorizationServerUrl(@Nullable String authorizationServerUrl) {
    this.authorizationServerUrl = authorizationServerUrl;
  }


  @Nullable
  public String getAudience() {
    return audience;
  }
  public void setAudience(@Nullable String audience) {
    this.audience = audience;
  }


  public int getNumberOfThreads() {
    return numberOfThreads;
  }

  public void setNumberOfThreads(int numberOfThreads) {
    this.numberOfThreads = numberOfThreads;
  }

  public int getMaxJobsActive() {
    return maxJobsActive;
  }

  public void setMaxJobsActive(int maxJobsActive) {
    this.maxJobsActive = maxJobsActive;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Read/Write in the database                                          */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Detect if something change
   *
   * @return true if the configuration change in the database
   */
  public boolean read() {
    return false;
  }

  public void write() {

  }

  /**
   * Check the configuration. If it is not complete, return false. Attention, this method does not
   * check if the connection works, just it is valid or not
   *
   * @return null if all is correct, else an explanation
   */
  public String checkValidation() {
    if (isCloudConfiguration()) {
      StringBuilder check = new StringBuilder();
      if (empty(region))
        check.append("Missing region;");
      if (empty(clusterId))
        check.append("Missing clusterId;");
      if (empty(clientId))
        check.append("Missing clientId;");
      if (empty(clientSecret))
        check.append("Missing clientSecret;");
      return check.toString().isEmpty() ? null : check.toString();
    }
    // for a local, the gateway may be empty, we'll provide a default value
    return null;
  }

  public String getLogConfiguration() {

    StringBuilder logConfiguration = new StringBuilder();
    logConfiguration.append("Cloud? ");
    logConfiguration.append(isCloudConfiguration());
    if (isCloudConfiguration()) {
      logConfiguration.append(" ClusterId[");
      logConfiguration.append(getClusterId());
      logConfiguration.append("] Region[");
      logConfiguration.append(getRegion());
      logConfiguration.append("]");
    } else {
      logConfiguration.append(" OAuth? ");
      logConfiguration.append(isOAuthConfiguration());

      logConfiguration.append(" Gateway[");
      logConfiguration.append(getGateway());
      logConfiguration.append("] usePlainText[");
      logConfiguration.append(isPlaintext());
      logConfiguration.append("]");
    }

    if (isOAuthConfiguration()) {
      logConfiguration.append(" ClientID[");
      logConfiguration.append(getClientId());
      logConfiguration.append("] ClientSecret[");
      String clientSecret = getClientSecret();
      logConfiguration.append(clientSecret == null ? "null" : (clientSecret + "****").substring(0, 3) + "****");
      logConfiguration.append("] Audience[");
      logConfiguration.append(getAudience());
      logConfiguration.append("] authorizationServerUrl[");
      logConfiguration.append(getAuthorizationServerUrl());
      logConfiguration.append("]");

    }

    return logConfiguration.toString();

  }

  /**
   * Return true if the value is null or empty
   *
   * @param value value to verify
   * @return true if the value is empty
   */
  private boolean empty(String value) {
    return value == null || value.trim().isEmpty();
  }
}
