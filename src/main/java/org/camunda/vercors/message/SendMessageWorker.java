/* ******************************************************************** */
/*                                                                      */
/*  SendMessageWorker                                                   */
/*                                                                      */
/*  Send a Camunda BPMN Message                                         */
/* ******************************************************************** */
package org.camunda.vercors.message;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.vercors.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class SendMessageWorker extends AbstractWorker {

    public static final String INPUT_MESSAGE_NAME = "messageName";
    public static final String INPUT_CORRELATION_VARIABLES = "correlationVariables";
    public static final String INPUT_MESSAGE_VARIABLES = "messageVariables";
    public static final String INPUT_MESSAGE_ID_VARIABLES = "messageId";
    public static final String INPUT_MESSAGE_DURATION = "messageDuration";
    public static final String WORKERTYPE_SEND_MESSAGE = "v-send-message";

    Logger logger = LoggerFactory.getLogger(SendMessageWorker.class.getName());

    @Autowired
    private ZeebeClient zeebeClient;


    public SendMessageWorker() {
        super(WORKERTYPE_SEND_MESSAGE,
                Arrays.asList(
                        WorkerParameter.getInstance(INPUT_MESSAGE_NAME, String.class, Level.REQUIRED),
                        WorkerParameter.getInstance(INPUT_CORRELATION_VARIABLES, String.class, Level.OPTIONAL),
                        WorkerParameter.getInstance(INPUT_MESSAGE_VARIABLES, String.class, Level.OPTIONAL),
                        WorkerParameter.getInstance(INPUT_MESSAGE_ID_VARIABLES, String.class, Level.OPTIONAL),
                        WorkerParameter.getInstance(INPUT_MESSAGE_DURATION, Object.class, Level.OPTIONAL)),
                Collections.emptyList());
    }

    // , fetchVariables={"urlMessage", "messageName","correlationVariables","variables"}
    @ZeebeWorker(type = WORKERTYPE_SEND_MESSAGE, autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    public void execute(final JobClient jobClient, final ActivatedJob activatedJob) {
        logger.info("VercorsSendMessage: start");
        try {
            sendMessageViaGrpc(getInputStringValue(INPUT_MESSAGE_NAME, null, activatedJob),
                    getInputStringValue(INPUT_CORRELATION_VARIABLES, null, activatedJob),
                    getInputStringValue(INPUT_MESSAGE_VARIABLES, null, activatedJob),
                    getInputStringValue(INPUT_MESSAGE_ID_VARIABLES, null, activatedJob),
                    getInputDurationValue(INPUT_MESSAGE_DURATION, null, activatedJob),
                    activatedJob);


        } catch (Exception e) {
            logger.error("SendMessage: We got an exception! Send a BPMN Error " + e.getMessage());
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
            logger.error("VercorsConnector[" + getName() + "] One variable expected for the correction");
            throw new ZeebeBpmnError("TOO_MANY_CORRELATION_VARIABLE_ERROR", "Worker [" + getName() + "] One variable expected for the correction:[" + correlationVariablesList + "]");
        }
        String correlationValue = null;
        if (!correlationVariables.isEmpty())
            correlationValue = correlationVariables.values().stream()
                    .findFirst()
                    .toString();
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
        if (variableList ==null)
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
