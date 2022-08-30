package org.camunda.cherry.ping;


import io.camunda.connector.api.ConnectorContext;
import io.camunda.connector.api.ConnectorFunction;
import org.camunda.cherry.definition.AbstractConnector;


public class PingConnector extends AbstractConnector implements ConnectorFunction {

    PingConnector() {
        super("c-pingconnector");
    }
    @Override
    public Object execute(ConnectorContext context) throws Exception {

        var request = context.getVariablesAsType(PingConnectorInput.class);

        context.validate(request);

        return new PingConnectorOutput( System.currentTimeMillis());
    }
}

