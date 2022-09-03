package org.camunda.cherry.ping;

import org.camunda.cherry.definition.AbstractConnectorInput;
import org.camunda.cherry.definition.RunnerParameter;

import java.util.Arrays;
import java.util.List;

public class PingConnectorInput extends AbstractConnectorInput {
    private String message;
    private int delay;


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
                        "Delay to sleep")
        );
    }


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
