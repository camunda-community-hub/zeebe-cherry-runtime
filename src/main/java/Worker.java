import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobWorker;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Example application that connects to a cluster on Camunda Cloud, or a locally deployed cluster.
 *
 * <p>When connecting to a cluster in Camunda Cloud, this application assumes that the following
 * environment variables are set:
 *
 * <ul>
 *   <li>ZEEBE_ADDRESS
 *   <li>ZEEBE_CLIENT_ID (implicitly required by {@code ZeebeClient} if authorization is enabled)
 *   <li>ZEEBE_CLIENT_SECRET (implicitly required by {@code ZeebeClient} if authorization is enabled)
 *   <li>ZEEBE_AUTHORIZATION_SERVER_URL (implicitly required by {@code ZeebeClient} if authorization is enabled)
 * </ul>
 *
 * <p><strong>Hint:</strong> When you create client credentials in Camunda Cloud you have the option
 * to download a file with above lines filled out for you.
 *
 * <p>When connecting to a local cluster, you only need to set {@code ZEEBE_ADDRESS}.
 * This application also assumes that authentication is disabled for a locally deployed clusterL
 */
public class Worker {

    private static final String JOB_TYPE = "greet";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting worker...");

        final String zeebeAddress = getEnvironmentVariable("ZEEBE_ADDRESS");

        System.out.println("Connecting to " + zeebeAddress);
        ZeebeClient client = createZeebeClient(zeebeAddress);

        System.out.println("Registering worker for jobType:" + JOB_TYPE);
        final JobWorker jobWorker = client.newWorker().jobType(JOB_TYPE).handler(new WorkerJobHandler()).open();

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {

                    System.out.println("Closing worker for jobType:" + JOB_TYPE);
                    jobWorker.close();

                    System.out.println("Closing client connected to " + zeebeAddress);
                    client.close();

                    System.out.println("Worker Shutdown Complete");
                    countDownLatch.countDown();
                })
        );

        countDownLatch.await();
    }

    private static ZeebeClient createZeebeClient(String gatewayAddress) {
        if (gatewayAddress.contains("zeebe.camunda.io")) {
            checkEnvVars("ZEEBE_CLIENT_ID", "ZEEBE_CLIENT_SECRET", "ZEEBE_AUTHORIZATION_SERVER_URL");
            /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
             * See JavaDoc on class level for details
             */
            return ZeebeClient.newClientBuilder().gatewayAddress(gatewayAddress).build();
        } else {
            // connect to local deployment; assumes that authentication is disabled
            return ZeebeClient.newClientBuilder().gatewayAddress(gatewayAddress).usePlaintext().build();
        }
    }

    private static String getEnvironmentVariable(final String key) {
        checkEnvVars(key);

        final Map<String, String> envVars = System.getenv();

        return envVars.get(key);
    }

    private static void checkEnvVars(String... keys) {
        final Map<String, String> envVars = System.getenv();

        for (String key : keys) {
            if (!envVars.containsKey(key)) {
                throw new IllegalStateException("Unable to find mandatory environment variable " + key);
            }
        }
    }
}
