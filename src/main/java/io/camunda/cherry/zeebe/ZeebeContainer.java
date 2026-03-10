/* ******************************************************************** */
/*                                                                      */
/*  ZeebeContainer                                                      */
/*                                                                      */
/*  Save the current Zeebe Client                                       */
/* ******************************************************************** */
package io.camunda.cherry.zeebe;

import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.LogOperation;
import io.camunda.client.CamundaClient;
import io.camunda.client.CamundaClientConfiguration;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.response.Topology;
import io.camunda.client.jobhandling.CamundaClientExecutorService;
import io.camunda.client.spring.properties.CamundaClientProperties;
import io.camunda.connector.api.document.DocumentFactory;
import io.camunda.connector.runtime.core.document.DocumentFactoryImpl;
import io.camunda.connector.runtime.core.document.store.CamundaDocumentStore;
import io.camunda.connector.runtime.core.document.store.CamundaDocumentStoreImpl;
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

    /**
     * Connection correct is zeebeClient !=null && isConnected = true
     */
    private final boolean isConnected = false;
    private final CamundaClient camundaClient;
    private final CamundaClientProperties camundaClientProperties;
    Logger logger = LoggerFactory.getLogger(ZeebeContainer.class.getName());
    @Autowired
    LogOperation logOperation;

    public ZeebeContainer(final CamundaClient camundaClient, final CamundaClientProperties camundaClientProperties) {
        this.camundaClient = camundaClient;

        this.camundaClientProperties = camundaClientProperties;
    }

    /**
     * Return the current ZeebeClient Attention: do not save it, it may be deleted and recreated on
     * demand
     *
     * @return the zeebeClient
     */

    public CamundaClient getZeebeClient() {
        return camundaClient;
    }

    public CamundaClientConfiguration getZeebeClientConfiguration() {
        return camundaClient.getConfiguration();
    }

    public DocumentFactory getDocumentFactory() {
        CamundaDocumentStore documentStore = new CamundaDocumentStoreImpl(camundaClient);
        return new DocumentFactoryImpl(documentStore);
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
        CamundaClient localZeebe = camundaClient;

        if (localZeebe == null)
            return;
        localZeebe.close();
    }


    public void restart() {
        stopZeebeClient();
        startZeebeClient();
    }

    /**
     * Check if the Zeebe Server is alive
     *
     * @return true if the zeebe server is alive, else false
     */
    public boolean pingZeebeClient() {
        CamundaClient localZeebe = camundaClient;

        if (localZeebe == null)
            return false;
        try {

            CamundaFuture<Topology> send = localZeebe.newTopologyRequest().send();

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
        return camundaClient != null;
    }

    /**
     * get the number of jobs in the
     *
     * @return the number of threads used when the ZeebeClient is started
     */
    public int getNumberOfThreads() {

        return camundaClient.getConfiguration().getNumJobWorkerExecutionThreads();
    }

    public void setNumberOfThreads(int numberOfThreads) {
        CamundaClientExecutorService.createDefault(numberOfThreads);
        // throw new TechnicalException("Can't upgrade the number of threads");
    }

    /**
     * @return the number of threads used when the ZeebeClient is started
     */
    public int getMaxJobsActive() {
        return camundaClient.getConfiguration().getDefaultJobWorkerMaxJobsActive();
    }

    /**
     * Check the connection, and restart it if it is possible
     *
     * @return true if the connection is up and running, false else
     */
    public boolean checkConnection() {
        CamundaClient localZeebe = camundaClient;

        if (localZeebe == null)
            startZeebeClient();
        return pingZeebeClient();
    }

}
