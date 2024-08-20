/* ******************************************************************** */
/*                                                                      */
/*  AdminRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runtime/nbthreads          */

/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.zeebe.ZeebeContainer;
import io.camunda.zeebe.spring.client.properties.CamundaClientProperties;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("cherry")
public class AdminRestController {

  private final JobRunnerFactory jobRunnerFactory;
  private final ZeebeContainer zeebeContainer;
  private final CamundaClientProperties camundaClientProperties;
  private final DataSource dataSource;
  Logger logger = LoggerFactory.getLogger(AdminRestController.class.getName());

  AdminRestController(JobRunnerFactory jobRunnerFactory,
                      ZeebeContainer zeebeContainer,
                      CamundaClientProperties camundaClientProperties,
                      DataSource dataSource) {
    this.jobRunnerFactory = jobRunnerFactory;
    this.zeebeContainer = zeebeContainer;
    this.camundaClientProperties = camundaClientProperties;
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

    parameters.put("zeebekindconnection", camundaClientProperties.getMode().toString());

    String clientSecret = camundaClientProperties.getAuth().getClientSecret();
    if (clientSecret != null) {
      if (clientSecret.length() > 2)
        clientSecret = clientSecret.substring(0, 2) + "***************";
      else
        clientSecret = "******************";
    }

    switch (camundaClientProperties.getMode()) {
    case saas:
      parameters.put("cloudRegion", camundaClientProperties.getRegion());
      parameters.put("cloudClusterID", camundaClientProperties.getClusterId());
      parameters.put("cloudClientID", camundaClientProperties.getAuth().getClientId());
      parameters.put("cloudClientSecret", clientSecret); // never send the client Secret
      break;
    case oidc:
      parameters.put("gatewayAddress", camundaClientProperties.getZeebe().getGatewayUrl());
      parameters.put("clientId", camundaClientProperties.getAuth().getClientId());
      parameters.put("clientSecret", clientSecret);
      parameters.put("AutorizationServerUrl", camundaClientProperties.getAuth().getIssuer());
      parameters.put("clientAudience", camundaClientProperties.getZeebe().getAudience());

      parameters.put("tenantIds", camundaClientProperties.getTenantIds() == null ?
          "" :
          String.join(";", camundaClientProperties.getTenantIds()));

      break;
    case simple:
      parameters.put("gatewayAddress", camundaClientProperties.getZeebe().getGatewayUrl());
      break;

    }

    // we don't want the configuration here, but the running information
    parameters.put("maxJobsActive", jobRunnerFactory.getMaxJobActive());
    parameters.put("nbThreads", jobRunnerFactory.getNumberOfThreads());

    try (Connection con = dataSource.getConnection()) {
      parameters.put("datasourceProductName", con.getMetaData().getDatabaseProductName());
      parameters.put("datasourceUrl", con.getMetaData().getURL());
      parameters.put("datasourceUserName", con.getMetaData().getUserName());

    } catch (Exception e) {
      logger.error("During getParameters() : {}", e);
    }

    parameters.put("version", getVersion());

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

  private String getVersion() {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    try (FileReader fileReader = new FileReader("pom.xml")) {
      Model model = reader.read(fileReader);
      return model.getVersion();
    } catch (IOException | XmlPullParserException e) {
      logger.error("Exception during load pom.xml: {}", e);
      return null;
    }
  }
}
