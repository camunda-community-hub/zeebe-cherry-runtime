/* ******************************************************************** */
/*                                                                      */
/*  ZeebeContainer                                                      */
/*                                                                      */
/*  Save the current Zeebe Client                                       */
/* ******************************************************************** */
package io.camunda.cherry.zeebe;

import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.LogOperation;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@Configuration
@PropertySource("classpath:application.yaml")
@EnableScheduling

public class ZeebeContainer {

  Logger logger = LoggerFactory.getLogger(ZeebeContainer.class.getName());

  @Autowired
  ZeebeConfiguration zeebeConfiguration;
  @Autowired
  LogOperation logOperation;
  private ZeebeClient zeebeClient;

  /**
   * Connection correct is zeebeClient !=null && isConnected = true
   */
  private boolean isConnected = false;
  /**
   * Number of thread currently used at the Zeebe Client
   */

  /**
   * Start the ZeebeClient
   */
  public void startZeebeeClient() throws TechnicalException {
    zeebeClient = null;
    String validation = zeebeConfiguration.checkValidation();
    if (validation != null) {
      logger.error("Incorrect configuration: " + validation);
      logOperation.logError("Incorrect Zeebe configuration " + validation);
      return;
    }

    logger.info("ZeebeContainer.startZeebe {} ", zeebeConfiguration.getLogConfiguration());
    ZeebeClientBuilder zeebeClientBuilder = null;
    switch (zeebeConfiguration.getTypeConnection()) {
    // ---- CLOUD connection
    case CLOUD -> {
      ZeebeClient.newCloudClientBuilder()
          .withClusterId(zeebeConfiguration.getClusterId())
          .withClientId(zeebeConfiguration.getClientId())
          .withClientSecret(zeebeConfiguration.getClientSecret())
          .withRegion(zeebeConfiguration.getRegion());
      break;
    }

    // ---- IDENTITY connection (with OAuth)
    case IDENTITY -> {
      zeebeClientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(zeebeConfiguration.getGatewayAddress());
      if (zeebeConfiguration.isPlaintext())
        zeebeClientBuilder = zeebeClientBuilder.usePlaintext();

      // see https://docs.camunda.io/docs/apis-tools/java-client/
      // https://github.com/jwulf/zeebe-node-sm-mt-example
      final OAuthCredentialsProvider provider = new OAuthCredentialsProviderBuilder() //
          .clientId(zeebeConfiguration.getClientId())
          .clientSecret(zeebeConfiguration.getClientSecret())
          .audience(zeebeConfiguration.getAudience())
          .build();
      zeebeClientBuilder = zeebeClientBuilder.credentialsProvider(provider);
      break;
    }

    // ---- CLASSIC connection

    case DIRECTIPADDRESS -> {
      zeebeClientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(zeebeConfiguration.getGatewayAddress());
      if (zeebeConfiguration.isPlaintext())
        zeebeClientBuilder = zeebeClientBuilder.usePlaintext();
    }
    default ->
      throw new TechnicalException("Unkon connection type [" + zeebeConfiguration.getTypeConnection().toString() + "]");

    }


    // Multi tenancy?
    if (!zeebeConfiguration.getListTenantIds().isEmpty())
      zeebeClientBuilder = zeebeClientBuilder.defaultJobWorkerTenantIds(zeebeConfiguration.getListTenantIds());

    try {
      zeebeClient = zeebeClientBuilder.numJobWorkerExecutionThreads(zeebeConfiguration.getNumberOfThreads())
          .defaultJobWorkerMaxJobsActive(zeebeConfiguration.getMaxJobsActive())
          .build();
    } catch (Exception e) {
      logOperation.logError("Can't start ZeebeClient ", e);
      throw new TechnicalException("Can't start ZeebeClient", e);
    }
    isConnected = pingZeebeClient();

    logger.info("ZeebeConnected: {} ClientNumberOfThreads=[{}]", isConnected,
        zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads());
  }

  /**
   * Check if the Zeebe Server is alive
   *
   * @return true if the zeebe server is alive, else false
   */
  public boolean pingZeebeClient() {
    if (zeebeClient == null)
      return false;
    try {

      ZeebeFuture<Topology> send = zeebeClient.newTopologyRequest().send();
      Topology join = send.join();

      return true;
    } catch (Exception e) {
      logger.error("PingZeebe exception {}", e.toString());
      return false;
    }
  }

  /**
   * Stop the zeebeClient
   */
  public void stopZeebeeClient() {
    if (zeebeClient == null)
      return;
    zeebeClient.close();
    zeebeClient = null;
  }

  /**
   * Return the current ZeebeClient Attention: do not save it, it may be deleted and recreated on
   * demand
   *
   * @return the zeebeClient
   */
  public ZeebeClient getZeebeClient() {
    return zeebeClient;
  }

  public boolean isOk() {
    return zeebeClient != null;
  }

  /**
   * get the number of jobs in the
   *
   * @return the number of threads used when the ZeebeClient is started
   */
  public int getNumberOfThreads() {
    return zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads();
  }

  /**
   * @return the number of threads used when the ZeebeClient is started
   */
  public int getMaxJobsActive() {
    return zeebeClient.getConfiguration().getDefaultJobWorkerMaxJobsActive();
  }

  /**
   * Check the connection, and restart it if it is possible
   * @return true if the connection is up and running, false else
   */
  public boolean retryConnection() {

    if (getZeebeClient() == null)
      startZeebeeClient();
    return pingZeebeClient();
  }

}
