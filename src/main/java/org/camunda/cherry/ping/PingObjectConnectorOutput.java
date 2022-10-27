package org.camunda.cherry.ping;

import org.camunda.cherry.definition.AbstractConnectorOutput;

public class PingObjectConnectorOutput  {

    private Long internalTimeStampMS;
    private String internalIpAddress;



    public PingObjectConnectorOutput(long currentTimestamp, String ipAddress) {
        super();
        this.internalTimeStampMS = currentTimestamp;
        this.internalIpAddress = ipAddress;
    }


    /* Return an objet, getter can be regular */
    public long getTimeStampMS() {
        return internalTimeStampMS;
    }
    /* Return an objet, getter can be regular */
    public String getIpAddress() {
        return internalIpAddress;
    }

}
