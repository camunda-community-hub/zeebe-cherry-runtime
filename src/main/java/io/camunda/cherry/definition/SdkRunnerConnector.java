package io.camunda.cherry.definition;

import io.camunda.connector.api.outbound.OutboundConnectorFunction;

import java.util.Collections;

public class SdkRunnerConnector extends AbstractRunner {

  private final OutboundConnectorFunction outboundConnectorFunction;
  private String name;

  public SdkRunnerConnector(OutboundConnectorFunction outboundConnectorFunction) {

    super("", // String type,
        Collections.emptyList(), //  listInput,
        Collections.emptyList(), //  listOutput,
        Collections.emptyList()); // listBpmnErrors);
    this.outboundConnectorFunction = outboundConnectorFunction;
  }

  public OutboundConnectorFunction getTransportedConnector() {
    return outboundConnectorFunction;
  }

  /**
   * The type is known after, in the annotation for example
   *
   * @param type type to set
   */
  @Override
  public void setType(String type) {
    super.setType(type);
  }

  /**
   * Return the name
   *
   * @return name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * the name has to be provided after.
   *
   * @param name name to save
   */
  public void setName(String name) {
    this.name = name;
  }

  public boolean isWorker() {
    return false;
  }

  public boolean isConnector() {
    return true;
  }
}
