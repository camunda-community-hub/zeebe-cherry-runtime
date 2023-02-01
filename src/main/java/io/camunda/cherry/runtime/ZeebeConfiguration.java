package io.camunda.cherry.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@Component
@PropertySource("classpath:application.properties2")
public class ZeebeConfiguration {

  @Value("${zeebe.client.broker.gateway-address:localhost:26500}")
  @Nullable
  public String gateway;

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

  public boolean isCouldConfiguration() {
    return clientId != null && !clientId.trim().isEmpty();
  }

  public String getGatewayAddress() {
    return gateway;
  }

  /**
   * Check the configuration. If it is not complete, return false.
   * Attention, this method does not check if the connection works, just it is valid or not
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
