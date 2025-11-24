package org.gripday.gatewayservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("CircuitBreakerOpenException Tests")
class CircuitBreakerOpenExceptionTest {

  @Test
  @DisplayName("Should create exception with circuit breaker details")
  void shouldCreateExceptionWithCircuitBreakerDetails() {
    // Arrange
    var circuitBreakerName = "user-service-cb";
    var serviceName = "user-service";
    var retryAfterSeconds = 30;

    // Act
    var exception = new CircuitBreakerOpenException(circuitBreakerName, serviceName, retryAfterSeconds);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getReason()).contains("Circuit breaker");
    assertThat(exception.getReason()).contains(circuitBreakerName);
    assertThat(exception.getReason()).contains(serviceName);
    assertThat(exception.getReason()).contains("is open");
    assertThat(exception.getCircuitBreakerName()).isEqualTo(circuitBreakerName);
    assertThat(exception.getServiceName()).isEqualTo(serviceName);
    assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfterSeconds);
  }

  @Test
  @DisplayName("Should create exception with default circuit breaker")
  void shouldCreateExceptionWithDefaultCircuitBreaker() {
    // Arrange
    var circuitBreakerName = "default-service";
    var serviceName = "default-service";
    var retryAfterSeconds = 60;

    // Act
    var exception = new CircuitBreakerOpenException(circuitBreakerName, serviceName, retryAfterSeconds);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getCircuitBreakerName()).isEqualTo(circuitBreakerName);
    assertThat(exception.getServiceName()).isEqualTo(serviceName);
    assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfterSeconds);
  }

  @Test
  @DisplayName("Should create exception with different retry durations")
  void shouldCreateExceptionWithDifferentRetryDurations() {
    // Arrange
    var circuitBreakerName = "bookstore-service-cb";
    var serviceName = "bookstore-service";
    var retryAfterSeconds = 120;

    // Act
    var exception = new CircuitBreakerOpenException(circuitBreakerName, serviceName, retryAfterSeconds);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getRetryAfterSeconds()).isEqualTo(120);
  }
}
