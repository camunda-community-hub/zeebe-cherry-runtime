package io.camunda.cherry.definition.connector;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.Collections;
import java.util.List;

public class SdkRunnerConnector extends AbstractRunner {

  private final OutboundConnectorFunction outboundConnectorFunction;
  private String nameInCache;

  public SdkRunnerConnector(OutboundConnectorFunction outboundConnectorFunction) {

    super("", // String type
        Collections.emptyList(), //  listInput
        Collections.emptyList(), //  listOutput
        Collections.emptyList()); // listBpmnErrors
    this.outboundConnectorFunction = outboundConnectorFunction;
    this.setType(getType());
  }

  public OutboundConnectorFunction getTransportedConnector() {
    return outboundConnectorFunction;
  }

  /**
   * Get the type from the annotation
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

  @Override
  public List<RunnerParameter> getListInput() {
    OutboundConnector connectorAnnotation = outboundConnectorFunction.getClass().getAnnotation(OutboundConnector.class);
    List<String> listInputString = List.of(connectorAnnotation.inputVariables());
    return listInputString.stream().map(t -> {
      return RunnerParameter.getInstance(t, // name
          t, // label
          String.class, null, // default Value
          RunnerParameter.Level.OPTIONAL, "");
    }).toList();

  }

  /**
   * For the ID, we return the name of the class, not the RunnerConnector
   *
   * @return the ID of the runner
   */
  @Override
  public String getId() {
    return getTransportedConnector().getClass().getName();
  }

  public boolean isWorker() {
    return false;
  }

  public boolean isConnector() {
    return true;
  }

  public String toString() {
    if (nameInCache == null)
      nameInCache = getName();
    return nameInCache;
  }
}
