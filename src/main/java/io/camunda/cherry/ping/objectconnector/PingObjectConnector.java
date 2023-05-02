/* -------------------------------------------------------------------- */
/* PingObjectConnector                                                  */
/* This connector return an Object as the output variable               */
/* This is the same Input/execution as the PingConnector                */
/* but the result is different.                                         */
/* See PingConnectorOutput versus PingObjectConnectorOutput             */
/* -------------------------------------------------------------------- */
package io.camunda.cherry.ping.objectconnector;

import io.camunda.cherry.definition.AbstractConnector;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.cherry.ping.connector.PingConnector;
import io.camunda.cherry.ping.connector.PingConnectorInput;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Random;

@Component
@OutboundConnector(name = PingConnector.TYPE_PINGCONNECTOR, inputVariables = { "message", "delay",
    "throwErrorPlease" }, type = PingConnector.TYPE_PINGCONNECTOR)
public class PingObjectConnector extends AbstractConnector implements IntFrameworkRunner, OutboundConnectorFunction {

  public static final String ERROR_BAD_WEATHER = "BAD_WEATHER";

  private final Random random = new Random();

  public PingObjectConnector() {
    super("c-pingobjectconnector", PingConnectorInput.class, PingObjectConnectorOutput.class,
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
    return "Ping Object connector";
  }

  @Override
  public String getLabel() {
    return "Ping (Object ConnectorSDK)";
  }

  @Override
  public String getDescription() {
    return "Do a simple ping as a connector, and return object containing timestamp, ipAddress. A Delay can be set as parameter";
  }

  @Override
  public Object execute(OutboundConnectorContext context) throws Exception {

    PingConnectorInput pingConnectorInput = context.getVariablesAsType(PingConnectorInput.class);
    context.replaceSecrets(pingConnectorInput);

    if (pingConnectorInput.isThrowErrorPlease())
      throw new ConnectorException(ERROR_BAD_WEATHER, "Raining too much");

    // context.validate(pingConnectorInput);
    int delay = pingConnectorInput.getDelay();
    if (delay < 0) {
      delay = random.nextInt(10000) + 1500;
    }
    Thread.sleep(delay);
    InetAddress IP = InetAddress.getLocalHost();

    return new PingObjectConnectorOutput(System.currentTimeMillis(), IP.getHostAddress());
  }
}
