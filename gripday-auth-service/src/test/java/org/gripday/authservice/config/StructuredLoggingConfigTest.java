package org.gripday.authservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Test for structured logging configuration.
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
  void shouldCreateSecurityLogger() {
    // Given
    var config = new StructuredLoggingConfig();

    // When
    var logger = config.securityLogger();

    // Then
    assertThat(logger).isNotNull();
    assertThat(logger).isInstanceOf(StructuredLoggingConfig.SecurityLogger.class);
  }

  @Test
  void shouldLogAuthenticationAttemptSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logAuthenticationAttempt("testuser", "success", null, "192.168.1.1", "Mozilla/5.0");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("username")).isNull();
    assertThat(MDC.get("result")).isNull();
  }

  @Test
  void shouldLogUserRegistrationSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logUserRegistration("testuser", "test@example.com", "success", null);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("username")).isNull();
    assertThat(MDC.get("email")).isNull();
  }

  @Test
  void shouldLogTokenOperationSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logTokenOperation("refresh", "testuser", "success", "access_token");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("operation")).isNull();
    assertThat(MDC.get("username")).isNull();
  }

  @Test
  void shouldLogDatabaseOperationSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();

    // When - Should not throw exceptions
    logger.logDatabaseOperation("SELECT", "users", 150L, "success");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("operation")).isNull();
    assertThat(MDC.get("table")).isNull();
  }

  @Test
  void shouldLogBusinessEventSuccessfully() {
    // Given
    var logger = new StructuredLoggingConfig.StructuredLogger();
    var context = Map.of("userId", "123", "action", "profile_update");

    // When - Should not throw exceptions
    logger.logBusinessEvent("user_profile_updated", context);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("event")).isNull();
    assertThat(MDC.get("userId")).isNull();
    assertThat(MDC.get("action")).isNull();
  }

  @Test
  void shouldLogSecurityEventSuccessfully() {
    // Given
    var securityLogger = new StructuredLoggingConfig.SecurityLogger();
    var additionalContext = Map.of("attemptCount", "3");

    // When - Should not throw exceptions
    securityLogger.logSecurityEvent("login_attempt", "testuser", "authenticate", "success",
        "192.168.1.1", "Mozilla/5.0", additionalContext);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("securityEvent")).isNull();
    assertThat(MDC.get("username")).isNull();
    assertThat(MDC.get("action")).isNull();
  }

  @Test
  void shouldLogAccountLockoutSuccessfully() {
    // Given
    var securityLogger = new StructuredLoggingConfig.SecurityLogger();

    // When - Should not throw exceptions
    securityLogger.logAccountLockout("testuser", "too_many_attempts", "192.168.1.1", 5);

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("securityEvent")).isNull();
    assertThat(MDC.get("username")).isNull();
    assertThat(MDC.get("reason")).isNull();
  }

  @Test
  void shouldLogPrivilegeEscalationSuccessfully() {
    // Given
    var securityLogger = new StructuredLoggingConfig.SecurityLogger();

    // When - Should not throw exceptions
    securityLogger.logPrivilegeEscalation("testuser", "USER", "ADMIN", "admin@example.com");

    // Then - MDC should be cleaned up after logging
    assertThat(MDC.get("securityEvent")).isNull();
    assertThat(MDC.get("username")).isNull();
    assertThat(MDC.get("fromRole")).isNull();
  }
}