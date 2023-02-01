package io.camunda.cherry.definition;

import java.util.HashMap;
import java.util.Map;

public class WatcherOrderInformation {
  public String processId;

  public AbstractWatcher.WatcherAction orderAction = AbstractWatcher.WatcherAction.CREATEPROCESSINSTANCEPERID;
  public String taskName;
  public String taskId;

  public Map<String, Object> variables = new HashMap<>();
  /**
   * Any object the watcher want to transport
   */
  public Object transport;
  /**
   * Label is visible to administrator, to understand and get tracking
   */
  private String label;

  public void setVariable(String name, Object value) {
    variables.put(name, value);
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Object getTransport() {
    return transport;
  }

  public void setTransport(Object transport) {
    this.transport = transport;
  }
}
