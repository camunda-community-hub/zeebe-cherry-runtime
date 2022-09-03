/* ******************************************************************** */
/*                                                                      */
/*  Abstract Connector                                                     */
/*                                                                      */
/*  To be manage by Cherry, a worker must extend this class            */
/*  It contains the basic information required by Cherry                */
/*   - define the type, and Input/Output/Errors                         */
/*   - be able to give description, logo, name                          */
/*  and the contract implementation on parameters                       */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import io.camunda.connector.api.ConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractConnector extends AbstractRunner implements ConnectorFunction {
    Logger logger = LoggerFactory.getLogger(AbstractConnector.class.getName());

    protected AbstractConnector(String type,
                                Class<?> connectorInputClass,
                                Class<?> connectorOutputClass,
                                List<BpmnError> listBpmnErrors) {
        super(type, Collections.emptyList(),
                Collections.emptyList(),
                listBpmnErrors);
        // Create class ConnectorInput/ConnectorOutput to get parameters
        try {
            Object inputClass =  connectorInputClass.getConstructors()[0].newInstance();
            if (inputClass instanceof AbstractConnectorInput abstractConnectorInput) {
                setListInput(abstractConnectorInput.getInputParameters());
            } else {
                logger.error("AbstractConnector: connectorInputClass must extends AbstractConnectorInput");
            }

        } catch (Exception e) {
            logger.error("AbstractConnector: can't create ConnectorInput to get listOfParameters " + e);
        }
        try {
            for (Constructor constructor : connectorOutputClass.getConstructors()) {
                if (constructor.getParameterCount()==0) {
                    Object outputClass = constructor.newInstance();
                    if (outputClass instanceof AbstractConnectorOutput abstractConnectorOutput) {
                        setListOutput( abstractConnectorOutput.getOutputParameters());
                    } else {
                        logger.error("AbstractConnector: connectorOutputClass must extends AbstractConnectorOutput");
                    }
                }
            }

        } catch (Exception e) {
            logger.error("AbstractConnector: can't create ConnectorOutput to get list OfParameters" + e);
        }

    }


}
