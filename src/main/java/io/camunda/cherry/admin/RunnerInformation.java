/* ******************************************************************** */
/*                                                                      */
/*  WorkerInformation                                                   */
/*                                                                      */
/*  Collect worker information from a worker.                           */
/*  This class works as a facade. It's easy then to get the JSON        */
/*  from this object                                                    */
/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWorker;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.definition.RunnerParameter;
import io.camunda.cherry.runtime.CherryHistoricFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RunnerInformation {


    private AbstractRunner runner;

    private boolean active;

    private boolean displayLogo = true;

    private CherryHistoricFactory.Statistic statistic;

    private CherryHistoricFactory.Performance performance;



    /**
     * Keep the runner in the class. This class is a facade
     *
     * @param runner
     * @return runner information for a runner
     */
    public static RunnerInformation getRunnerInformation(AbstractRunner runner) {
        RunnerInformation workerInformation = new RunnerInformation();
        workerInformation.runner = runner;
        return workerInformation;
    }

    /**
     * Get all information on runner
     */
    public String getName() {
        return runner.getIdentification();
    }

    public String getLabel() {
        return runner.getLabel();
    }

    public String getDisplayLabel() {
        return runner.getDisplayLabel();
    }

    public String getType() {
        return runner.getType();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<RunnerParameter> getListInput() {
        return runner.getListInput();
    }

    public List<RunnerParameter> getListOutput() {
        return runner.getListOutput();
    }

    public List<BpmnError> getListBpmnErrors() {
        return runner.getListBpmnErrors();
    }

    public String getClassName() {
        return runner.getClass().getName();
    }

    public String getDescription() {
        return runner.getDescription();
    }

    public String getLogo() {
        return displayLogo ? runner.getLogo() : null;
    }

    public TYPE_RUNNER getTypeRunner() {
        return runner instanceof AbstractWorker ? TYPE_RUNNER.WORKER : TYPE_RUNNER.CONNECTOR;
    }

    public void setDisplayLogo(boolean displayLogo) {
        this.displayLogo = displayLogo;
    }

    /**
     * If the runner has definition error, is will not be possible to start it
     *
     * @return the list of errors
     */
    public String getDefinitionErrors() {
        return String.join(", ", runner.getDefinitionErrors());
    }

    public enum TYPE_RUNNER {WORKER, CONNECTOR}

    public void setStatistic(CherryHistoricFactory.Statistic statistic) {
        this.statistic = statistic;
    }

    public void setPerformance(CherryHistoricFactory.Performance performance) {
        this.performance = performance;
    }

    public CherryHistoricFactory.Statistic getStatistic() {
        return statistic;
    }

    public CherryHistoricFactory.Performance getPerformance() {
        return performance;
    }
}
