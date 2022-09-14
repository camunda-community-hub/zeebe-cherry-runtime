/* ******************************************************************** */
/*                                                                      */
/*  CherryJobWorkerFactory                                                 */
/*                                                                      */
/*  Detect and start workers                                            */
/* ******************************************************************** */
package org.camunda.cherry.runtime;

import io.camunda.connector.runtime.jobworker.ConnectorJobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import org.camunda.cherry.definition.AbstractConnector;
import org.camunda.cherry.definition.AbstractRunner;
import org.camunda.cherry.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Component
public class CherryJobRunnerFactory {
    public static final String WORKER_NOT_FOUND = "WorkerNotFound";
    public static final String UNKNOWN_WORKER_CLASS = "UnknownWorkerClass";
    public static final String WORKER_INVALID_DEFINITION = "WORKER_INVALID_DEFINITION";


    Logger logger = LoggerFactory.getLogger(CherryJobRunnerFactory.class.getName());


    @Autowired
    List<AbstractConnector> listAbstractConnector;

    @Autowired
    List<AbstractWorker> listAbstractWorker;

    @Autowired
    ZeebeContainer zeebeContainer;
    List<Running> listRunnerRunning = new ArrayList<>();

    @PostConstruct
    public void startAll() {
        zeebeContainer.startZeebeeClient();
        if (!zeebeContainer.isOk()) {
            logger.error("ZeebeClient is not started, can't start runner");
            return;
        }

        List<AbstractRunner> listRunners = Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream()).toList();


        for (AbstractRunner runner : listRunners) {
            String errors = String.join(", ", runner.getDefinitionErrors());
            if (!errors.isEmpty()) {
                logger.error("Runner [" + runner.getIdentification() + "] can't start, errors " + errors);
                continue;
            }


            JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild = null;
            try {
                jobWorkerBuild = createJobWorker(runner);
            } catch (Exception e) {
                logger.error("Can't start runner " + runner.getIdentification() + " : " + e);
            }
            if (jobWorkerBuild != null) {
                logger.info("CherryJobRunnerFactory: start [" + runner.getType()
                        + (runner.getName() != null ? " (" + runner.getName() + ")" : "")
                        + "]");
                listRunnerRunning.add(new Running(runner, new ContainerJobWorker(jobWorkerBuild.open())));
            }
        }
    }

    public void stopAll() {
        for (Running running : listRunnerRunning) {
            if (running.runner != null) {
                try {
                    stopRunner(running.runner.getIdentification());
                } catch (OperationException e) {
                    logger.error("Error on worker [" + running.runner.getIdentification() + "]");

                } catch (Exception e) {
                    logger.error("Error on worker [" + running.runner.getIdentification() + "]");
                }
            }
        }
        zeebeContainer.stopZeebeeClient();

    }

    /**
     * Stop a runner
     *
     * @param runnerName name of the runner (connector/worker)
     * @return true if the runner is stopped
     */
    public boolean stopRunner(String runnerName) throws OperationException {
        for (Running running : listRunnerRunning) {
            if (running.runner().getIdentification().equals(runnerName)) {
                closeJobWorker(running.containerJobWorker.jobWorker);
                running.containerJobWorker.jobWorker = null;
                return true;
            }
        }
        throw new OperationException(WORKER_NOT_FOUND, "Worker not found");
    }

    /**
     * Start a runner
     *
     * @param runnerName name of the runner (connector/worker)
     * @return true if the runner started
     * @throws Exception
     */
    public boolean startRunner(String runnerName) throws OperationException {
        for (Running running : listRunnerRunning) {
            if (running.runner().getIdentification().equals(runnerName)) {
                if (!running.runner.isValidDefinition())
                    throw new OperationException(WORKER_INVALID_DEFINITION, "Worker has error in the definition : "
                            + String.join(";", running.runner.getDefinitionErrors()));

                closeJobWorker(running.containerJobWorker.jobWorker);
                running.containerJobWorker.jobWorker = null;
                JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild = createJobWorker(running.runner);
                running.containerJobWorker.jobWorker = jobWorkerBuild.open();
                return true;
            }
        }
        throw new OperationException(WORKER_NOT_FOUND, "Worker not found");
    }

    public boolean isRunnerActive(String runnerName) throws OperationException {
        for (Running running : listRunnerRunning) {
            if (running.runner().getIdentification().equals(runnerName)) {
                return running.containerJobWorker.jobWorker != null;
            }
        }
        throw new OperationException(WORKER_NOT_FOUND, "Worker not found");
    }


    public int getNumberOfThreads() {
        return zeebeContainer.getNumberOfhreads();
    }

    public void setNumberOfThreads(int numberOfThreadsRequired) {
        zeebeContainer.setNumberOfThreadsRequired(numberOfThreadsRequired);

        stopAll();
        startAll();
    }
    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Administration on Runner (stop,start)                   */
    /*                                                          */
    /* -------------------------------------------------------- */

    private void closeJobWorker(JobWorker jobWorker) {
        if (jobWorker == null)
            return;
        if (jobWorker.isClosed())
            return;
        jobWorker.close();
        while (!jobWorker.isClosed()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                // do nothing
            }

        }
    }

    private JobWorkerBuilderStep1.JobWorkerBuilderStep3 createJobWorker(AbstractRunner runner) throws OperationException {
        JobWorkerBuilderStep1.JobWorkerBuilderStep2 jobWorkerBuild2 = zeebeContainer.getZeebeClient().newWorker()
                .jobType(runner.getType());

        JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild3;
        if (runner instanceof AbstractWorker abstractWorker)
            jobWorkerBuild3 = jobWorkerBuild2.handler(abstractWorker);
        else if (runner instanceof AbstractConnector abstractConnector)
            jobWorkerBuild3 = jobWorkerBuild2.handler(new ConnectorJobHandler(abstractConnector));
        else
            throw new OperationException(UNKNOWN_WORKER_CLASS, "Unknown AbstractRunner class");
        jobWorkerBuild3.name(runner.getName() == null ? runner.getType() : runner.getName());

        List<String> listVariablesInput = runner.getListFetchVariables();
        if (listVariablesInput != null) {
            jobWorkerBuild3.fetchVariables(listVariablesInput);
        }
        return jobWorkerBuild3;
    }

    /**
     * Not possible to restart a jobWorker: must be created again !
     */
    private static class ContainerJobWorker {
        public JobWorker jobWorker;

        public ContainerJobWorker(JobWorker jobWorker) {
            this.jobWorker = jobWorker;
        }
    }

    record Running(AbstractRunner runner, ContainerJobWorker containerJobWorker) {
    }


    /**
     * Declare an exception on an operation
     */
    public class OperationException extends Exception {
        public String exceptionCode;
        public String explanation;

        OperationException(String exceptionCode, String explanation) {
            this.exceptionCode = exceptionCode;
            this.explanation = explanation;
        }
    }
}
