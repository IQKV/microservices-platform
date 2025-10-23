package org.gripday.gatewayservice.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.gripday.gatewayservice.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Circuit breaker filter for fault tolerance.
 * Implements circuit breaker patterns with fallback mechanisms for service failures.
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);
    
    private final GatewayProperties gatewayProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerFilter(GatewayProperties gatewayProperties, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.gatewayProperties = gatewayProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayProperties.circuitBreaker().enabled()) {
            return chain.filter(exchange);
        }
        
        var request = exchange.getRequest();
        var path = request.getPath().value();
        
        // Determine which circuit breaker to use based on the route
        var circuitBreakerName = determineCircuitBreakerName(path);
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        
        logger.debug("Applying circuit breaker '{}' for path: {}, state: {}", 
            circuitBreakerName, path, circuitBreaker.getState());
        
        return chain.filter(exchange)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(throwable -> {
                logger.error("Circuit breaker '{}' triggered for path: {}", circuitBreakerName, path, throwable);
                return handleCircuitBreakerOpen(exchange, circuitBreakerName, throwable);
            });
    }

    private String determineCircuitBreakerName(String path) {
        // Map paths to specific circuit breakers
        if (path.startsWith("/api/v1/auth/")) {
            return "auth-service";
        }
        
        // Default circuit breaker for unknown services
        return "default-service";
    }

    private Mono<Void> handleCircuitBreakerOpen(ServerWebExchange exchange, String circuitBreakerName, Throwable throwable) {
        var response = exchange.getResponse();
        var request = exchange.getRequest();
        
        // Determine if this is a circuit breaker open state or actual service failure
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        var isCircuitOpen = circuitBreaker.getState() == CircuitBreaker.State.OPEN;
        
        HttpStatus status;
        String errorCode;
        String message;
        
        if (isCircuitOpen) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "CIRCUIT_BREAKER_OPEN";
            message = "Service temporarily unavailable due to circuit breaker";
            logger.warn("Circuit breaker '{}' is OPEN, rejecting request to: {}", circuitBreakerName, request.getPath().value());
        } else {
            status = HttpStatus.BAD_GATEWAY;
            errorCode = "SERVICE_UNAVAILABLE";
            message = "Upstream service is currently unavailable";
            logger.error("Service failure for circuit breaker '{}', path: {}", circuitBreakerName, request.getPath().value(), throwable);
        }
        
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        // Add circuit breaker headers
        response.getHeaders().add("X-Circuit-Breaker", circuitBreakerName);
        response.getHeaders().add("X-Circuit-Breaker-State", circuitBreaker.getState().toString());
        
        if (isCircuitOpen) {
            var waitDuration = gatewayProperties.circuitBreaker().waitDurationInOpenState();
            response.getHeaders().add("Retry-After", String.valueOf(waitDuration.getSeconds()));
        }
        
        var correlationId = MDC.get("correlationId");
        var tenantId = MDC.get("tenantId");
        
        var errorResponse = createCircuitBreakerErrorResponse(errorCode, message, 
            request.getPath().value(), correlationId, tenantId, circuitBreakerName, isCircuitOpen);
        
        var buffer = response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String createCircuitBreakerErrorResponse(String errorCode, String message, String path, 
            String correlationId, String tenantId, String circuitBreakerName, boolean isCircuitOpen) {
        
        var errorResponseBuilder = new StringBuilder();
        errorResponseBuilder.append("{\n");
        errorResponseBuilder.append("  \"error\": {\n");
        errorResponseBuilder.append("    \"code\": \"").append(errorCode).append("\",\n");
        errorResponseBuilder.append("    \"message\": \"").append(message).append("\",\n");
        
        if (isCircuitOpen) {
            errorResponseBuilder.append("    \"details\": \"The service is temporarily unavailable. Please try again later.\",\n");
            var retryAfter = gatewayProperties.circuitBreaker().waitDurationInOpenState().getSeconds();
            errorResponseBuilder.append("    \"retryAfter\": ").append(retryAfter).append(",\n");
        } else {
            errorResponseBuilder.append("    \"details\": \"The upstream service is currently experiencing issues.\",\n");
        }
        
        errorResponseBuilder.append("    \"timestamp\": \"").append(Instant.now()).append("\",\n");
        errorResponseBuilder.append("    \"path\": \"").append(path).append("\",\n");
        errorResponseBuilder.append("    \"circuitBreaker\": \"").append(circuitBreakerName).append("\"");
        
        if (correlationId != null) {
            errorResponseBuilder.append(",\n    \"correlationId\": \"").append(correlationId).append("\"");
        }
        
        if (tenantId != null) {
            errorResponseBuilder.append(",\n    \"tenantId\": \"").append(tenantId).append("\"");
        }
        
        errorResponseBuilder.append("\n  }\n}");
        
        return errorResponseBuilder.toString();
    }

    @Override
    public int getOrder() {
        return -25; // Execute after rate limiting but before routing
    }
}