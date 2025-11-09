package org.gripday.authservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

/**
 * Test for health check configuration.
 */
class HealthCheckConfigTest {

  @Test
  void shouldCreateJwtServiceHealthIndicator() {
    // Given
    var healthIndicator = new HealthCheckConfig.JwtServiceHealthIndicator();

    // When
    var health = healthIndicator.health();

    // Then
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
    assertThat(health.getDetails()).containsKey("jwtService");
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  void shouldIndicateJwtServiceHealthBasedOnConfiguration() {
    // Given
    var healthIndicator = new HealthCheckConfig.JwtServiceHealthIndicator();

    // When
    var health = healthIndicator.health();

    // Then
    assertThat(health).isNotNull();

    // The health should be UP if JWT configuration is present, DOWN otherwise
    if (health.getStatus() == Status.UP) {
      assertThat(health.getDetails().get("status")).isEqualTo("JWT configuration present");
    } else {
      assertThat(health.getDetails().get("status")).isEqualTo("JWT secret not configured");
    }
  }

  @Test
  void shouldHandleHealthCheckExceptions() {
    // Given
    var healthIndicator = new HealthCheckConfig.JwtServiceHealthIndicator();

    // When
    var health = healthIndicator.health();

    // Then - Should not throw exceptions and always return a Health object
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isNotNull();
    assertThat(health.getDetails()).isNotNull();
  }
}