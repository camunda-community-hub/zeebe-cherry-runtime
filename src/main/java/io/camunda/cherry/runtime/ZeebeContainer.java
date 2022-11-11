/* ******************************************************************** */
/*                                                                      */
/*  ZeebeContainer                                                      */
/*                                                                      */
/*  Save the current Zeebe Client                                       */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration

public class ZeebeContainer {

    Logger logger = LoggerFactory.getLogger(ZeebeContainer.class.getName());


    @Autowired
    ZeebeConfiguration zeebeConfiguration;


    private ZeebeClient zeebeClient;

    /**
     * Number of thread currently used at the Zeebe Client
     */
    private int numberOfThreads = 1;

    /**
     * Number of thread required when the zeebe client will restart
     */
    private int numberOfThreadsRequired = 1;

    /**
     * Start the ZeebeClient
     */
    protected void startZeebeeClient() {
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
            zeebeClientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(zeebeConfiguration.getGatewayAddress()).usePlaintext();
        }
        zeebeClient = zeebeClientBuilder
                .numJobWorkerExecutionThreads(numberOfThreadsRequired)
                .build();
        numberOfThreads = numberOfThreadsRequired;
        logger.info("ZeebeClient number of thread=" + zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads());
    }

    /**
     * Stop the zeebeClient
     */
    public void stopZeebeeClient() {
        zeebeClient.close();
        zeebeClient = null;
    }

    /**
     * Return the current ZeebeClient
     * Attention: do not save it, it may be deleted and recreated on demand
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
     * Protected because the main interface for this information is the CherryJobRunningFactory
     *
     * @return the number of threads used when the ZeebeClient is started
     */
    protected int getNumberOfhreads() {
        return numberOfThreads;
    }

    /**
     * Protected because the main interface for this information is the CherryJobRunningFactory
     * Setting this information does not recreate a zeebeclient. All runners must be stop before.
     *
     * @param numberOfThreads number of threads used at the next startup
     */
    protected void setNumberOfThreadsRequired(int numberOfThreads) {
        this.numberOfThreadsRequired = numberOfThreads;
    }
}
