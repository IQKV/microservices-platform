package org.gripday.gatewayservice.filter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Circuit breaker filter for fault tolerance. Implements circuit breaker patterns with fallback mechanisms for service failures.
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);

  private final GatewayProperties gatewayProperties;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final ObjectMapper objectMapper;

  public CircuitBreakerFilter(GatewayProperties gatewayProperties, CircuitBreakerRegistry circuitBreakerRegistry, ObjectMapper objectMapper) {
    this.gatewayProperties = gatewayProperties;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.objectMapper = objectMapper;
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
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    // Add circuit breaker headers
    response.getHeaders().add("X-Circuit-Breaker", circuitBreakerName);
    response.getHeaders().add("X-Circuit-Breaker-State", circuitBreaker.getState().toString());

    if (isCircuitOpen) {
      var waitDuration = gatewayProperties.circuitBreaker().waitDurationInOpenState();
      response.getHeaders().add("Retry-After", String.valueOf(waitDuration.getSeconds()));
    }

    var correlationId = MDC.get("correlationId");
    var tenantId = MDC.get("tenantId");

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
    pd.setTitle(status.getReasonPhrase());
    pd.setType(URI.create("/problems/" + (isCircuitOpen ? "circuit_breaker_open" : "service_unavailable")));
    pd.setInstance(URI.create(request.getPath().value()));
    pd.setProperty("code", errorCode);
    pd.setProperty("timestamp", Instant.now().toString());
    pd.setProperty("circuitBreaker", circuitBreakerName);
    if (isCircuitOpen) {
      var retryAfter = gatewayProperties.circuitBreaker().waitDurationInOpenState().getSeconds();
      pd.setProperty("retryAfter", retryAfter);
    }
    if (correlationId != null) {
      pd.setProperty("correlationId", correlationId);
    }
    if (tenantId != null) {
      pd.setProperty("tenantId", tenantId);
    }

    try {
      byte[] body = objectMapper.writeValueAsBytes(pd);
      var buffer = response.bufferFactory().wrap(body);
      return response.writeWith(Mono.just(buffer));
    } catch (Exception e) {
      var fallback = ("{\n  \"type\": \"" + pd.getType() + "\",\n" +
          "  \"title\": \"" + pd.getTitle() + "\",\n" +
          "  \"status\": " + pd.getStatus() + ",\n" +
          "  \"detail\": \"" + message + "\",\n" +
          "  \"instance\": \"" + request.getPath().value() + "\"\n}")
          .getBytes(StandardCharsets.UTF_8);
      var buffer = response.bufferFactory().wrap(fallback);
      return response.writeWith(Mono.just(buffer));
    }
  }

  @Override
  public int getOrder() {
    return -25; // Execute after rate limiting but before routing
  }
}