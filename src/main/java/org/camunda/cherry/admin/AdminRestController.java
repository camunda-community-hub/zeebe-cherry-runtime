/* ******************************************************************** */
/*                                                                      */
/*  AdminRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runtime/nbthreads          */

/* ******************************************************************** */
package org.camunda.cherry.admin;

import org.camunda.cherry.definition.AbstractRunner;
import org.camunda.cherry.runtime.CherryJobRunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("cherry")
public class AdminRestController {

    Logger logger = LoggerFactory.getLogger(AdminRestController.class.getName());


    @Autowired
    CherryJobRunnerFactory cherryJobRunnerFactory;
    /**
     * Spring populate the list of all workers
     */
    @Autowired
    private List<AbstractRunner> listRunner;

    @GetMapping(value = "/api/runtime/nbthreads", produces = "application/json")
    public Integer getNumberOfThreads() {
        return cherryJobRunnerFactory.getNumberOfThreads();

    }

    @PostMapping(value = "/api/runtime/setnbthreads", produces = "application/json")
    public void setNumberOfThread(@PathVariable Integer numberOfThreads) {
        cherryJobRunnerFactory.setNumberOfThreads(numberOfThreads);

    }

}
