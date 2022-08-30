package org.camunda.cherry.ping;

public class PingConnectorOutput {

    private long timeStamp;


    PingConnectorOutput(long currentTimestamp) {
        this.timeStamp = currentTimestamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
