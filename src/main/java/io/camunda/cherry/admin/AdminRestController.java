/* ******************************************************************** */
/*                                                                      */
/*  AdminRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runtime/nbthreads          */

/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.runtime.CherryHistoricFactory;
import io.camunda.cherry.runtime.CherryJobRunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("cherry")
public class AdminRestController {

    Logger logger = LoggerFactory.getLogger(AdminRestController.class.getName());


    @Autowired
    CherryJobRunnerFactory cherryJobRunnerFactory;

    @Autowired
    CherryHistoricFactory cherryHistoricFactory;
    /**
     * Spring populate the list of all workers
     */
    @Autowired
    private List<AbstractRunner> listRunner;

    @GetMapping(value = "/api/runtime/parameters", produces = "application/json")
    public Map<String,Object> getParameters() {
        Map<String,Object> parameters = new HashMap<>();
        parameters.put("NumberOfThreads", cherryJobRunnerFactory.getNumberOfThreads());
        return parameters;

    }
    @GetMapping(value = "/api/runtime/threads", produces = "application/json")
    public Integer getNumberOfThreads() {
        return cherryJobRunnerFactory.getNumberOfThreads();

    }

    @PutMapping(value = "/api/runtime/setthreads", produces = "application/json")
    public void setNumberOfThread(@RequestParam(name = "threads") Integer numberOfThreads) {
        cherryJobRunnerFactory.setNumberOfThreads(numberOfThreads);

    }


}
