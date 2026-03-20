package io.camunda.cherry.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.feel.FeelEngineWrapper;
import io.camunda.connector.runtime.annotation.OutboundConnectorObjectMapper;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Why 8.8 does not define this wrapper as bean? Mystery
 */
@Configuration
public class CherryEngineWrapper {
    @Autowired
    CherrySecretProvider secretProvider;

    @Bean
    public FeelEngineWrapper cherryFeelEngineWrapper() {
        return new FeelEngineWrapper();
    }

    @Bean
    public MeterRegistry cherryMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public SecretProviderAggregator cherryProviderAggregator() {
        return new SecretProviderAggregator(List.of(secretProvider));
    }


    @Bean
    @OutboundConnectorObjectMapper
    public ObjectMapper cherryOutboundConnectorObjectMapper() {
        return new ObjectMapper();
    }

}
