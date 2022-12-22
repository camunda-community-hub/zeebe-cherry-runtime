/* -------------------------------------------------------------------- */
/* PingConnector                                                        */
/* This connector return a list of output variable, not an object       */
/* This is the same Input/execution as the PingObjectConnector          */
/* but the result is different.                                         */
/* See PingConnectorOutput versus PingObjectConnectorOutput             */
/* -------------------------------------------------------------------- */
package io.camunda.cherry.ping;


import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

/* ------------------------------------------------------------------- */

@Component
public class PingConnector extends AbstractConnector implements IntFrameworkRunner, OutboundConnectorFunction {

    public static final String ERROR_NO_CONNECTION = "NO_CONNECTION";

    protected PingConnector() {
        super("c-pingconnector",
                PingConnectorInput.class,
                PingConnectorOutput.class,
                Collections.singletonList(new BpmnError(ERROR_NO_CONNECTION, "Can't realize the connection")));
    }

    /**
     * mark this Connector as a Framework runner
     *
     * @return true because this worker is part of the Cherry framework
     */
    @Override
    public boolean isFrameworkRunner() {
        return true;
    }

    @Override
    public String getName() {
        return "Ping connector";
    }

    @Override
    public String getLabel() {
        return "Ping (ConnectorSDK)";
    }

    @Override
    public String getDescription() {
        return "Do a simple ping as a connector, and return timestamp, ipAdress. A Delay can be set as parameter";
    }

    @Override
    public Object execute(OutboundConnectorContext context) throws Exception {

        PingConnectorInput pingConnectorInput = context.getVariablesAsType(PingConnectorInput.class);

        if (pingConnectorInput.isThrowErrorPlease()) {
            throw new ConnectorException(ERROR_NO_CONNECTION, "No connection to the earth");
        }
        // context.validate(pingConnectorInput);
        Thread.sleep( pingConnectorInput.getDelay());
        InetAddress ipAddress=InetAddress.getLocalHost();

        return new PingConnectorOutput(System.currentTimeMillis(),
            ipAddress.getHostAddress(),
            Map.of("JDK", System.getProperty("java.version")));
    }

}

