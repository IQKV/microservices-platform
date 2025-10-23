package org.gripday.gatewayservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for gateway observability configuration.
 */
class ObservabilityConfigTest {

    @Test
    void shouldCreateGatewayServiceMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        
        // When
        var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);
        
        // Then
        assertThat(metrics).isNotNull();
        
        // Test request metrics
        var requestTimer = metrics.startRequestTimer();
        assertThat(requestTimer).isNotNull();
        
        metrics.recordRequestSuccess(requestTimer, "auth-service");
        assertThat(meterRegistry.counter("gripday.gateway.request.total", "result", "success", "route", "auth-service").count()).isEqualTo(1.0);
        
        metrics.recordRequestFailure("auth-service", "timeout", 504);
        assertThat(meterRegistry.counter("gripday.gateway.request.total", "result", "failure", "route", "auth-service", "reason", "timeout", "status", "504").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateAuthenticationMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);
        
        // When
        var authTimer = metrics.startAuthenticationTimer();
        metrics.recordAuthenticationSuccess(authTimer);
        
        // Then
        assertThat(meterRegistry.counter("gripday.gateway.authentication.total", "result", "success").count()).isEqualTo(1.0);
        
        // Test failure
        metrics.recordAuthenticationFailure("invalid_token");
        assertThat(meterRegistry.counter("gripday.gateway.authentication.total", "result", "failure", "reason", "invalid_token").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateRateLimitMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);
        
        // When
        metrics.recordRateLimitHit("/api/v1/auth/login", "tenant-123");
        
        // Then
        assertThat(meterRegistry.counter("gripday.gateway.ratelimit.hit", "endpoint", "/api/v1/auth/login", "tenant", "tenant-123").count()).isEqualTo(1.0);
        
        // Test without tenant
        metrics.recordRateLimitHit("/api/v1/auth/signup", null);
        assertThat(meterRegistry.counter("gripday.gateway.ratelimit.hit", "endpoint", "/api/v1/auth/signup", "tenant", "unknown").count()).isEqualTo(1.0);
    }

    @Test
    void shouldCreateCircuitBreakerMetrics() {
        // Given
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);
        
        // When
        metrics.recordCircuitBreakerOpen("auth-service");
        metrics.recordCircuitBreakerClosed("auth-service");
        
        // Then
        assertThat(meterRegistry.counter("gripday.gateway.circuitbreaker.open", "service", "auth-service").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("gripday.gateway.circuitbreaker.closed", "service", "auth-service").count()).isEqualTo(1.0);
    }
}