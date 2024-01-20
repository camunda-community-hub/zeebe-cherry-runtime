/* ******************************************************************** */
/*                                                                      */
/*  AdminRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runtime/nbthreads          */

/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.zeebe.ZeebeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("cherry")
public class AdminRestController {

  private final JobRunnerFactory jobRunnerFactory;
  private final HistoryFactory historyFactory;
  private final ZeebeConfiguration zeebeConfiguration;
  private final DataSource dataSource;
  Logger logger = LoggerFactory.getLogger(AdminRestController.class.getName());

  AdminRestController(JobRunnerFactory jobRunnerFactory,
                      HistoryFactory historyFactory,
                      ZeebeConfiguration zeebeConfiguration,
                      DataSource dataSource) {
    this.jobRunnerFactory = jobRunnerFactory;
    this.historyFactory = historyFactory;
    this.zeebeConfiguration = zeebeConfiguration;
    this.dataSource = dataSource;
  }

  @GetMapping(value = "/api/ping", produces = "application/json")
  public Map<String, Object> ping() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("timestamp", System.currentTimeMillis());
    return parameters;
  }

  @GetMapping(value = "/api/runtime/parameters", produces = "application/json")
  public Map<String, Object> getParameters() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("zeebekindconnection", zeebeConfiguration.getTypeConnection().toString());

    switch (zeebeConfiguration.getTypeConnection()) {
    case CLOUD -> {

      parameters.put("cloudRegion", zeebeConfiguration.getRegion());
      parameters.put("cloudClusterID", zeebeConfiguration.getClusterId());
      parameters.put("cloudClientID", zeebeConfiguration.getClientId());
      parameters.put("cloudClientSecret", ""); // never send the client Secret
    }
    case IDENTITY -> {
      parameters.put("gatewayAddress", zeebeConfiguration.getGatewayAddress());
      parameters.put("clientId", zeebeConfiguration.getClientId());
      parameters.put("clientSecret",
          (zeebeConfiguration.getClientSecret() != null && zeebeConfiguration.getClientSecret().length() > 0 ?
              zeebeConfiguration.getClientSecret().charAt(0) :
              "*") + "*********");
      parameters.put("AutorizationServerUrl", zeebeConfiguration.getAuthorizationServerUrl());
      parameters.put("clientAudience", zeebeConfiguration.getAudience());

      parameters.put("plainText", zeebeConfiguration.isPlaintext());
      parameters.put("tenantIds",
          zeebeConfiguration.getListTenantIds() == null ? "" : String.join(";", zeebeConfiguration.getListTenantIds()));
    }
    case DIRECTIPADDRESS -> {
      parameters.put("gatewayAddress", zeebeConfiguration.getGatewayAddress());
      parameters.put("plainText", zeebeConfiguration.isPlaintext());
      parameters.put("tenantIds",
          zeebeConfiguration.getListTenantIds() == null ? "" : String.join(";", zeebeConfiguration.getListTenantIds()));
    }
    }
    // we don't want the configuration here, but the running information
    parameters.put("maxJobsActive", jobRunnerFactory.getMaxJobActive());
    parameters.put("nbThreads", jobRunnerFactory.getNumberOfThreads());

    try (Connection con = dataSource.getConnection()) {
      parameters.put("datasourceProductName", con.getMetaData().getDatabaseProductName());
      parameters.put("datasourceUrl", con.getMetaData().getURL());
      parameters.put("datasourceUserName", con.getMetaData().getUserName());

    } catch (Exception e) {
      logger.error("During getParameters() " + e);
    }
    return parameters;
  }

  @GetMapping(value = "/api/runtime/threads", produces = "application/json")
  public Integer getNumberOfThreads() {
    return jobRunnerFactory.getNumberOfThreads();
  }

  @PutMapping(value = "/api/runtime/setthreads", produces = "application/json")
  public void setNumberOfThread(@RequestParam(name = "threads") Integer numberOfThreads) {
    jobRunnerFactory.setNumberOfThreads(numberOfThreads);
  }

}
