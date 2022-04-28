import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;

import java.util.Collections;

public class WorkerJobHandler implements JobHandler {

    @Override
    public void handle(JobClient client, ActivatedJob job) {
        final String greeting = job.getCustomHeaders().getOrDefault("greeting", "Hello");
        final String name = (String) job.getVariablesAsMap().getOrDefault("name", "Zeebe user");

        final String message = String.format("%s %s!", greeting, name);

        client.newCompleteCommand(job.getKey()).variables(Collections.singletonMap("message", message)).send().join();
    }
}
