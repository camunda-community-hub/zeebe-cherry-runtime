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
import java.util.stream.Collectors;
import java.util.stream.Stream;

// https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

@Component
public class CherryJobRunnerFactory {
    Logger logger = LoggerFactory.getLogger(CherryJobRunnerFactory.class.getName());


    @Autowired
    List<AbstractConnector> listAbstractConnector;

    @Autowired
    List<AbstractWorker> listAbstractWorker;

    @Autowired
    ZeebeContainer zeebeContainer;
    List<Running> listJobRunning = new ArrayList<>();

    @PostConstruct
    public void startAll() {
        zeebeContainer.startZeebeeClient();
        if (!zeebeContainer.isOk()) {
            logger.error("ZeebeClient is not started, can't start runner");
            return;
        }

        List<AbstractRunner> listRunners = Stream.concat(listAbstractConnector.stream(), listAbstractWorker.stream()).toList();

        for (AbstractRunner runner : listRunners) {
            JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild = null;
            try {
                jobWorkerBuild = createJobWorker(runner);
            } catch (Exception e) {
                logger.error("Can't start runner " + runner.getName() + " : " + e);
            }
            if (jobWorkerBuild !=null) {
                logger.info("CherryJobRunnerFactory: start [" + runner.getType()
                        + (runner.getName() != null ? " (" + runner.getName() + ")" : "")
                        + "]");
                listJobRunning.add(new Running(runner, new ContainerJobWorker(jobWorkerBuild.open())));
            }
        }

    }

    /**
     * Stop a runner
     *
     * @param runnerName name of the runner (connector/worker)
     * @return true if the runner is stopped
     */
    public boolean stopRunner(String runnerName) {
        for (Running running : listJobRunning) {
            if (running.runner().getName().equals(runnerName)) {
                closeJobWorker(running.containerJobWorker.jobWorker);
                return true;
            }
        }
        return false;
    }

    /**
     * Start a runner
     *
     * @param runnerName name of the runner (connector/worker)
     * @return true if the runner started
     * @throws Exception
     */
    public boolean startRunner(String runnerName) throws Exception {
        for (Running running : listJobRunning) {
            if (running.runner().getName().equals(runnerName)) {
                closeJobWorker(running.containerJobWorker.jobWorker);

                JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild = createJobWorker(running.runner);
                running.containerJobWorker.jobWorker = jobWorkerBuild.open();
                return true;
            }
        }
        return false;
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

    private JobWorkerBuilderStep1.JobWorkerBuilderStep3 createJobWorker(AbstractRunner runner) throws Exception {
        JobWorkerBuilderStep1.JobWorkerBuilderStep2 jobWorkerBuild2 = zeebeContainer.getZeebeClient().newWorker()
                .jobType(runner.getType());

        JobWorkerBuilderStep1.JobWorkerBuilderStep3 jobWorkerBuild3;
        if (runner instanceof AbstractWorker abstractWorker)
            jobWorkerBuild3 = jobWorkerBuild2.handler(abstractWorker);
        else if (runner instanceof AbstractConnector abstractConnector)
            jobWorkerBuild3 = jobWorkerBuild2.handler(new ConnectorJobHandler(abstractConnector));
        else
            throw new Exception("Unknown AbstractRunner class");
        jobWorkerBuild3.name(runner.getName() == null ? runner.getType() : runner.getName());

        List<String> listVariablesInput = runner.getListFetchVariables();
        if (listVariablesInput != null)
            jobWorkerBuild3.fetchVariables(listVariablesInput);
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
}
