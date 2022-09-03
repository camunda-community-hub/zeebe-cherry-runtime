/* ******************************************************************** */
/*                                                                      */
/*  AbstractConnectorOutput                                                     */
/*                                                                      */
/*  Use this abstract class when you to define a connector Output       */
/*  and give information on parameters. This information is used        */
/*  in the administrative page.                                         */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import java.util.Collections;
import java.util.List;

public class AbstractConnectorOutput {

    /**
     * Give a default constructor
     * A empty constructor must be provide to let the Cherry Framework to create an instance to be able to call getOutputParameters
     */
    public AbstractConnectorOutput() {
    }
    /**
     * Return the list of Parameters used by the connector
     * @return list of parameters
     */
    public List<RunnerParameter> getOutputParameters() {
        return Collections.emptyList();
    }

}