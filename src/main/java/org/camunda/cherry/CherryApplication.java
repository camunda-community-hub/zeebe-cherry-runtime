/* ******************************************************************** */
/*                                                                      */
/*  Cherry application                                                 */
/*                                                                      */
/*  Spring boot application                                             */
/* ******************************************************************** */
package org.camunda.cherry;


import io.camunda.zeebe.spring.client.EnableZeebeClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableZeebeClient
public class CherryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CherryApplication.class, args);
    }
    // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}
