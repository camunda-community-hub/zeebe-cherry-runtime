package io.camunda.cherry.definition.connector;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.connector.api.annotation.OutboundConnector;
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
   * Get the type from the annotation
   *
   */
  @Override
  public String getType() {
    OutboundConnector connectorAnnotation = outboundConnectorFunction.getClass().getAnnotation(OutboundConnector.class);
    return connectorAnnotation.type();
  }

  /**
   * Return the name
   *
   * @return name
   */
  @Override
  public String getName() {
    OutboundConnector connectorAnnotation = outboundConnectorFunction.getClass().getAnnotation(OutboundConnector.class);
    return connectorAnnotation.name();
  }

  public boolean isWorker() {
    return false;
  }

  public boolean isConnector() {
    return true;
  }

  public String toString() {
    return name;
  }
}
