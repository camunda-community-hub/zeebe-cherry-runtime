package org.camunda.vercors.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.camunda.vercors.definition.WorkerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
@EnableZeebeClient
public class SendMessageWorker extends WorkerBase {

    Logger logger = LoggerFactory.getLogger(SendMessageWorker.class.getName());


    public SendMessageWorker() {
        super("v-send-message",
                Arrays.asList(
                        WorkerParameter.getInstance("urlMessage", String.class, Level.REQUIRED),
                        WorkerParameter.getInstance("messageName", String.class, Level.REQUIRED),
                        WorkerParameter.getInstance("correlationVariables", String.class, Level.OPTIONAL),
                        WorkerParameter.getInstance("variables", String.class, Level.OPTIONAL)),
                Collections.emptyList());
    }

    @ZeebeWorker(type = "v-send-message",  autoComplete = true, fetchVariables={"urlMessage", "messageName","correlationVariables","variables"})
    public void handleWorkerExecution(final JobClient client, final ActivatedJob job) {
        super.handleWorkerExecution(client, job);
    }


    public void execute(final JobClient client, final ActivatedJob job) {

        try {
            String urlMessage = getInputStringValue("urlMessage", "http://localhost:8080/engine-rest/message", job);

            String messageName = getInputStringValue("messageName", null, job);

            String correlationVariableList = getInputStringValue("correlationVariables", null, job);

            String variableList = getInputStringValue("variables", null, job);

            sendMessageViaHttp( urlMessage, messageName, correlationVariableList, variableList,job);
        } catch (Exception e) {
            logger.error("SendMessage: We got an exception ! Send a BPMN Error " + e.getMessage());
        }
    }

    /**
     * Return a Map of variable value, from a list of variable. Variables are separate by a comma.
     * Example firstName,lastName
     * @param variableList list of variables, separate by a comma
     * @param activatedJob job to call to get values
     * @return map of variable Name / variable value
     */
    private Map<String, Object> extractVariable(String variableList, final ActivatedJob activatedJob) {
        Map<String,Object> variables = new HashMap<>();
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

    /**
     * @param urlMessage url message to call
     * @param messageName message name
     * @param businessKey business key (not exist in C8, will be deleted)
     * @param variableList list of variable to send
     * @param activatedJob job to call to get values
     * @throws Exception in case of error
     */
    private void sendMessageViaHttp(String urlMessage,
                                    String messageName,
                                    String businessKey,
                                    String variableList,
                                    ActivatedJob activatedJob)
            throws Exception {
        Map<String,Object> jsonMessage = new HashMap<>();
        jsonMessage.put("messageName", messageName);

        if (businessKey != null)
            jsonMessage.put("businessKey", businessKey);

        if (variableList != null) {
            Map<String,Object> variables = extractVariable(variableList, activatedJob);
            jsonMessage.put("processVariables", variables);

        }
        ObjectMapper mapper = new ObjectMapper();
        String postBody = mapper.writeValueAsString(jsonMessage);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(postBody))
                .uri(URI.create(urlMessage))
                .header("content-type", "application/json") // add request header
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());
    }
}
