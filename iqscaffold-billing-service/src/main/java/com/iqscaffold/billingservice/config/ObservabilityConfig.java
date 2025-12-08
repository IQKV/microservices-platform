package com.iqscaffold.billingservice.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for observability features including distributed tracing and metrics.
 * 
 * <p>Configures:
 * <ul>
 *   <li>OpenTelemetry for distributed tracing</li>
 *   <li>Micrometer for metrics collection</li>
 *   <li>Observation API for unified observability</li>
 *   <li>Prometheus metrics export</li>
 *   <li>@Timed annotation support for method-level metrics</li>
 *   <li>Scheduled metrics updates</li>
 * </ul>
 * 
 * <p>Distributed tracing is automatically enabled through Spring Boot's auto-configuration
 * with OpenTelemetry. Traces are exported to the configured OTLP endpoint.
 * 
 * <p>Metrics are exposed via Prometheus endpoint at /actuator/prometheus
 */
@Configuration
@EnableScheduling
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

    /**
     * Enable @Timed annotation support for method-level timing metrics.
     * 
     * <p>This allows marking methods with @Timed to automatically
     * record execution time metrics.
     * 
     * @param meterRegistry the meter registry
     * @return the timed aspect
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}
