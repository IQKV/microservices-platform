package org.gripday.gatewayservice.config;

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
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Configuration for observability features in the gateway service.
 * Provides correlation ID generation, MDC management, and custom metrics for reactive applications.
 */
@Configuration
@EnableConfigurationProperties(GripdayGatewayObservabilityProperties.class)
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
     * Global filter to add correlation ID and trace information for reactive requests.
     */
    @Bean
    public CorrelationIdGlobalFilter correlationIdGlobalFilter() {
        return new CorrelationIdGlobalFilter();
    }

    /**
     * Custom metrics for gateway service monitoring.
     */
    @Bean
    public GatewayServiceMetrics gatewayServiceMetrics(MeterRegistry meterRegistry) {
        return new GatewayServiceMetrics(meterRegistry);
    }

    /**
     * Global filter that adds correlation ID and trace information to reactive context.
     */
    public static class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {
        
        private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
        private static final String REQUEST_ID_HEADER = "X-Request-ID";
        private static final String TENANT_ID_HEADER = "X-Tenant-ID";
        
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            var request = exchange.getRequest();
            var response = exchange.getResponse();
            
            // Generate or extract correlation ID
            var correlationId = getOrGenerateCorrelationId(request);
            var requestId = getOrGenerateRequestId(request);
            var tenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);
            
            // Add to response headers
            response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
            response.getHeaders().add(REQUEST_ID_HEADER, requestId);
            
            // Add to MDC for logging (reactive context)
            return chain.filter(exchange)
                .contextWrite(ctx -> {
                    ctx = ctx.put("correlationId", correlationId);
                    ctx = ctx.put("requestId", requestId);
                    if (tenantId != null) {
                        ctx = ctx.put("tenantId", tenantId);
                    }
                    return ctx;
                })
                .doOnEach(signal -> {
                    if (signal.hasValue() || signal.hasError()) {
                        // Set MDC for logging
                        MDC.put("correlationId", correlationId);
                        MDC.put("requestId", requestId);
                        if (tenantId != null) {
                            MDC.put("tenantId", tenantId);
                        }
                    }
                })
                .doFinally(signalType -> MDC.clear());
        }
        
        private String getOrGenerateCorrelationId(org.springframework.http.server.reactive.ServerHttpRequest request) {
            var correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
            return correlationId != null ? correlationId : UUID.randomUUID().toString();
        }
        
        private String getOrGenerateRequestId(org.springframework.http.server.reactive.ServerHttpRequest request) {
            var requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
            return requestId != null ? requestId : "req-" + System.currentTimeMillis();
        }
        
        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    /**
     * Custom metrics for gateway service.
     */
    public static class GatewayServiceMetrics {
        
        private final Timer requestTimer;
        private final Timer authenticationTimer;
        private final Timer rateLimitTimer;
        private final MeterRegistry meterRegistry;
        
        public GatewayServiceMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.requestTimer = Timer.builder("gripday.gateway.request.duration")
                .description("Time taken for gateway request processing")
                .register(meterRegistry);
            this.authenticationTimer = Timer.builder("gripday.gateway.authentication.duration")
                .description("Time taken for authentication validation")
                .register(meterRegistry);
            this.rateLimitTimer = Timer.builder("gripday.gateway.ratelimit.duration")
                .description("Time taken for rate limit checking")
                .register(meterRegistry);
        }
        
        public Timer.Sample startRequestTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordRequestSuccess(Timer.Sample sample, String route) {
            sample.stop(Timer.builder("gripday.gateway.request.success")
                .tag("route", route)
                .description("Successful gateway requests")
                .register(meterRegistry));
            meterRegistry.counter("gripday.gateway.request.total", "result", "success", "route", route).increment();
        }
        
        public void recordRequestFailure(String route, String reason, int statusCode) {
            meterRegistry.counter("gripday.gateway.request.total", 
                "result", "failure", 
                "route", route, 
                "reason", reason,
                "status", String.valueOf(statusCode)).increment();
        }
        
        public Timer.Sample startAuthenticationTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordAuthenticationSuccess(Timer.Sample sample) {
            sample.stop(authenticationTimer);
            meterRegistry.counter("gripday.gateway.authentication.total", "result", "success").increment();
        }
        
        public void recordAuthenticationFailure(String reason) {
            meterRegistry.counter("gripday.gateway.authentication.total", "result", "failure", "reason", reason).increment();
        }
        
        public void recordRateLimitHit(String endpoint, String tenantId) {
            meterRegistry.counter("gripday.gateway.ratelimit.hit", 
                "endpoint", endpoint, 
                "tenant", tenantId != null ? tenantId : "unknown").increment();
        }
        
        public void recordCircuitBreakerOpen(String service) {
            meterRegistry.counter("gripday.gateway.circuitbreaker.open", "service", service).increment();
        }
        
        public void recordCircuitBreakerClosed(String service) {
            meterRegistry.counter("gripday.gateway.circuitbreaker.closed", "service", service).increment();
        }
    }
}