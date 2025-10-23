package org.gripday.authservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Configuration for observability features including OpenTelemetry, metrics, and logging.
 * Provides correlation ID generation, MDC management, and custom metrics with environment-specific settings.
 */
@Configuration
@EnableConfigurationProperties(GripdayObservabilityProperties.class)
@AutoConfiguration(before = ObservationAutoConfiguration.class)
public class ObservabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    private final GripdayObservabilityProperties observabilityProperties;
    private final Environment environment;

    public ObservabilityConfig(GripdayObservabilityProperties observabilityProperties, Environment environment) {
        this.observabilityProperties = observabilityProperties;
        this.environment = environment;
    }

    /**
     * Configures OpenTelemetry SDK with environment-specific settings.
     */
    @Bean
    @ConditionalOnProperty(name = "gripday.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
    public OpenTelemetry openTelemetry() {
        var tracingProps = observabilityProperties.tracing();
        
        // Create resource with service information
        var resource = Resource.getDefault()
            .merge(Resource.create(Attributes.builder()
                .put("service.name", tracingProps.serviceName())
                .put("service.version", "1.0.0")
                .put("deployment.environment", getActiveProfile())
                .build()));

        // Configure OTLP exporter with environment-specific settings
        var spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(tracingProps.endpoint())
            .setTimeout(tracingProps.timeout())
            .build();

        // Configure tracer provider with sampling
        var tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                .setMaxExportBatchSize(512)
                .setScheduleDelay(Duration.ofSeconds(5))
                .build())
            .setResource(resource)
            .setSampler(Sampler.traceIdRatioBased(tracingProps.samplingRate()))
            .build();

        // Build OpenTelemetry SDK
        var openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();

        logger.info("OpenTelemetry configured for service: {} with endpoint: {} and sampling rate: {}", 
            tracingProps.serviceName(), tracingProps.endpoint(), tracingProps.samplingRate());

        return openTelemetry;
    }

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
     * Gets the active Spring profile for environment identification.
     */
    private String getActiveProfile() {
        var activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : "default";
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
        private final Timer databaseQueryTimer;
        private final Timer redisOperationTimer;
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
            this.databaseQueryTimer = Timer.builder("gripday.auth.database.query.duration")
                .description("Time taken for database queries")
                .register(meterRegistry);
            this.redisOperationTimer = Timer.builder("gripday.auth.redis.operation.duration")
                .description("Time taken for Redis operations")
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
        
        public Timer.Sample startDatabaseQueryTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordDatabaseQuerySuccess(Timer.Sample sample, String operation) {
            sample.stop(Timer.builder("gripday.auth.database.query.success")
                .tag("operation", operation)
                .description("Successful database queries")
                .register(meterRegistry));
            meterRegistry.counter("gripday.auth.database.query.total", "result", "success", "operation", operation).increment();
        }
        
        public void recordDatabaseQueryFailure(String operation, String reason) {
            meterRegistry.counter("gripday.auth.database.query.total", 
                "result", "failure", 
                "operation", operation, 
                "reason", reason).increment();
        }
        
        public Timer.Sample startRedisOperationTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordRedisOperationSuccess(Timer.Sample sample, String operation) {
            sample.stop(Timer.builder("gripday.auth.redis.operation.success")
                .tag("operation", operation)
                .description("Successful Redis operations")
                .register(meterRegistry));
            meterRegistry.counter("gripday.auth.redis.operation.total", "result", "success", "operation", operation).increment();
        }
        
        public void recordRedisOperationFailure(String operation, String reason) {
            meterRegistry.counter("gripday.auth.redis.operation.total", 
                "result", "failure", 
                "operation", operation, 
                "reason", reason).increment();
        }
        
        public void recordActiveUsers(int count) {
            meterRegistry.gauge("gripday.auth.users.active", count);
        }
        
        public void recordTotalUsers(int count) {
            meterRegistry.gauge("gripday.auth.users.total", count);
        }
        
        public void recordFailedLoginAttempts(String username, String reason) {
            meterRegistry.counter("gripday.auth.login.failed", "reason", reason).increment();
        }
        
        public void recordPasswordResetRequest(String method) {
            meterRegistry.counter("gripday.auth.password.reset.request", "method", method).increment();
        }
        
        public void recordAccountLockout(String reason) {
            meterRegistry.counter("gripday.auth.account.lockout", "reason", reason).increment();
        }
    }
}