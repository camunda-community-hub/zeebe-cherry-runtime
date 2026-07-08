/* ******************************************************************** */
/*                                                                      */
/*  RuntimeRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runtime/nbthreads          */

/* ******************************************************************** */
package io.camunda.cherry.rest;

import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.store.StoreFactory;
import io.camunda.cherry.tenants.TenantsManager;
import io.camunda.client.spring.properties.CamundaClientProperties;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("cherry")
public class RuntimeRestController {

    private final JobRunnerFactory jobRunnerFactory;
    private final CamundaClientProperties camundaClientProperties;
    private final DataSource dataSource;
    private final TenantsManager tenantsManager;
    private final StoreFactory storeFactory;
    Logger logger = LoggerFactory.getLogger(RuntimeRestController.class.getName());

    RuntimeRestController(JobRunnerFactory jobRunnerFactory,
                          CamundaClientProperties camundaClientProperties,
                          DataSource dataSource, TenantsManager tenantsManager,
                          StoreFactory storeFactory) {
        this.jobRunnerFactory = jobRunnerFactory;
        this.camundaClientProperties = camundaClientProperties;
        this.dataSource = dataSource;
        this.tenantsManager = tenantsManager;
        this.storeFactory = storeFactory;
    }

    @GetMapping(value = "/api/ping", produces = "application/json")
    public Map<String, Object> ping() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RestAttribute.TIMESTAMP, System.currentTimeMillis());
        return parameters;
    }

    @GetMapping(value = "/api/runtime/parameters", produces = "application/json")
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(RestAttribute.ZEEBE_KIND_CONNECTION, camundaClientProperties.getMode().toString().toLowerCase());

        String clientSecret = camundaClientProperties.getAuth().getClientSecret();
        if (clientSecret != null) {
            if (clientSecret.length() > 2)
                clientSecret = clientSecret.substring(0, 2) + "***************";
            else
                clientSecret = "******************";
        }

        switch (camundaClientProperties.getMode()) {
            case saas:
                parameters.put(RestAttribute.CLOUD_REGION, camundaClientProperties.getCloud().getRegion());
                parameters.put(RestAttribute.CLOUD_CLUSTER_ID, camundaClientProperties.getCloud().getClusterId());
                parameters.put(RestAttribute.CLOUD_CLIENT_ID, camundaClientProperties.getAuth().getClientId());
                parameters.put(RestAttribute.CLOUD_CLIENT_SECRET, clientSecret);
                break;
            case selfManaged:
                parameters.put(RestAttribute.GRPC_ADDRESS, camundaClientProperties.getGrpcAddress().toString());
                parameters.put(RestAttribute.REST_ADDRESS, camundaClientProperties.getRestAddress().toString());
                parameters.put(RestAttribute.CLIENT_ID, camundaClientProperties.getAuth().getClientId());
                parameters.put(RestAttribute.CLIENT_SECRET, clientSecret);
                parameters.put(RestAttribute.AUTORIZATION_SERVER_URL,
                        camundaClientProperties.getAuth().getTokenUrl());
                parameters.put(RestAttribute.CLIENT_AUDIENCE, camundaClientProperties.getAuth().getAudience());
                Set<String> tenantIds = tenantsManager.getActiveTenantsIds();
                parameters.put(RestAttribute.TENANT_IDS, tenantIds == null ? "" : String.join(",", tenantIds));
                break;
        }

        parameters.put(RestAttribute.MAX_JOBS_ACTIVE, jobRunnerFactory.getMaxJobActive());
        parameters.put(RestAttribute.NB_THREADS, jobRunnerFactory.getNumberOfThreads());

        try (Connection con = dataSource.getConnection()) {
            parameters.put(RestAttribute.DATASOURCE_PRODUCT_NAME, con.getMetaData().getDatabaseProductName());
            parameters.put(RestAttribute.DATASOURCE_URL, con.getMetaData().getURL());
            parameters.put(RestAttribute.DATASOURCE_USER_NAME, con.getMetaData().getUserName());
        } catch (Exception e) {
            logger.error("During getParameters()", e);
        }

        parameters.put(RestAttribute.VERSION, getVersion());

        parameters.put(RestAttribute.STORES,
                storeFactory.getStores().stream()
                        .map(s -> Map.of(RestAttribute.NAME, s.getName(), RestAttribute.URL, s.getUrl(), RestAttribute.TYPE, s.getType()))
                        .toList());

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
