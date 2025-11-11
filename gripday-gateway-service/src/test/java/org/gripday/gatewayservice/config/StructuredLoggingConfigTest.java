package org.gripday.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Test for gateway structured logging configuration.
 */
class StructuredLoggingConfigTest {

  @Test
  void shouldCreateStructuredLogger() {
    // Given
    var config = new StructuredLoggingConfig();

    // When
    var logger = config.structuredLogger();

    // Then
    assertThat(logger).isNotNull();
    assertThat(logger).isInstanceOf(StructuredLoggingConfig.StructuredLogger.class);
  }

  @Test
  void shouldCreateRequestLogger() {
    // Given
    var config = new StructuredLoggingConfig();

    // When
    var logger = config.requestLogger();

    // Then
    assertThat(logger).isNotNull();
    assertThat(logger).isInstanceOf(StructuredLoggingConfig.RequestLogger.class);
  }

  @Test
  void shouldCreatePerformanceLogger() {
    // Given
    var config = new StructuredLoggingConfig();

    // When
    var logger = config.performanceLogger();

    // Then
    assertThat(logger).isNotNull();
    assertThat(logger).isInstanceOf(StructuredLoggingConfig.PerformanceLogger.class);
  }

  @Test
  void shouldLogGatewayRequestSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logGatewayRequest("GET", "/api/v1/auth/login", "auth-service", "192.168.1.1", "Mozilla/5.0", "tenant-123");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("method")).isNull();
    assertThat(MDC.get("path")).isNull();
    assertThat(MDC.get("route")).isNull();
  }

  @Test
  void shouldLogGatewayResponseSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logGatewayResponse("GET", "/api/v1/auth/login", "auth-service", 200, 150L, "success");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("method")).isNull();
    assertThat(MDC.get("statusCode")).isNull();
  }

  @Test
  void shouldLogAuthenticationEventSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logAuthenticationEvent("testuser", "success", null, "auth-service");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("username")).isNull();
    assertThat(MDC.get("result")).isNull();
  }

  @Test
  void shouldLogRateLimitEventSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logRateLimitEvent("/api/v1/auth/login", "tenant-123", "allowed", 5, 10);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("endpoint")).isNull();
    assertThat(MDC.get("tenantId")).isNull();
  }

  @Test
  void shouldLogCircuitBreakerEventSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logCircuitBreakerEvent("auth-service", "open", "failure_rate_exceeded");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("service")).isNull();
    assertThat(MDC.get("state")).isNull();
  }

  @Test
  void shouldLogIncomingRequestSuccessfully() {
    // Given
    var requestLogger = new StructuredLoggingConfig.RequestLogger();
    var headers = Map.of("Authorization", "Bearer token", "Content-Type", "application/json");

    // When - Should not throw exceptions
    requestLogger.logIncomingRequest("corr-123", "POST", "/api/v1/auth/login", headers, "{\"username\":\"test\"}");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("requestType")).isNull();
    assertThat(MDC.get("correlationId")).isNull();
    assertThat(MDC.get("method")).isNull();
  }

  @Test
  void shouldLogOutgoingRequestSuccessfully() {
    // Given
    var requestLogger = new StructuredLoggingConfig.RequestLogger();
    var headers = Map.of("X-Forwarded-For", "192.168.1.1");

    // When - Should not throw exceptions
    requestLogger.logOutgoingRequest("corr-123", "POST", "http://auth-service:8080/api/v1/auth/login", "auth-service", headers);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("requestType")).isNull();
    assertThat(MDC.get("correlationId")).isNull();
    assertThat(MDC.get("targetService")).isNull();
  }

  @Test
  void shouldLogResponseSuccessfully() {
    // Given
    var requestLogger = new StructuredLoggingConfig.RequestLogger();
    var headers = Map.of("Content-Type", "application/json");

    // When - Should not throw exceptions
    requestLogger.logResponse("corr-123", 200, 150L, headers, "{\"success\":true}");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("responseType")).isNull();
    assertThat(MDC.get("correlationId")).isNull();
    assertThat(MDC.get("statusCode")).isNull();
  }

  @Test
  void shouldLogSlowRequestSuccessfully() {
    // Given
    var performanceLogger = new StructuredLoggingConfig.PerformanceLogger();

    // When - Should not throw exceptions
    performanceLogger.logSlowRequest("GET", "/api/v1/auth/login", "auth-service", 5000L, "downstream_timeout");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("performanceEvent")).isNull();
    assertThat(MDC.get("method")).isNull();
    assertThat(MDC.get("durationMs")).isNull();
  }

  @Test
  void shouldLogResourceUsageSuccessfully() {
    // Given
    var performanceLogger = new StructuredLoggingConfig.PerformanceLogger();

    // When - Should not throw exceptions
    performanceLogger.logResourceUsage("cpu", 85.5, 80.0, "%");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("performanceEvent")).isNull();
    assertThat(MDC.get("resource")).isNull();
    assertThat(MDC.get("usage")).isNull();
  }

  @Test
  void shouldLogThroughputMetricsSuccessfully() {
    // Given
    var performanceLogger = new StructuredLoggingConfig.PerformanceLogger();

    // When - Should not throw exceptions
    performanceLogger.logThroughputMetrics("/api/v1/auth/login", 150, 200);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("performanceEvent")).isNull();
    assertThat(MDC.get("endpoint")).isNull();
    assertThat(MDC.get("requestsPerSecond")).isNull();
  }
}