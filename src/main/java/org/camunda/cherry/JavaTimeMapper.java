/* ******************************************************************** */
/*                                                                      */
/*  JavaTimeMapper                                                      */
/*                                                                      */
/*  To serialize JDK 8 Date, this mapper is required                    */
/* ******************************************************************** */
package org.camunda.cherry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.camunda.zeebe.client.api.JsonMapper;
import io.camunda.zeebe.client.impl.ZeebeObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaTimeMapper {
    @Bean
    public JsonMapper jsonMapper() {
        final var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new ZeebeObjectMapper(objectMapper);
    }

}
