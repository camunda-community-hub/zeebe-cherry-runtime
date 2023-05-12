/* -------------------------------------------------------------------- */
/* PingConnector                                                        */
/* This connector return a list of output variable, not an object       */
/* This is the same Input/execution as the PingObjectConnector          */
/* but the result is different.                                         */
/* See PingConnectorOutput versus PingObjectConnectorOutput             */
/* -------------------------------------------------------------------- */
package io.camunda.cherry.embeddedrunner.ping.connector;

import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

/* ------------------------------------------------------------------- */

@Component
@OutboundConnector(name = PingConnector.TYPE_PINGCONNECTOR, inputVariables = { PingConnectorInput.INPUT_MESSAGE,
    PingConnectorInput.INPUT_DELAY,
    PingConnectorInput.INPUT_THROWERRORPLEASE }, type = PingConnector.TYPE_PINGCONNECTOR)
public class PingConnector extends AbstractConnector implements IntFrameworkRunner, OutboundConnectorFunction {

  public static final String ERROR_BAD_WEATHER = "BAD_WEATHER";
  public static final String TYPE_PINGCONNECTOR = "c-pingconnector";

  private final Random random = new Random();

  public PingConnector() {
    super(TYPE_PINGCONNECTOR, PingConnectorInput.class, PingConnectorOutput.class,
        Collections.singletonList(new BpmnError(ERROR_BAD_WEATHER, "Why this is a bad weather?")));
  }

  /**
   * mark this Connector as a Framework runner
   *
   * @return true because this worker is part of the Cherry framework
   */
  @Override
  public boolean isFrameworkRunner() {
    return true;
  }

  @Override
  public String getName() {
    return "Ping connector";
  }

  @Override
  public String getLabel() {
    return "Ping (ConnectorSDK)";
  }

  @Override
  public String getDescription() {
    return "Do a simple ping as a connector, and return timestamp, ipAdress. A Delay can be set as parameter";
  }

  @Override
  public Object execute(OutboundConnectorContext context) throws Exception {

    PingConnectorInput pingConnectorInput = context.getVariablesAsType(PingConnectorInput.class);
    context.replaceSecrets(pingConnectorInput);

    if (pingConnectorInput.isThrowErrorPlease()) {
      throw new ConnectorException(ERROR_BAD_WEATHER, "Raining too much");
    }
    // context.validate(pingConnectorInput);
    int delay = pingConnectorInput.getDelay();
    if (delay < 0) {
      delay = random.nextInt(30000) + 1500;
    }

    Thread.sleep(delay);
    InetAddress ipAddress = InetAddress.getLocalHost();

    return new PingConnectorOutput(System.currentTimeMillis(), ipAddress.getHostAddress(),
        Map.of("JDK", System.getProperty("java.version")));
  }
}
