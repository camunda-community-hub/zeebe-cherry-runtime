/* ******************************************************************** */
/*                                                                      */
/*  WorkerInformation                                                   */
/*                                                                      */
/*  Collect worker information from a worker.                           */
/*  This class works as a facade. It's easy then to get the JSON        */
/*  from this object                                                    */
/* ******************************************************************** */
package org.camunda.cherry.admin;

import org.camunda.cherry.definition.AbstractRunner;
import org.camunda.cherry.definition.BpmnError;
import org.camunda.cherry.definition.RunnerParameter;

import java.util.List;

public class RunnerInformation {


    private AbstractRunner runner;

    /**
     * Keep the worker in the class. This class is a facade
     *
     * @param worker
     * @return
     */
    public static RunnerInformation getWorkerInformation(AbstractRunner worker) {
        RunnerInformation workerInformation = new RunnerInformation();
        workerInformation.runner = worker;
        return workerInformation;
    }

    /**
     * Get all information on runner
     */
    public String getName() {
        return runner.getName();
    }

    public boolean isActive() {
        return true;
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
        return runner.getLogo();
    }

}
