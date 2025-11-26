package com.iqscaffold.gatewayservice.filter;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.exception.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Circuit breaker filter for fault tolerance. Implements circuit breaker patterns with fallback mechanisms for service failures.
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);

  private final IqScaffoldProperties properties;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  public CircuitBreakerFilter(final IqScaffoldProperties properties, final CircuitBreakerRegistry circuitBreakerRegistry) {
    this.properties = properties;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!properties.gateway().circuitBreaker().enabled()) {
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
      return "user-service";
    }

    // Default circuit breaker for unknown services
    return "default-service";
  }

  private Mono<Void> handleCircuitBreakerOpen(ServerWebExchange exchange, String circuitBreakerName, Throwable throwable) {
    // Determine if this is a circuit breaker open state or actual service failure
    var circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
    var isCircuitOpen = circuitBreaker.getState() == CircuitBreaker.State.OPEN;

    if (isCircuitOpen) {
      var waitDuration = properties.gateway().circuitBreaker().waitDurationInOpenState();
      var retryAfterSeconds = (int) waitDuration.getSeconds();
      logger.warn("Circuit breaker '{}' is OPEN, rejecting request", circuitBreakerName);
      return Mono.error(new CircuitBreakerOpenException(circuitBreakerName,
          extractServiceName(circuitBreakerName), retryAfterSeconds));
    } else {
      logger.error("Service failure for circuit breaker '{}'", circuitBreakerName, throwable);
      // For non-open states, propagate the original error
      return Mono.error(throwable);
    }
  }

  private String extractServiceName(String circuitBreakerName) {
    // Extract service name from circuit breaker name
    if (circuitBreakerName.endsWith("-service")) {
      return circuitBreakerName;
    }
    return circuitBreakerName + "-service";
  }

  @Override
  public int getOrder() {
    return -25; // Execute after rate limiting but before routing
  }
}
