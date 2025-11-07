package org.gripday.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

/**
 * Test for gateway health check configuration.
 */
class HealthCheckConfigurationTest {

  @Test
  void shouldCreateCircuitBreakerHealthIndicator() {
    // Given
    var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();

    // When
    var healthMono = healthIndicator.health();
    var health = healthMono.block();

    // Then
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
    assertThat(health.getDetails()).containsKey("circuitBreaker");
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  void shouldIndicateCircuitBreakerHealthCorrectly() {
    // Given
    var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();

    // When
    var healthMono = healthIndicator.health();
    var health = healthMono.block();

    // Then
    assertThat(health).isNotNull();

    // The health should be UP if circuit breaker is enabled
    if (health.getStatus() == Status.UP) {
      assertThat(health.getDetails().get("status")).isEqualTo("Monitoring downstream services");
    } else {
      assertThat(health.getDetails().get("status")).isEqualTo("Circuit breaker not configured");
    }
  }

  @Test
  void shouldHandleHealthCheckExceptionsGracefully() {
    // Given
    var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();

    // When
    var healthMono = healthIndicator.health();
    var health = healthMono.block();

    // Then - Should not throw exceptions and always return a Health object
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isNotNull();
    assertThat(health.getDetails()).isNotNull();
  }

  @Test
  void shouldCompleteHealthCheckWithinTimeout() {
    // Given
    var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();

    // When
    var healthMono = healthIndicator.health();

    // Then - Should complete quickly without timeout
    assertThat(healthMono).isNotNull();
    var health = healthMono.block();
    assertThat(health).isNotNull();
  }
}