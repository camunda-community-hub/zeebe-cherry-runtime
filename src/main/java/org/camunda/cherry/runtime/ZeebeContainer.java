/* ******************************************************************** */
/*                                                                      */
/*  ZeebeContainer                                                      */
/*                                                                      */
/*  Save the current Zeebe Client                                       */
/* ******************************************************************** */
package org.camunda.cherry.runtime;

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
     * Start the ZeebeClient
     */
    protected void startZeebeeClient()  {
        zeebeClient=null;
        String validation = zeebeConfiguration.checkValidation();
        if (validation!=null) {
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
            zeebeClientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(zeebeConfiguration.getGatewayAddress());
        }
        zeebeClient = zeebeClientBuilder
                //         .numJobWorkerExecutionThreads(1)
                .build();
        logger.info("ZeebeClient number of thread=" + zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads());
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
}
