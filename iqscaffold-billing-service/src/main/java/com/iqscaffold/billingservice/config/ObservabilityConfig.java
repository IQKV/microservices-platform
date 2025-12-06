package com.iqscaffold.billingservice.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for observability features including distributed tracing.
 * 
 * <p>Configures:
 * <ul>
 *   <li>OpenTelemetry for distributed tracing</li>
 *   <li>Micrometer for metrics collection</li>
 *   <li>Observation API for unified observability</li>
 * </ul>
 * 
 * <p>Distributed tracing is automatically enabled through Spring Boot's auto-configuration
 * with OpenTelemetry. Traces are exported to the configured OTLP endpoint.
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Enable @Observed annotation support for automatic observation of methods.
     * 
     * <p>This allows marking service methods with @Observed to automatically
     * create spans and metrics for those methods.
     * 
     * @param observationRegistry the observation registry
     * @return the observed aspect
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
