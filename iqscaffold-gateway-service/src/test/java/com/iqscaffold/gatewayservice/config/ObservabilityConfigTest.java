package com.iqscaffold.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

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

    metrics.recordRequestSuccess(requestTimer, "user-service");
    assertThat(meterRegistry.counter("iqscaffold.gateway.request.total", "result", "success", "route", "user-service").count()).isEqualTo(1.0);

    metrics.recordRequestFailure("user-service", "timeout", 504);
    assertThat(meterRegistry.counter("iqscaffold.gateway.request.total", "result", "failure", "route", "user-service", "reason", "timeout", "status", "504").count()).isEqualTo(1.0);
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
    assertThat(meterRegistry.counter("iqscaffold.gateway.authentication.total", "result", "success").count()).isEqualTo(1.0);

    // Test failure
    metrics.recordAuthenticationFailure("invalid_token");
    assertThat(meterRegistry.counter("iqscaffold.gateway.authentication.total", "result", "failure", "reason", "invalid_token").count()).isEqualTo(1.0);
  }

  @Test
  void shouldCreateRateLimitMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordRateLimitHit("/api/v1/auth/login", "tenant-123");

    // Then
    assertThat(meterRegistry.counter("iqscaffold.gateway.ratelimit.hit", "endpoint", "/api/v1/auth/login", "tenant", "tenant-123").count()).isEqualTo(1.0);

    // Test without tenant
    metrics.recordRateLimitHit("/api/v1/auth/signup", null);
    assertThat(meterRegistry.counter("iqscaffold.gateway.ratelimit.hit", "endpoint", "/api/v1/auth/signup", "tenant", "unknown").count()).isEqualTo(1.0);
  }

  @Test
  void shouldCreateCircuitBreakerMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordCircuitBreakerOpen("user-service");
    metrics.recordCircuitBreakerClosed("user-service");

    // Then
    assertThat(meterRegistry.counter("iqscaffold.gateway.circuitbreaker.open", "service", "user-service").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("iqscaffold.gateway.circuitbreaker.closed", "service", "user-service").count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordRouteLatencyMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordRouteLatency("user-service", 250L);

    // Then
    var timer = meterRegistry.timer("iqscaffold.gateway.route.latency", "route", "user-service");
    assertThat(timer.count()).isEqualTo(1L);
  }

  @Test
  void shouldRecordConnectionMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordActiveConnections(25);

    // Then
    var gauge = meterRegistry.find("iqscaffold.gateway.connections.active").gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(25.0);
  }

  @Test
  void shouldRecordRequestMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordTotalRequests("GET", "user-service");
    metrics.recordResponseStatus(200, "user-service");

    // Then
    assertThat(meterRegistry.counter("iqscaffold.gateway.requests.total", "method", "GET", "route", "user-service").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("iqscaffold.gateway.responses.total", "status_code", "200", "status_class", "2xx", "route", "user-service").count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordTenantMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordTenantRequests("tenant-123", "/api/v1/auth/login");
    metrics.recordTenantRequests(null, "/api/v1/auth/signup");

    // Then
    assertThat(meterRegistry.counter("iqscaffold.gateway.tenant.requests", "tenant", "tenant-123", "endpoint", "/api/v1/auth/login").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("iqscaffold.gateway.tenant.requests", "tenant", "unknown", "endpoint", "/api/v1/auth/signup").count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordCorsMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordCorsRequests("https://example.com", "GET");
    metrics.recordCorsRequests(null, "POST");

    // Then
    assertThat(meterRegistry.counter("iqscaffold.gateway.cors.requests", "origin", "https://example.com", "method", "GET").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("iqscaffold.gateway.cors.requests", "origin", "unknown", "method", "POST").count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordTransformationMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordTransformationTime("request", 15L);
    metrics.recordTransformationTime("response", 8L);

    // Then
    var requestTimer = meterRegistry.timer("iqscaffold.gateway.transformation.duration", "type", "request");
    var responseTimer = meterRegistry.timer("iqscaffold.gateway.transformation.duration", "type", "response");
    assertThat(requestTimer.count()).isEqualTo(1L);
    assertThat(responseTimer.count()).isEqualTo(1L);
  }

  @Test
  void shouldRecordLoadBalancingMetrics() {
    // Given
    var meterRegistry = new SimpleMeterRegistry();
    var metrics = new ObservabilityConfig.GatewayServiceMetrics(meterRegistry);

    // When
    metrics.recordLoadBalancingDecision("user-service", "instance-1");
    metrics.recordHealthCheckResult("user-service", true);

    // Then
    assertThat(meterRegistry.counter("iqscaffold.gateway.loadbalancing.decisions", "service", "user-service", "instance", "instance-1").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("iqscaffold.gateway.healthcheck.results", "service", "user-service", "result", "healthy").count()).isEqualTo(1.0);
  }

  @Test
  void shouldCreateCorrelationIdGlobalFilter() {
    // Given & When
    var filter = new ObservabilityConfig.CorrelationIdGlobalFilter();

    // Then
    assertThat(filter).isNotNull();
    assertThat(filter).isInstanceOf(ObservabilityConfig.CorrelationIdGlobalFilter.class);
  }
}