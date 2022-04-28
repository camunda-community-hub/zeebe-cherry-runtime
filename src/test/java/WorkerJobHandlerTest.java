import org.camunda.community.zeebe.testutils.stubs.ActivatedJobStub;
import org.camunda.community.zeebe.testutils.stubs.JobClientStub;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.data.MapEntry.entry;
import static org.camunda.community.zeebe.testutils.ZeebeWorkerAssertions.assertThat;

class WorkerJobHandlerTest {

    private final WorkerJobHandler sutJubHandler = new WorkerJobHandler();

    @Test
    public void testDefaultBehavior() {
        // given
        final JobClientStub jobClient = new JobClientStub();
        final ActivatedJobStub activatedJob = jobClient.createActivatedJob();

        // when
        sutJubHandler.handle(jobClient, activatedJob);

        // then
        assertThat(activatedJob).completed().extractingOutput().containsOnly(entry("message", "Hello Zeebe user!"));
    }

    @Test
    public void testMessageGeneration() {
        // given
        final JobClientStub jobClient = new JobClientStub();
        final ActivatedJobStub activatedJob = jobClient.createActivatedJob();

        activatedJob.setCustomHeaders(Collections.singletonMap("greeting", "Howdy"));
        activatedJob.setInputVariables(Collections.singletonMap("name", "ladies and gentlemen"));

        // when
        sutJubHandler.handle(jobClient, activatedJob);

        // then
        assertThat(activatedJob).completed().extractingOutput().containsOnly(entry("message", "Howdy ladies and gentlemen!"));
    }
}