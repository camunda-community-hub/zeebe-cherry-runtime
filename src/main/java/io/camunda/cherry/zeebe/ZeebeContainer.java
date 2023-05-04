/* ******************************************************************** */
/*                                                                      */
/*  ZeebeContainer                                                      */
/*                                                                      */
/*  Save the current Zeebe Client                                       */
/* ******************************************************************** */
package io.camunda.cherry.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.response.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@Component
@Configuration
@PropertySource("classpath:application.yaml")

public class ZeebeContainer {

  Logger logger = LoggerFactory.getLogger(ZeebeContainer.class.getName());

  @Autowired
  ZeebeConfiguration zeebeConfiguration;

  private ZeebeClient zeebeClient;

  /**
   * Number of thread currently used at the Zeebe Client
   */


  /**
   * Start the ZeebeClient
   */
  public void startZeebeeClient() {
    zeebeClient = null;
    String validation = zeebeConfiguration.checkValidation();
    if (validation != null) {
      logger.error("Incorrect configuration: " + validation);
      return;
    }

    ZeebeClientBuilder zeebeClientBuilder;
    if (zeebeConfiguration.isCouldConfiguration()) {
      zeebeClientBuilder = ZeebeClient.newCloudClientBuilder()
          .withClusterId(zeebeConfiguration.clusterId)
          .withClientId(zeebeConfiguration.clientId)
          .withClientSecret(zeebeConfiguration.clientSecret)
          .withRegion(zeebeConfiguration.region);

    } else {
      zeebeClientBuilder = ZeebeClient.newClientBuilder()
          .gatewayAddress(zeebeConfiguration.getGatewayAddress())
          .usePlaintext();
    }
    zeebeClient = zeebeClientBuilder.numJobWorkerExecutionThreads(zeebeConfiguration.getNumberOfThreads()).build();

    pingZeebeClient();

    logger.info("ZeebeClient number of threads=" + zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads());
  }

  /**
   * Check if the Zeebe Server is alive
   *
   * @return true if the zeebe server is alive, else false
   */
  public boolean pingZeebeClient() {
    try {
      ZeebeFuture<Topology> send = zeebeClient.newTopologyRequest().send();
      Topology join = send.join();

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Stop the zeebeClient
   */
  public void stopZeebeeClient() {
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
   * @return the number of threads used when the ZeebeClient is started
   */
  public int getNumberOfThreads() {
    return zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads();
  }

  /**
   *
   * @return the number of threads used when the ZeebeClient is started
   */
  public int getMaxJobsActive() {
    return zeebeClient.getConfiguration().getDefaultJobWorkerMaxJobsActive();
  }


}
