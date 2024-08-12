package io.camunda.cherry.embeddedrunner.ping.connector;

import io.camunda.cherry.definition.AbstractConnectorInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;

public class PingConnectorInput extends AbstractConnectorInput {

  // see
  // https://docs.camunda.io/docs/components/integration-framework/connectors/custom-built-connectors/connector-sdk/#validation

  @NotEmpty
  protected final static String INPUT_MESSAGE = "message";
  protected final static String INPUT_DELAY = "delay";
  protected final static String INPUT_THROWERRORPLEASE = "throwErrorPlease";
  // must be the same as the constant
  private String message;
  private int delay;
  private boolean throwErrorPlease;

  public boolean isThrowErrorPlease() {
    return throwErrorPlease;
  }

  /**
   * Return the parameters definition
   *
   * @return list of parameters
   */
  @Override
  public List<RunnerParameter> getInputParameters() {
    return Arrays.asList(
        RunnerParameter.getInstance(INPUT_MESSAGE, "Message", String.class, RunnerParameter.Level.OPTIONAL,
            "Message to log"),
        RunnerParameter.getInstance(INPUT_DELAY, "Delay", Long.class, RunnerParameter.Level.OPTIONAL, "Delay to sleep"),
        RunnerParameter.getInstance(INPUT_THROWERRORPLEASE, "Throw ControllerPage Please", Boolean.class,
            RunnerParameter.Level.OPTIONAL, "If true, then the connector throw an error").setVisibleInTemplate());
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }
}
