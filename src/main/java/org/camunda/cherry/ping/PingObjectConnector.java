/* -------------------------------------------------------------------- */
/* PingObjectConnector                                                  */
/* This connector return an Object as the output variable               */
/* This is the same Input/execution as the PingConnector                */
/* but the result is different.                                         */
/* See PingConnectorOutput versus PingObjectConnectorOutput             */
/* -------------------------------------------------------------------- */
package org.camunda.cherry.ping;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.camunda.cherry.definition.AbstractConnector;
import org.camunda.cherry.definition.IntFrameworkRunner;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Collections;

@Component
public class PingObjectConnector extends AbstractConnector implements IntFrameworkRunner, OutboundConnectorFunction {


    protected PingObjectConnector() {
        super("c-pingobjectconnector",
                PingConnectorInput.class,
                PingObjectConnectorOutput.class,
                Collections.emptyList());
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
        return "Ping Object connector";
    }

    @Override
    public String getLabel() {
        return "Ping (Object ConnectorSDK)";
    }

    @Override
    public String getDescription() {
        return "Do a simple ping as a connector, and return object containing timestamp, ipAddress. A Delay can be set as parameter";
    }

    @Override
    public Object execute(OutboundConnectorContext context) throws Exception {

        PingConnectorInput pingConnectorInput = context.getVariablesAsType(PingConnectorInput.class);

        // context.validate(pingConnectorInput);
        Thread.sleep( pingConnectorInput.getDelay());
        InetAddress IP=InetAddress.getLocalHost();

        return new PingObjectConnectorOutput(System.currentTimeMillis(), IP.getHostAddress());
    }
}
