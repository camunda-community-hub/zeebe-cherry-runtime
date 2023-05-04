package io.camunda.cherry.embeddedrunner.ping.objectconnector;

public class PingObjectConnectorOutput {

  private final Long internalTimeStampMS;
  private final String internalIpAddress;

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
