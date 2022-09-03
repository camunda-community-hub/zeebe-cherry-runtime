package org.camunda.cherry.ping;

import org.camunda.cherry.definition.AbstractConnectorOutput;
import org.camunda.cherry.definition.RunnerParameter;

import java.util.Arrays;
import java.util.List;

public class PingConnectorOutput extends AbstractConnectorOutput {

    private long timeStamp;

    PingConnectorOutput(){
        super();
    }
    PingConnectorOutput(long currentTimestamp) {
        super();
        this.timeStamp = currentTimestamp;
    }

    @Override
    public List<RunnerParameter> getOutputParameters() {
        return Arrays.asList(RunnerParameter.getInstance("timestamp", "Time stamp", String.class, RunnerParameter.Level.REQUIRED, "Produce a timestamp"));
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
