/* ******************************************************************** */
/*                                                                      */
/*  SendMessageWorker                                                   */
/*                                                                      */
/*  Send a Camunda BPMN Message                                         */
/* ******************************************************************** */
package org.camunda.cherry.message;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.cherry.definition.AbstractWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class SendMessageWorker extends AbstractWorker {

    /**
     * Worker type
     */
    private static final String WORKERTYPE_SEND_MESSAGE = "c-send-message";

    private static final String INPUT_MESSAGE_NAME = "messageName";
    private static final String INPUT_CORRELATION_VARIABLES = "correlationVariables";
    private static final String INPUT_MESSAGE_VARIABLES = "messageVariables";
    private static final String INPUT_MESSAGE_ID_VARIABLES = "messageId";
    private static final String INPUT_MESSAGE_DURATION = "messageDuration";

    private static final String BPMNERROR_TOO_MANY_CORRELATION_VARIABLE_ERROR = "TOO_MANY_CORRELATION_VARIABLE_ERROR";

    @Autowired
    private ZeebeClient zeebeClient;


    public SendMessageWorker() {
        super(WORKERTYPE_SEND_MESSAGE,
                Arrays.asList(
                        WorkerParameter.getInstance(INPUT_MESSAGE_NAME, "Message name", String.class, Level.REQUIRED, "Message name"),
                        WorkerParameter.getInstance(INPUT_CORRELATION_VARIABLES, "Correlation variables", String.class, Level.OPTIONAL, "Correlation variables. The content of theses variable is used to find the process instance to unfroze"),
                        WorkerParameter.getInstance(INPUT_MESSAGE_VARIABLES, "Message variables", String.class, Level.OPTIONAL, "Variables to copy in the message"),
                        WorkerParameter.getInstance(INPUT_MESSAGE_ID_VARIABLES, "ID message", String.class, Level.OPTIONAL, "Id of the message"),
                        WorkerParameter.getInstance(INPUT_MESSAGE_DURATION, "Duratino (in ms)", Object.class, Level.OPTIONAL, "Message duration. After this delay, message is deleted if it doesn't fit a process instance")),
                Collections.emptyList(),
                Arrays.asList(AbstractWorker.BpmnError.getInstance(BPMNERROR_TOO_MANY_CORRELATION_VARIABLE_ERROR, "Correlation error")));
    }

    // , fetchVariables={"urlMessage", "messageName","correlationVariables","variables"}
    @Override
    @ZeebeWorker(type = WORKERTYPE_SEND_MESSAGE, autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    @Override
    public void execute(final JobClient jobClient, final ActivatedJob activatedJob, ContextExecution contextExecution) {

        String messageName = getInputStringValue(INPUT_MESSAGE_NAME, null, activatedJob);
        try {
            sendMessageViaGrpc(messageName,
                    getInputStringValue(INPUT_CORRELATION_VARIABLES, null, activatedJob),
                    getInputStringValue(INPUT_MESSAGE_VARIABLES, null, activatedJob),
                    getInputStringValue(INPUT_MESSAGE_ID_VARIABLES, null, activatedJob),
                    getInputDurationValue(INPUT_MESSAGE_DURATION, null, activatedJob),
                    activatedJob);


        } catch (Exception e) {
            logError("Error during sendMessage [" + messageName + "] :" + e);
            throw e;
        }
    }


    /**
     * Send a message
     * https://docs.camunda.io/docs/apis-clients/grpc/#publishmessage-rpc
     *
     * @param messageName              the message name
     * @param correlationVariablesList List of variable where the value has to be fetched to get the correlation key of the message. Attention: only one variable is expected
     * @param messageVariableList      the message variables send to the message
     * @param messageId                the unique ID of the message; can be omitted. only useful to ensure only one message with the given ID will ever be published (during its lifetime)
     * @param timeToLiveDuration       how long the message should be buffered on the broker
     * @param activatedJob             information on job to execute
     */
    private void sendMessageViaGrpc(String messageName,
                                    String correlationVariablesList,
                                    String messageVariableList,
                                    String messageId,
                                    Duration timeToLiveDuration,
                                    final ActivatedJob activatedJob) {
        Map<String, Object> correlationVariables = extractVariable(correlationVariablesList, activatedJob);
        Map<String, Object> messageVariables = extractVariable(messageVariableList, activatedJob);

        // At this moment, we expect only one variable for the correlation key
        if (correlationVariables.size() > 1) {
            logError("One (and only one) variable is expected for the correction");
            throw new ZeebeBpmnError(BPMNERROR_TOO_MANY_CORRELATION_VARIABLE_ERROR, "Worker [" + getName() + "] One variable expected for the correction:[" + correlationVariablesList + "]");
        }
        String correlationValue = null;
        if (!correlationVariables.isEmpty()) {
            Map.Entry<String, Object> entry = correlationVariables.entrySet().iterator().next();
            correlationValue= entry.getValue()==null? null : entry.getValue().toString();
        }
        PublishMessageCommandStep1.PublishMessageCommandStep3 messageCommand = zeebeClient
                .newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey(correlationValue == null ? "" : correlationValue);
        if (timeToLiveDuration != null)
            messageCommand = messageCommand.timeToLive(timeToLiveDuration);
        if (!messageVariables.isEmpty()) {
            messageCommand = messageCommand.variables(messageVariables);
        }
        if (messageId != null)
            messageCommand = messageCommand.messageId(messageId);

        messageCommand.send().join();
    }

    /**
     * Return a Map of variable value, from a list of variable. Variables are separate by a comma.
     * Example firstName,lastName
     *
     * @param variableList list of variables, separate by a comma
     * @param activatedJob job to call to get values
     * @return map of variable Name / variable value
     */
    private Map<String, Object> extractVariable(String variableList, final ActivatedJob activatedJob) {
        Map<String, Object> variables = new HashMap<>();

        if (variableList == null)
            return Collections.emptyMap();
        StringTokenizer stVariable = new StringTokenizer(variableList, ",");
        while (stVariable.hasMoreTokens()) {
            StringTokenizer stOneVariable = new StringTokenizer(stVariable.nextToken(), "=");
            String name = (stOneVariable.nextToken());
            String value = (stOneVariable.hasMoreTokens() ? stOneVariable.nextToken() : null);
            if (value == null) {
                variables.put(name, getValue(name, null, activatedJob));
            } else
                variables.put(name, getValue(name, value, activatedJob));
        }
        return variables;
    }


}
