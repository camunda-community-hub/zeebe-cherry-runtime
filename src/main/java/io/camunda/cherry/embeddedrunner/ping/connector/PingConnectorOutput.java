/* -------------------------------------------------------------------- */
/* PingConnectorOutput                                                  */
/* We want to return not an object, but a list of parameter (timeStamp, */
/* ipAddress)                                                           */
/* To force the template to generate one output per parameter:          */
/*  - declare a getListOutput()                                         */
/*  - add a list of get<parameter>() method.                            */
/*  ATTENTION: the get method must start by a LOWER CASE. Example       */
/*    gettimeStampMS(), getipAddress()                                  */
/*  if you don't do that, then the engine can't retrieve the value      */
/* -------------------------------------------------------------------- */
package io.camunda.cherry.embeddedrunner.ping.connector;

import io.camunda.cherry.definition.AbstractConnectorOutput;
import io.camunda.cherry.definition.RunnerParameter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PingConnectorOutput extends AbstractConnectorOutput {

  private long internalTimeStampMS;
  private String internalIpAddress;

  private Map<String, Object> parameters;

  public PingConnectorOutput() {
    super();
  }

  public PingConnectorOutput(long currentTimestamp, String ipAddress, Map<String, Object> parameters) {
    super();
    this.internalTimeStampMS = currentTimestamp;
    this.internalIpAddress = ipAddress;
    this.parameters = parameters;
  }

  /*
  We want to return a serie of output values (not an object). So, we give here the list of parameters, and the generator will
  create one output field per parameters.
  BUT here is the trick: the Zeebe Engine will need to map expression with each output. So, the Feel Expression will search
  for each output, a method getmyField : the first letter must be in lower case.

  To return an object (and not a serie of field), see the objectConnector
   */
  @Override
  public List<RunnerParameter> getOutputParameters() {
    return Arrays.asList(
        RunnerParameter.getInstance("timeStampMS", "Time stamp", Long.class, RunnerParameter.Level.REQUIRED,
            "Produce a timestamp"),
        RunnerParameter.getInstance("ipAddress", "Ip Address", String.class, RunnerParameter.Level.REQUIRED,
            "Returm the IpAddress"),
        RunnerParameter.getInstance("parameters", "Parameters", Map.class, RunnerParameter.Level.REQUIRED,
            "Returm parameters"));
  }

  /* The getter must start by a lower case */
  public long gettimeStampMS() {
    return internalTimeStampMS;
  }

  /* The getter must start by a lower case */
  public String getipAddress() {
    return internalIpAddress;
  }

  public Map<String, Object> getparameters() {
    return parameters;
  }
}
