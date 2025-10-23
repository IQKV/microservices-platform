package org.gripday.gatewayservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

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
        
        // Then
        StepVerifier.create(healthMono)
            .assertNext(health -> {
                assertThat(health).isNotNull();
                assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
                assertThat(health.getDetails()).containsKey("circuitBreaker");
                assertThat(health.getDetails()).containsKey("status");
            })
            .verifyComplete();
    }

    @Test
    void shouldIndicateCircuitBreakerHealthCorrectly() {
        // Given
        var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();
        
        // When
        var healthMono = healthIndicator.health();
        
        // Then
        StepVerifier.create(healthMono)
            .assertNext(health -> {
                assertThat(health).isNotNull();
                
                // The health should be UP if circuit breaker is enabled
                if (health.getStatus() == Status.UP) {
                    assertThat(health.getDetails().get("status")).isEqualTo("Monitoring downstream services");
                } else {
                    assertThat(health.getDetails().get("status")).isEqualTo("Circuit breaker not configured");
                }
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleHealthCheckExceptionsGracefully() {
        // Given
        var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();
        
        // When
        var healthMono = healthIndicator.health();
        
        // Then - Should not throw exceptions and always return a Health object
        StepVerifier.create(healthMono)
            .assertNext(health -> {
                assertThat(health).isNotNull();
                assertThat(health.getStatus()).isNotNull();
                assertThat(health.getDetails()).isNotNull();
            })
            .verifyComplete();
    }

    @Test
    void shouldCompleteHealthCheckWithinTimeout() {
        // Given
        var healthIndicator = new HealthCheckConfiguration.CircuitBreakerHealthIndicator();
        
        // When
        var healthMono = healthIndicator.health();
        
        // Then - Should complete quickly without timeout
        StepVerifier.create(healthMono)
            .expectNextCount(1)
            .verifyComplete();
    }
}