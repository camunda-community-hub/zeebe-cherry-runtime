/* ******************************************************************** */
/*                                                                      */
/*  AbstractConnectorInput                                                     */
/*                                                                      */
/*  Use this abstract class when you to define a connector Input        */
/*  and give information on input parameters. This information is used  */
/*  in the administrative page.                                         */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import io.camunda.connector.api.ConnectorInput;

import java.util.Collections;
import java.util.List;

public class AbstractConnectorInput implements ConnectorInput {

    /**
     * Return the list of Parameters used by the connector
     *
     * @return
     */
    public List<RunnerParameter> getInputParameters() {
        return Collections.emptyList();
    }

}
