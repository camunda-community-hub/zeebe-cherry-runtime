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
import io.camunda.zeebe.client.ZeebeClientConfiguration;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.spring.client.properties.CamundaClientProperties;
import io.camunda.zeebe.spring.client.properties.common.ZeebeClientProperties;
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
  LogOperation logOperation;

  /**
   * Connection correct is zeebeClient !=null && isConnected = true
   */
  private final boolean isConnected = false;

  private final ZeebeClient zeebeClient;
  private final CamundaClientProperties camundaClientProperties;

  public ZeebeContainer(final ZeebeClient zeebeClient, final CamundaClientProperties camundaClientProperties) {
    this.zeebeClient = zeebeClient;
    this.camundaClientProperties = camundaClientProperties;
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

  public ZeebeClientConfiguration getZeebeClientConfiguration() {
    return zeebeClient.getConfiguration();
  }

  /**
   * Start the ZeebeClient
   */
  public void startZeebeClient() throws TechnicalException {
    // Do nothing, already started
  }

  /**
   * Stop the zeebeClient
   */
  public void stopZeebeClient() {
    ZeebeClient localZeebe = zeebeClient;

    if (localZeebe == null)
      return;
    localZeebe.close();
  }

  /**
   * Check if the Zeebe Server is alive
   *
   * @return true if the zeebe server is alive, else false
   */
  public boolean pingZeebeClient() {
    ZeebeClient localZeebe = zeebeClient;

    if (localZeebe == null)
      return false;
    try {

      ZeebeFuture<Topology> send = localZeebe.newTopologyRequest().send();

      // Wait the result
      send.join();

      return true;
    } catch (Exception e) {
      logger.error("PingZeebe exception {}", e.toString());
      throw new TechnicalException(e);
    }
  }

  /**
   * Note: the class io/camunda/zeebe/spring/client/configuration/ZeebeClientProdAutoConfiguration
   * already define a zeebeClient as a bean. But this class known the real zeebeclient in use.
   *
   * @return the zeebeClient
   */
  // @Bean
  // @Primary
  /* public ZeebeClient zeebeClient() {
    return zeebeClient;
  }
*/
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

  public void setNumberOfThreads(int numberOfThreads) {
    if (zeebeClient.getConfiguration() instanceof ZeebeClientProperties zeebeClientProperties) {
      zeebeClientProperties.setExecutionThreads(numberOfThreads);
    }
    throw new TechnicalException("Can't upgrade the number of threads");
  }

  /**
   * @return the number of threads used when the ZeebeClient is started
   */
  public int getMaxJobsActive() {
    return zeebeClient.getConfiguration().getDefaultJobWorkerMaxJobsActive();
  }

  /**
   * Check the connection, and restart it if it is possible
   *
   * @return true if the connection is up and running, false else
   */
  public boolean checkConnection() {
    ZeebeClient localZeebe = zeebeClient;

    if (localZeebe == null)
      startZeebeClient();
    return pingZeebeClient();
  }

}
