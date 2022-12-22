package io.camunda.cherry.ping;

import io.camunda.cherry.definition.AbstractConnectorInput;
import io.camunda.cherry.definition.RunnerParameter;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotEmpty;


public class PingConnectorInput extends AbstractConnectorInput {

    // see https://docs.camunda.io/docs/components/integration-framework/connectors/custom-built-connectors/connector-sdk/#validation

    @NotEmpty private String message;
    private int delay;

    public boolean isThrowErrorPlease() {
        return throwErrorPlease;
    }

    private boolean throwErrorPlease;

    /**
     * Return the parameters definition
     *
     * @return list of parameters
     */
    @Override
    public List<RunnerParameter> getInputParameters() {
        return Arrays.asList(
                RunnerParameter.getInstance("message",
                        "Message",
                        String.class,
                        RunnerParameter.Level.OPTIONAL,
                        "Message to log"),
                RunnerParameter.getInstance("delay",
                        "Delay",
                        Long.class,
                        RunnerParameter.Level.OPTIONAL,
                        "Delay to sleep"),
            RunnerParameter.getInstance("throwErrorPlease",
                "Throw Error Please",
                Boolean.class,
                RunnerParameter.Level.OPTIONAL,
                "If true, then the connector throw an error")
        );
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
