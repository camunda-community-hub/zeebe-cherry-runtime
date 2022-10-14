package org.camunda.cherry.ping;

import org.camunda.cherry.definition.AbstractConnectorOutput;
import org.camunda.cherry.definition.RunnerParameter;

import java.util.Arrays;
import java.util.List;

public class PingConnectorOutput extends AbstractConnectorOutput {

    private long timeStampMS;

    public PingConnectorOutput() {
        super();
    }

    public PingConnectorOutput(long currentTimestamp) {
        super();
        this.timeStampMS = currentTimestamp;
    }

    @Override
    public List<RunnerParameter> getListOutput() {
        return Arrays.asList(RunnerParameter.getInstance("timeStampMS", "Time stamp", Long.class, RunnerParameter.Level.REQUIRED, "Produce a timestamp"));
    }

    public long getTimeStampMS() {
        return timeStampMS;
    }

}
