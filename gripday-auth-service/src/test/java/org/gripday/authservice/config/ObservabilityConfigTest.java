package org.gripday.authservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for observability configuration.
 */
class ObservabilityConfigTest {

    @Test
    void shouldCreateAuthServiceMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        
        // When
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // Then
        assertThat(metrics).isNotNull();
        
        // Test authentication metrics
        var authTimer = metrics.startAuthenticationTimer();
        assertThat(authTimer).isNotNull();
        
        metrics.recordAuthenticationSuccess(authTimer);
        assertThat(meterRegistry.counter("gripday.auth.authentication.total", "result", "success").count()).isEqualTo(1.0);
        
        metrics.recordAuthenticationFailure("invalid_credentials");
        assertThat(meterRegistry.counter("gripday.auth.authentication.total", "result", "failure", "reason", "invalid_credentials").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateRegistrationMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // When
        var regTimer = metrics.startRegistrationTimer();
        metrics.recordRegistrationSuccess(regTimer);
        
        // Then
        assertThat(meterRegistry.counter("gripday.auth.registration.total", "result", "success").count()).isEqualTo(1.0);
        
        // Test failure
        metrics.recordRegistrationFailure("duplicate_username");
        assertThat(meterRegistry.counter("gripday.auth.registration.total", "result", "failure", "reason", "duplicate_username").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateTokenRefreshMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // When
        var refreshTimer = metrics.startTokenRefreshTimer();
        metrics.recordTokenRefreshSuccess(refreshTimer);
        
        // Then
        assertThat(meterRegistry.counter("gripday.auth.token.refresh.total", "result", "success").count()).isEqualTo(1.0);
        
        // Test failure
        metrics.recordTokenRefreshFailure("invalid_token");
        assertThat(meterRegistry.counter("gripday.auth.token.refresh.total", "result", "failure", "reason", "invalid_token").count()).isEqualTo(1.0);
    }
}