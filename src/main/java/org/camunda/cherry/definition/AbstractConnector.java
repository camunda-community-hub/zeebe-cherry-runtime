package org.camunda.cherry.definition;

import org.camunda.cherry.runtime.ConnectorInformation;

public class AbstractConnector {

    private final String type;
    protected AbstractConnector(String type) {
        this.type = type;
    }

    public ConnectorInformation getInformation() {
        ConnectorInformation connectorInformation = new ConnectorInformation();
        connectorInformation.type = type;
        return connectorInformation;
    }
}
