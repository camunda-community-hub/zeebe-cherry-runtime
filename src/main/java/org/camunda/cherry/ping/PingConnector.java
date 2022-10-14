package org.camunda.cherry.ping;


import io.camunda.connector.api.outbound.OutboundConnectorContext;
import org.camunda.cherry.definition.AbstractConnector;
import org.camunda.cherry.definition.IntFrameworkRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Component
public class PingConnector extends AbstractConnector implements IntFrameworkRunner {

    protected PingConnector() {
        super("c-pingconnector",
                PingConnectorInput.class,
                PingConnectorOutput.class,
                Collections.emptyList());
    }

    /**
     * mark this Connector as a Framework runner
     *
     * @return
     */
    @Override
    public boolean isFrameworkRunner() {
        return true;
    }

    @Override
    public String getName() {
        return "Cherry:Ping connector";
    }

    @Override
    public String getLabel() {
        return "Cherry: Ping using the ConnectorSDK pattern";
    }

    @Override
    public String getDescription() {
        return "Do a simple ping as a connector, and return a timestamp. A Delay can be set as parameter";
    }

    @Override
    public Object execute(OutboundConnectorContext context) throws Exception {

        var pingConnectorInput = context.getVariablesAsType(PingConnectorInput.class);

        // context.validate(pingConnectorInput);

        return new PingConnectorOutput(System.currentTimeMillis());
    }


    @Override
    public String getResponseVariable() {
        return "response";
    }
}

