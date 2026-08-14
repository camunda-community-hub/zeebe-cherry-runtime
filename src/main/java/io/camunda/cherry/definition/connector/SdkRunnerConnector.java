package io.camunda.cherry.definition.connector;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.zeebe.ZeebeContainer;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.outbound.OutboundConnectorProvider;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.Collections;
import java.util.List;

public class SdkRunnerConnector extends AbstractRunner {

    private final OutboundConnectorFunction outboundConnectorFunction;
    private final OutboundConnectorProvider outboundConnectorProvider;
    private String nameInCache;

    public SdkRunnerConnector(OutboundConnectorFunction outboundConnectorFunction, ZeebeContainer zeebeContainer) {
        super("", // String type
                Collections.emptyList(), //  listInput
                Collections.emptyList(), //  listOutput
                Collections.emptyList(), // listBpmnErrors
                zeebeContainer);
        this.outboundConnectorFunction = outboundConnectorFunction;
        this.outboundConnectorProvider = null;
        this.setType(getType());
    }

    public SdkRunnerConnector(OutboundConnectorProvider outboundConnectorProvider, ZeebeContainer zeebeContainer) {
        super("", // String type
                Collections.emptyList(), //  listInput
                Collections.emptyList(), //  listOutput
                Collections.emptyList(), // listBpmnErrors
                zeebeContainer);
        this.outboundConnectorFunction = null;
        this.outboundConnectorProvider = outboundConnectorProvider;
        this.setType(getType());
    }

    public OutboundConnectorFunction getTransportedConnectorFunction() {
        return outboundConnectorFunction;
    }

    public OutboundConnectorProvider getTransportedConnectorProvider() {
        return outboundConnectorProvider;
    }

    /**
     * Get the type from the annotation
     */
    @Override
    public String getType() {
        OutboundConnector connectorAnnotation = getAnnotation();
        if (connectorAnnotation != null) {
            return connectorAnnotation.type();
        }
        return null;
    }

    /**
     * Return the name
     *
     * @return name
     */
    @Override
    public String getName() {
        OutboundConnector connectorAnnotation = getAnnotation();
        return connectorAnnotation.name();
    }

    @Override
    public String getCollectionName() {
        String className = getTransportedClassName();
        int secondDot = className == null ? -1 : className.indexOf('.', className.indexOf('.') + 1);
        return secondDot < 0 ? "" : className.substring(0, secondDot);
    }

    @Override
    public List<RunnerParameter> getListInput() {
        OutboundConnector connectorAnnotation = getAnnotation();
        List<String> listInputString = List.of(connectorAnnotation.inputVariables());
        return listInputString.stream().map(t -> {
            return RunnerParameter.getInstance(t, // name
                    t, // label
                    String.class, null, // default Value
                    RunnerParameter.Level.OPTIONAL, "");
        }).toList();

    }


    public String getTransportedClassName() {
        if (outboundConnectorFunction != null)
            return outboundConnectorFunction.getClass().getCanonicalName();
        if (outboundConnectorProvider != null)
            return outboundConnectorProvider.getClass().getCanonicalName();
        return null;
    }

    private OutboundConnector getAnnotation() {
        if (outboundConnectorFunction != null) {
            return outboundConnectorFunction.getClass().getAnnotation(OutboundConnector.class);
        }
        if (outboundConnectorProvider != null) {
            return outboundConnectorProvider.getClass().getAnnotation(OutboundConnector.class);
        }
        return null;
    }

    /**
     * For the ID, we return the name of the class, not the RunnerConnector
     *
     * @return the ID of the runner
     */
    @Override
    public String getId() {
        if (outboundConnectorFunction != null)
            return outboundConnectorFunction.getClass().getName();
        else if (outboundConnectorProvider != null)
            return outboundConnectorProvider.getClass().getName();
        return null;
    }

    public boolean isWorker() {
        return false;
    }

    public boolean isConnector() {
        return true;
    }

    public String toString() {
        if (nameInCache == null)
            nameInCache = getName();
        return nameInCache;
    }
}
