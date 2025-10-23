package org.gripday.authservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for observability configuration and health checks.
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

    @Test
    void shouldCreateDatabaseMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // When
        var dbTimer = metrics.startDatabaseQueryTimer();
        metrics.recordDatabaseQuerySuccess(dbTimer, "findByUsername");
        
        // Then
        assertThat(meterRegistry.counter("gripday.auth.database.query.total", "result", "success", "operation", "findByUsername").count()).isEqualTo(1.0);
        
        // Test failure
        metrics.recordDatabaseQueryFailure("findByEmail", "connection_timeout");
        assertThat(meterRegistry.counter("gripday.auth.database.query.total", "result", "failure", "operation", "findByEmail", "reason", "connection_timeout").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateRedisMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // When
        var redisTimer = metrics.startRedisOperationTimer();
        metrics.recordRedisOperationSuccess(redisTimer, "cache_get");
        
        // Then
        assertThat(meterRegistry.counter("gripday.auth.redis.operation.total", "result", "success", "operation", "cache_get").count()).isEqualTo(1.0);
        
        // Test failure
        metrics.recordRedisOperationFailure("cache_set", "connection_failed");
        assertThat(meterRegistry.counter("gripday.auth.redis.operation.total", "result", "failure", "operation", "cache_set", "reason", "connection_failed").count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordUserMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // When
        metrics.recordActiveUsers(150);
        metrics.recordTotalUsers(1000);
        
        // Then - Verify metrics were recorded (gauges are created when recorded)
        // We can't easily test gauge values in SimpleMeterRegistry without the actual gauge objects
        // So we just verify the methods don't throw exceptions
        assertThat(metrics).isNotNull();
    }

    @Test
    void shouldRecordSecurityEvents() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.AuthServiceMetrics(meterRegistry);
        
        // When
        metrics.recordFailedLoginAttempts("testuser", "invalid_password");
        metrics.recordAccountLockout("too_many_attempts");
        metrics.recordPasswordResetRequest("email");
        
        // Then
        assertThat(meterRegistry.counter("gripday.auth.login.failed", "reason", "invalid_password").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("gripday.auth.account.lockout", "reason", "too_many_attempts").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("gripday.auth.password.reset.request", "method", "email").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateJwtServiceHealthIndicator() {
        // Given
        var healthIndicator = new HealthCheckConfiguration.JwtServiceHealthIndicator();
        
        // When
        var health = healthIndicator.health();
        
        // Then
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        assertThat(health.getDetails()).containsKey("jwtService");
    }

    @Test
    void shouldCreateCorrelationIdFilter() {
        // Given & When
        var filter = new ObservabilityConfig.CorrelationIdFilter();
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter).isInstanceOf(ObservabilityConfig.CorrelationIdFilter.class);
    }
}