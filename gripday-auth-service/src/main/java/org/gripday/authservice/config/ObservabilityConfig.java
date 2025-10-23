package org.gripday.authservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Configuration for observability features including OpenTelemetry, metrics, and logging.
 * Provides correlation ID generation, MDC management, and custom metrics.
 */
@Configuration
@EnableConfigurationProperties(GripdayObservabilityProperties.class)
@AutoConfiguration(before = ObservationAutoConfiguration.class)
public class ObservabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    /**
     * Enables @Observed annotation support for automatic observation of methods.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    /**
     * Filter to add correlation ID and trace information to MDC for structured logging.
     */
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * Custom metrics for authentication service monitoring.
     */
    @Bean
    public AuthServiceMetrics authServiceMetrics(MeterRegistry meterRegistry) {
        return new AuthServiceMetrics(meterRegistry);
    }

    /**
     * Filter that adds correlation ID and trace information to MDC.
     */
    public static class CorrelationIdFilter extends OncePerRequestFilter {
        
        private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
        private static final String REQUEST_ID_HEADER = "X-Request-ID";
        private static final String TENANT_ID_HEADER = "X-Tenant-ID";
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            try {
                // Generate or extract correlation ID
                var correlationId = getOrGenerateCorrelationId(request);
                var requestId = getOrGenerateRequestId(request);
                var tenantId = request.getHeader(TENANT_ID_HEADER);
                
                // Add to MDC for logging
                MDC.put("correlationId", correlationId);
                MDC.put("requestId", requestId);
                if (tenantId != null) {
                    MDC.put("tenantId", tenantId);
                }
                
                // Add to response headers
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
                response.setHeader(REQUEST_ID_HEADER, requestId);
                
                filterChain.doFilter(request, response);
                
            } finally {
                // Clean up MDC
                MDC.clear();
            }
        }
        
        private String getOrGenerateCorrelationId(HttpServletRequest request) {
            var correlationId = request.getHeader(CORRELATION_ID_HEADER);
            return correlationId != null ? correlationId : UUID.randomUUID().toString();
        }
        
        private String getOrGenerateRequestId(HttpServletRequest request) {
            var requestId = request.getHeader(REQUEST_ID_HEADER);
            return requestId != null ? requestId : "req-" + System.currentTimeMillis();
        }
    }

    /**
     * Custom metrics for authentication service.
     */
    public static class AuthServiceMetrics {
        
        private final Timer authenticationTimer;
        private final Timer userRegistrationTimer;
        private final Timer tokenRefreshTimer;
        private final MeterRegistry meterRegistry;
        
        public AuthServiceMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.authenticationTimer = Timer.builder("gripday.auth.authentication.duration")
                .description("Time taken for user authentication")
                .register(meterRegistry);
            this.userRegistrationTimer = Timer.builder("gripday.auth.registration.duration")
                .description("Time taken for user registration")
                .register(meterRegistry);
            this.tokenRefreshTimer = Timer.builder("gripday.auth.token.refresh.duration")
                .description("Time taken for token refresh")
                .register(meterRegistry);
        }
        
        public Timer.Sample startAuthenticationTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordAuthenticationSuccess(Timer.Sample sample) {
            sample.stop(Timer.builder("gripday.auth.authentication.success")
                .description("Successful authentication attempts")
                .register(meterRegistry));
            meterRegistry.counter("gripday.auth.authentication.total", "result", "success").increment();
        }
        
        public void recordAuthenticationFailure(String reason) {
            meterRegistry.counter("gripday.auth.authentication.total", "result", "failure", "reason", reason).increment();
        }
        
        public Timer.Sample startRegistrationTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordRegistrationSuccess(Timer.Sample sample) {
            sample.stop(userRegistrationTimer);
            meterRegistry.counter("gripday.auth.registration.total", "result", "success").increment();
        }
        
        public void recordRegistrationFailure(String reason) {
            meterRegistry.counter("gripday.auth.registration.total", "result", "failure", "reason", reason).increment();
        }
        
        public Timer.Sample startTokenRefreshTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordTokenRefreshSuccess(Timer.Sample sample) {
            sample.stop(tokenRefreshTimer);
            meterRegistry.counter("gripday.auth.token.refresh.total", "result", "success").increment();
        }
        
        public void recordTokenRefreshFailure(String reason) {
            meterRegistry.counter("gripday.auth.token.refresh.total", "result", "failure", "reason", reason).increment();
        }
    }
}