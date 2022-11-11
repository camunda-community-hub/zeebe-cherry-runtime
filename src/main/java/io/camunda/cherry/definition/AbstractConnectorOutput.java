/* ******************************************************************** */
/*                                                                      */
/*  AbstractConnectorOutput                                                     */
/*                                                                      */
/*  Use this abstract class when you to define a connector Output       */
/*  and give information on parameters. This information is used        */
/*  in the administrative page.                                         */
/* ******************************************************************** */
package io.camunda.cherry.definition;

import java.util.Collections;
import java.util.List;

public class AbstractConnectorOutput {

    /**
     * ATTENTION: it is very important to let one public constructor with no parameters to let Cherry to instantiate an object, to get the listOfParameters
     */

    /**
     * Give a default constructor
     * A empty constructor must be provided to let the Cherry Framework to create an instance to be able to call getOutputParameters
     */
    public AbstractConnectorOutput() {
    }

    /**
     * Return the list of Parameters used by the connector
     *
     * @return list of parameters
     */
    public List<RunnerParameter> getListOutput() {
        return Collections.emptyList();
    }

}