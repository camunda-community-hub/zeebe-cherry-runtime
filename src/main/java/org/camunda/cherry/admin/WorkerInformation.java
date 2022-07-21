/* ******************************************************************** */
/*                                                                      */
/*  WorkerInformation                                                   */
/*                                                                      */
/*  Collect worker information from a worker.                           */
/*  This class works as a facade. It's easy then to get the JSON        */
/*  from this object                                                    */
/* ******************************************************************** */
package org.camunda.cherry.admin;

import org.camunda.cherry.definition.AbstractWorker;

import java.util.List;

public class WorkerInformation {


    private AbstractWorker worker;

    /**
     * Keep the worker in the class. This class is a facade
     * @param worker
     * @return
     */
    public static WorkerInformation getWorkerInformation(AbstractWorker worker) {
        WorkerInformation workerInformation = new WorkerInformation();
        workerInformation.worker = worker;
        return workerInformation;
    }

    /**
     * Get all information on workers
     */
    public String getName() {
        return worker.getName();
    }

    public boolean isActive() {
        return true;
    }
    public int getNbThreads() {
        return 1;
    }

    public List<AbstractWorker.WorkerParameter> getListInput() { return worker.getListInput();}
    public List<AbstractWorker.WorkerParameter> getListOutput() { return worker.getListOutput();}
    public List<String> getListBpmnErrors() { return worker.getListBpmnErrors();}

    public String getClassName() {
        return worker.getClass().getName();
    }
}
