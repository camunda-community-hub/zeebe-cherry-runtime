/* ******************************************************************** */
/*                                                                      */
/*  AdminRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/worker/list                */
/*  http://localhost:8080/cherry/api/worker/c-files-load-from-disk/stop */
/* ******************************************************************** */
package org.camunda.cherry.admin;

import org.camunda.cherry.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class AdminRestController {

    Logger logger = LoggerFactory.getLogger(AdminRestController.class.getName());


    /**
     * Spring populate the list of all workers
     */
    @Autowired
    private List<AbstractWorker> listWorkers;

    @GetMapping(value = "/api/worker/list", produces = "application/json")
    public List<WorkerInformation> getWorkersList() {
        return listWorkers.stream()
                .map(WorkerInformation::getWorkerInformation)
                .collect(Collectors.toList());

    }

    /**
     * Ask to stop a specific worker
     * @param workerName worker to stop
     * @return NOTFOUND or the worker information on this worker
     */
    @PutMapping(value = "/api/worker/{workerName}/stop", produces = "application/json")
    public WorkerInformation stopWorker(@PathVariable String workerName) {
        logger.info("Stop requested for [" + workerName + "]");

        // at this moment, just retrieve the worker
        List<WorkerInformation> listFiltered = listWorkers.stream().filter(w -> w.getName().equals(workerName))
                .map(WorkerInformation::getWorkerInformation)
                .collect(Collectors.toList());
        if (listFiltered.size() != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + workerName + "] not found");
        return listFiltered.get(0);

    }

    /**
     * Ask to start a specific worker
     * @param workerName worker to start
     * @return NOTFOUND or the worker information on this worker
     */
    @PutMapping(value = "/api/worker/{workerName}/start", produces = "application/json")
    public WorkerInformation startWorker(@PathVariable String workerName) {
        logger.info("Start requested for [" + workerName + "]");

        // at this moment, just retrieve the worker
        List<WorkerInformation> listFiltered = listWorkers.stream().filter(w -> w.getName().equals(workerName))
                .map(WorkerInformation::getWorkerInformation)
                .collect(Collectors.toList());

        if (listFiltered.size() != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + workerName + "] not found");
        return listFiltered.get(0);

    }
}
