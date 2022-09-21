/* ******************************************************************** */
/*                                                                      */
/*  AdminRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runtime/nbthreads          */

/* ******************************************************************** */
package org.camunda.cherry.admin;

import org.camunda.cherry.definition.AbstractRunner;
import org.camunda.cherry.runtime.CherryHistoricFactory;
import org.camunda.cherry.runtime.CherryJobRunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping(value = "/api/runtime/threads", produces = "application/json")
    public Integer getNumberOfThreads() {
        return cherryJobRunnerFactory.getNumberOfThreads();

    }

    @PutMapping(value = "/api/runtime/setthreads", produces = "application/json")
    public void setNumberOfThread(@RequestParam(name = "threads") Integer numberOfThreads) {
        cherryJobRunnerFactory.setNumberOfThreads(numberOfThreads);

    }

    @GetMapping(value = "/api/runtime/info", produces = "application/json")
    public Map<String,Object> getInfo( @RequestParam(name = "delaystatsinhours", required = false) Integer delayStatsInHours) {
        Map<String,Object> info = new HashMap<>();
        info.put("performance", cherryHistoricFactory.getEnginePerformance(delayStatsInHours==null? 24 : delayStatsInHours));
        info.put("statistic", cherryHistoricFactory.getEngineStatistic(delayStatsInHours==null? 24 : delayStatsInHours));
        return info;
    }
}
