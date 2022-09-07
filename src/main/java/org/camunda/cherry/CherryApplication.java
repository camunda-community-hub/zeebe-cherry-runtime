/* ******************************************************************** */
/*                                                                      */
/*  Cherry application                                                 */
/*                                                                      */
/*  Spring boot application                                             */
/* ******************************************************************** */
package org.camunda.cherry;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("org.camunda.cherry.runtime.ZeebeContainer")

public class CherryApplication {

    public static void main(String[] args) {

        SpringApplication.run(CherryApplication.class, args);
        // thanks to Spring, the class CherryJobRunnerFactory is active. All runners (worker, connectors) start then
    }
    // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}
