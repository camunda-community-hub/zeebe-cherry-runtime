/* ******************************************************************** */
/*                                                                      */
/*  WorkerRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runner/list                */
/*  http://localhost:8080/cherry/api/runner/c-files-load-from-disk/stop */
/* ******************************************************************** */
package org.camunda.cherry.admin;

import org.camunda.cherry.definition.AbstractRunner;
import org.camunda.cherry.runtime.CherryJobRunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("cherry")
public class RunnerRestController {


    Logger logger = LoggerFactory.getLogger(org.camunda.cherry.admin.RunnerRestController.class.getName());
    @Autowired
    CherryJobRunnerFactory cherryJobRunnerFactory;
    /**
     * Spring populate the list of all workers
     */
    @Autowired
    private List<AbstractRunner> listRunner;

    @GetMapping(value = "/api/runner/list", produces = "application/json")
    public List<RunnerInformation> getWorkersList() {
        return listRunner.stream()
                .map(RunnerInformation::getWorkerInformation)
                .map(this::completeRunnerInformation)
                .toList();
    }

    @GetMapping(value = "/api/runner/detail", produces = "application/json")
    public Optional<RunnerInformation> getWorker(@RequestParam(name = "runnerName") String runnerName) {
        return listRunner.stream()
                .filter(worker -> worker.getIdentification().equals(runnerName))
                .map(RunnerInformation::getWorkerInformation)
                .map(this::completeRunnerInformation)
                .findFirst();
    }

    /**
     * Ask to stop a specific worker
     *
     * @param runnerName worker to stop
     * @return NOTFOUND or the worker information on this worker
     */
    @PutMapping(value = "/api/runner/{runnerName}/stop", produces = "application/json")
    public RunnerInformation stopWorker(@PathVariable String runnerName) {
        logger.info("Stop requested for [" + runnerName + "]");
        try {
            boolean isStopped = cherryJobRunnerFactory.stopRunner(runnerName);
            logger.info("Stop executed for [" + runnerName + "]: " + isStopped);
            AbstractRunner runner = getRunnerByName(runnerName);
            RunnerInformation runnerInfo = RunnerInformation.getWorkerInformation(runner);
            return completeRunnerInformation(runnerInfo);
        } catch (CherryJobRunnerFactory.OperationException e) {
            if (e.exceptionCode.equals(CherryJobRunnerFactory.WORKER_NOT_FOUND))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "WorkerName [" + runnerName + "] error " + e);
        }
    }

    /**
     * Ask to start a specific worker
     *
     * @param runnerName worker to start
     * @return NOTFOUND or the worker information on this worker
     */
    @PutMapping(value = "/api/runner/{runnerName}/start", produces = "application/json")
    public RunnerInformation startWorker(@PathVariable String runnerName) {
        logger.info("Start requested for [" + runnerName + "]");
        try {
            boolean isStarted = cherryJobRunnerFactory.startRunner(runnerName);
            logger.info("Start executed for [" + runnerName + "]: " + isStarted);
            AbstractRunner runner = getRunnerByName(runnerName);
            RunnerInformation runnerInfo = RunnerInformation.getWorkerInformation(runner);
            return completeRunnerInformation(runnerInfo);
        } catch (CherryJobRunnerFactory.OperationException e) {
            if (e.exceptionCode.equals(CherryJobRunnerFactory.WORKER_NOT_FOUND))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "WorkerName [" + runnerName + "] error " + e);
        }
    }

    /**
     * Download the Template for a runner
     *
     * @param runnerName worker to start
     * @return NOTFOUND or the worker information on this worker
     */
    @PutMapping(value = "/api/runner/{runnerName}/template", produces = "application/json")
    public String downloadTemplate(@PathVariable String runnerName) {
        logger.info("Download template requested for [" + runnerName + "]");
        AbstractRunner runner = getRunnerByName(runnerName);
        if (runner == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
        String templateContentJson = runner.getTemplate();
        return templateContentJson;
    }

    private AbstractRunner getRunnerByName(String runnerName) {
        List<AbstractRunner> listFiltered = listRunner.stream().filter(w -> w.getIdentification().equals(runnerName)).toList();
        if (listFiltered.size() != 1)
            return null;
        return listFiltered.get(0);

    }

    private RunnerInformation completeRunnerInformation(RunnerInformation runnerInformation) {
        try {
            runnerInformation.setActive(cherryJobRunnerFactory.isRunnerActive(runnerInformation.getName()));
        } catch (CherryJobRunnerFactory.OperationException e) {
            // definitively not expected
        }
        return runnerInformation;
    }
}
