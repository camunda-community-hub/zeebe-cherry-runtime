package org.camunda.cherry.ping;

import io.camunda.connector.api.ConnectorInput;
import io.camunda.connector.api.Validator;

public class PingConnectorInput implements ConnectorInput {

    private String message;
    private int delay;

    public String getMessage() {
        return message;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
