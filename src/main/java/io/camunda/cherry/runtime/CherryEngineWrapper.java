package io.camunda.cherry.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import io.camunda.connector.feel.LocalFeelExpressionEvaluator;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import io.camunda.connector.runtime.annotation.ConnectorsObjectMapper;
import io.camunda.connector.runtime.annotation.OutboundConnectorObjectMapper;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Why 8.8 does not define this wrapper as bean? Mystery
 */
@Configuration
public class CherryEngineWrapper {
    private final CherrySecretProvider secretProvider;

    public CherryEngineWrapper(CherrySecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Bean
    public FeelExpressionEvaluator cherryFeelEngineWrapper() {
        return new LocalFeelExpressionEvaluator();
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
    @ConnectorsObjectMapper
    public ObjectMapper cherryOutboundConnectorObjectMapper() {
        // Same ObjectMapper any connector gets under the real Connector Runtime (Jdk8Module + JavaTimeModule,
        // lenient unknown-property/enum handling, etc.) - see ConnectorsObjectMapperSupplier in connector-object-mapper.
        // A bare `new ObjectMapper()` here has none of that, e.g. it can't serialize java.time.* output values.
        return ConnectorsObjectMapperSupplier.getCopy();
    }

}
