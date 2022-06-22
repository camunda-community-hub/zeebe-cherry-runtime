package org.camunda.vercors.actorsfilter;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.camunda.vercors.definition.AbstractWorker;
import org.camunda.vercors.pdf.OfficeToPdfWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

@Component
public class ActorFilterUsers extends AbstractWorker {


    public static final String BPMERROR_SYNTAXE_OPERATION_ERROR = "SYNTAX_OPERATION_ERROR";
    private final static String INPUT_ACTORFILTER = "actorFilter";
    private final static String INPUT_ACTORFILTER_V_USERS = "users";
    private final static String INPUT_USER_CANDIDATE = "userCandidate";
    private final static String INPUT_ANYTHING = "*";
    private final static String OUTPUT_RESULT = "*";
    Logger logger = LoggerFactory.getLogger(OfficeToPdfWorker.class.getName());

    public ActorFilterUsers() {
        super("io.camunda.zeebe:userTask",
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_ACTORFILTER, String.class, Level.OPTIONAL, "Give the code for the Actor filter"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_USER_CANDIDATE, String.class, Level.OPTIONAL, "For the actor filter [" + INPUT_ACTORFILTER_V_USERS + "] name of candidates"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_ANYTHING, Object.class, Level.OPTIONAL, "Any variables can be accessed")
                ),
                Collections.emptyList(),
                Arrays.asList(BPMERROR_SYNTAXE_OPERATION_ERROR));
    }

    @Override
    // @ Z  e e b e W orker(type = "io.camunda.zeebe:userTask")
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    @Override
    public void execute(final JobClient jobClient, final ActivatedJob activatedJob, ContextExecution contextExecution) {
        String actorFilter = getInputStringValue(INPUT_ACTORFILTER, null, activatedJob);
        if (INPUT_ACTORFILTER_V_USERS.equals(actorFilter)) {
            assignActorFilter(jobClient, activatedJob);
        }
    }

    private void assignActorFilter(final JobClient jobClient, final ActivatedJob activatedJob) {
        String actorFilterName = getInputStringValue(INPUT_USER_CANDIDATE, null, activatedJob);
        Object candidates = getValue(actorFilterName, null, activatedJob);
        // processID is the name of the process
        String processId = activatedJob.getBpmnProcessId();
        // processInstanceKey is the process instance id
        activatedJob.getProcessInstanceKey();
        // elementId is the taskName
        String elementId = activatedJob.getElementId();
        if (candidates instanceof String) {

        }
    }
}

