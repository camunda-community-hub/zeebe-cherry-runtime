/* ******************************************************************** */
/*                                                                      */
/*  MonitoringRestController                                           */
/*                                                                      */
/*  Rest API for the monitoring REST CALL                               */
/* example: http://localhost:8080/cherry/api/monitoring/pingzeebe       */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.zeebe.ZeebeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("cherry")

public class MonitoringRestController {

  private final JobRunnerFactory jobRunnerFactory;
  private final HistoryFactory historyFactory;
  private final ZeebeContainer zeebeContainer;
  private final DataSource dataSource;
  Logger logger = LoggerFactory.getLogger(AdminRestController.class.getName());

  MonitoringRestController(JobRunnerFactory jobRunnerFactory,
                           HistoryFactory historyFactory,
                           ZeebeContainer zeebeContainer,
                           DataSource dataSource) {
    this.jobRunnerFactory = jobRunnerFactory;
    this.historyFactory = historyFactory;
    this.zeebeContainer = zeebeContainer;
    this.dataSource = dataSource;
  }

  @GetMapping(value = "/api/monitoring/pingzeebe", produces = "application/json")
  public Map<String, Object> pingZeebe() {
    logger.info("Monitoring.pingZeebe - start");
    Map<String, Object> parameters = new HashMap<>();

    try {
      parameters.put("timestamp", System.currentTimeMillis());
      parameters.put("status", zeebeContainer.pingZeebeClient() ? "OK" : "FAIL");
      parameters.put("comment", "");
    } catch (TechnicalException te) {
      parameters.put("status", "FAIL");
      parameters.put("comment", te.getMessage());
    }

    logger.info("Monitoring.pingZeebe - end {}", parameters);
    return parameters;
  }
}
