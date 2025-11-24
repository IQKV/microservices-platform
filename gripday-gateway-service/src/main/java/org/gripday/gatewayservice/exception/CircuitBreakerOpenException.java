package org.gripday.gatewayservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when circuit breaker is open and requests are being rejected.
 */
public class CircuitBreakerOpenException extends ResponseStatusException {

  private final String circuitBreakerName;
  private final String serviceName;
  private final int retryAfterSeconds;

  public CircuitBreakerOpenException(String circuitBreakerName, String serviceName, int retryAfterSeconds) {
    super(HttpStatus.SERVICE_UNAVAILABLE,
        String.format("Circuit breaker '%s' is open for service: %s. Service temporarily unavailable", 
            circuitBreakerName, serviceName));
    this.circuitBreakerName = circuitBreakerName;
    this.serviceName = serviceName;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public String getCircuitBreakerName() {
    return circuitBreakerName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public int getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
