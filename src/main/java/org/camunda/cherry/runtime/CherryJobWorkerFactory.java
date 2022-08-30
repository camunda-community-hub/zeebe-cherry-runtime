/* ******************************************************************** */
/*                                                                      */
/*  CherryJobWorkerFactory                                                 */
/*                                                                      */
/*  Detect and start workers                                            */
/* ******************************************************************** */
package org.camunda.cherry.runtime;

import io.camunda.connector.api.ConnectorFunction;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import org.camunda.cherry.definition.AbstractConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class CherryJobWorkerFactory {

    @Autowired
    List<ConnectorFunction> listConnectors;

    @Autowired
    ZeebeClient zeebeClient;

    @PostConstruct
    public void startAll() {
        // Detect all connectors
        //
        /*
         if (envVarAddress != null) {
            clientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(envVarAddress);
        } else {
            clientBuilder = ZeebeClient.newClientBuilder().gatewayAddress("localhost:26500").usePlaintext();
        }

         */
        var zeebeClient = ZeebeClient.newClientBuilder().build();

        List<JobWorker> listWorkers = new ArrayList<>();
        for (ConnectorFunction connector: listConnectors) {
            ConnectorInformation connectorInformation;
            if (connector instanceof AbstractConnector) {
                connectorInformation =((AbstractConnector) connector).getInformation();
            }
            else {
                connectorInformation = buildInformationFromConnector( connector );
            }

            zeebeClient.newWorker()
                    .jobType( connectorInformation.type)
                    .handler(new ConnectorJobHandler(connector))
                    .name("SLACK")
                    .fetchVariables("foo", "bar")
                    .open());
        }
    }
    // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}

private ConnectorInformation buildInformationFromConnector( ConnectorFunction connectorFunction) {
    return null;
}
}
