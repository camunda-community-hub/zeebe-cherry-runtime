package org.camunda.cherry.ping;


import io.camunda.connector.api.ConnectorContext;
import org.camunda.cherry.definition.AbstractConnector;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Component
public class PingConnector extends AbstractConnector {

    protected PingConnector() {
        super("c-pingconnector",
                PingConnectorInput.class,
                PingConnectorOutput.class,
                Collections.emptyList());
    }

    @Override
    public Object execute(ConnectorContext context) throws Exception {

        var request = context.getVariablesAsType(PingConnectorInput.class);

        context.validate(request);

        return new PingConnectorOutput(System.currentTimeMillis());
    }

    @Override
    public String getDescription() {
        return "Do a simple ping as a connector, and return a timestamp. A Delay can be set as parameter";
    }
}

