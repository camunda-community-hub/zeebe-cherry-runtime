package io.camunda.cherry.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class collect the Watchers in the configuration, to start it
 */
@Component
@ConfigurationProperties(prefix = "watchers")
public class WatcherPropertyList {
  private List<Map<String, Object>> execution;

  public List<Map<String, Object>> getExecution() {
    return execution == null ? Collections.emptyList() : execution;
  }

  public void setExecution(List<Map<String, Object>> execution) {
    this.execution = execution;
  }
}
