/* ******************************************************************** */
/*                                                                      */
/*  Cherry application                                                 */
/*                                                                      */
/*  Spring boot application                                             */
/* ******************************************************************** */
package io.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * This Application is moved to io.camunda in order to detect all io.camunda.connector and
 * io.camunda.cherry objects
 */
@SpringBootApplication
@ConfigurationPropertiesScan("io.camunda")
public class CherryApplication {

  public static void main(String[] args) {

    SpringApplication.run(CherryApplication.class, args);
    // thanks to Spring, the class CherryJobRunnerFactory is active. All runners (worker,
    // connectors) start then
  }
  // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}
