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
  public String gateway;

  @Value("${zeebe.client.security.plaintext:true}")
  @Nullable
  public String plaintext;

  @Value("${zeebe.client.cloud.region:}")
  @Nullable
  public String region;

  @Value("${zeebe.client.cloud.clusterId:}")
  @Nullable
  public String clusterId;

  @Value("${zeebe.client.cloud.clientId:}")
  @Nullable
  public String clientId;

  @Value("${zeebe.client.cloud.clientSecret:}")
  @Nullable
  public String clientSecret;

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

  public boolean isCouldConfiguration() {
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
  public String getPlaintext() {
    return plaintext;
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
   * @return true if the configuration change in the database
   */
  public boolean read(){
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
    if (isCouldConfiguration()) {
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
