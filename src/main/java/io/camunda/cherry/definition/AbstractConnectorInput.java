/* ******************************************************************** */
/*                                                                      */
/*  AbstractConnectorInput                                                     */
/*                                                                      */
/*  Use this abstract class when you to define a connector Input        */
/*  and give information on input parameters. This information is used  */
/*  in the administrative page.                                         */
/* ******************************************************************** */
package io.camunda.cherry.definition;

import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.Collections;
import java.util.List;

public class AbstractConnectorInput {

  /**
   * Return the list of Parameters used by the connector
   *
   * @return list of InputParameters
   */
  public List<RunnerParameter> getInputParameters() {
    return Collections.emptyList();
  }

  protected InputParametersInfo getInputParametersInfo() {
    return new InputParametersInfo(Collections.emptyList(), null);
  }

  /**
   * Create the list and give a different class. If the Cherry input connector is created from a
   * basic connector, give the Input connector. The Cherry will be able to verify the list against
   * the Input: all fields are declared? All RunnerParameters exists as a member in the class?
   */
  protected record InputParametersInfo(List<RunnerParameter> listRunners, Class<?> inputClass) {
  }
}
