package org.gripday.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ObservabilityProperties Tests")
class ObservabilityPropertiesTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Nested
  @DisplayName("TracingProperties Tests")
  class TracingPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid tracing properties")
    void shouldValidateSuccessfullyWithValidTracingProperties() {
      var tracingProperties = new GripdayProperties.ObservabilityProperties.TracingProperties(
          true,
          "gateway-service",
          0.1,
          "http://jaeger:14250",
          Duration.ofSeconds(5),
          Duration.ofSeconds(10),
          100
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.TracingProperties>> violations = validator.validate(tracingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when serviceName is blank")
    void shouldFailValidationWhenServiceNameIsBlank() {
      var tracingProperties = new GripdayProperties.ObservabilityProperties.TracingProperties(
          true,
          "",
          0.1,
          "http://jaeger:14250",
          Duration.ofSeconds(5),
          Duration.ofSeconds(10),
          100
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.TracingProperties>> violations = validator.validate(tracingProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when samplingRate is below minimum")
    void shouldFailValidationWhenSamplingRateIsBelowMinimum() {
      var tracingProperties = new GripdayProperties.ObservabilityProperties.TracingProperties(
          true,
          "gateway-service",
          -0.1,
          "http://jaeger:14250",
          Duration.ofSeconds(5),
          Duration.ofSeconds(10),
          100
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.TracingProperties>> violations = validator.validate(tracingProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when samplingRate exceeds maximum")
    void shouldFailValidationWhenSamplingRateExceedsMaximum() {
      var tracingProperties = new GripdayProperties.ObservabilityProperties.TracingProperties(
          true,
          "gateway-service",
          1.5,
          "http://jaeger:14250",
          Duration.ofSeconds(5),
          Duration.ofSeconds(10),
          100
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.TracingProperties>> violations = validator.validate(tracingProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("MetricsProperties Tests")
  class MetricsPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid metrics properties")
    void shouldValidateSuccessfullyWithValidMetricsProperties() {
      var metricsProperties = new GripdayProperties.ObservabilityProperties.MetricsProperties(
          true,
          "/actuator/metrics",
          "gateway",
          true,
          true,
          true,
          Map.of("environment", "production"),
          List.of("http.server.requests", "jvm.memory.used")
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.MetricsProperties>> violations = validator.validate(metricsProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when path is blank")
    void shouldFailValidationWhenPathIsBlank() {
      var metricsProperties = new GripdayProperties.ObservabilityProperties.MetricsProperties(
          true,
          "",
          "gateway",
          true,
          true,
          true,
          Map.of(),
          List.of()
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.MetricsProperties>> violations = validator.validate(metricsProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when prefix is blank")
    void shouldFailValidationWhenPrefixIsBlank() {
      var metricsProperties = new GripdayProperties.ObservabilityProperties.MetricsProperties(
          true,
          "/metrics",
          "",
          true,
          true,
          true,
          Map.of(),
          List.of()
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.MetricsProperties>> violations = validator.validate(metricsProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("LoggingProperties Tests")
  class LoggingPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid logging properties")
    void shouldValidateSuccessfullyWithValidLoggingProperties() {
      var loggingProperties = new GripdayProperties.ObservabilityProperties.LoggingProperties(
          "INFO",
          "json",
          true,
          true,
          true,
          true,
          true,
          true,
          true,
          "X-Correlation-ID",
          "X-Request-ID",
          "X-Tenant-ID"
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.LoggingProperties>> violations = validator.validate(loggingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully with DEBUG level")
    void shouldValidateSuccessfullyWithDebugLevel() {
      var loggingProperties = new GripdayProperties.ObservabilityProperties.LoggingProperties(
          "DEBUG",
          "console",
          true,
          true,
          true,
          true,
          true,
          true,
          true,
          "X-Correlation-ID",
          "X-Request-ID",
          "X-Tenant-ID"
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.LoggingProperties>> violations = validator.validate(loggingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid log level")
    void shouldFailValidationWithInvalidLogLevel() {
      var loggingProperties = new GripdayProperties.ObservabilityProperties.LoggingProperties(
          "INVALID",
          "json",
          true,
          true,
          true,
          true,
          true,
          true,
          true,
          "X-Correlation-ID",
          "X-Request-ID",
          "X-Tenant-ID"
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.LoggingProperties>> violations = validator.validate(loggingProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid format")
    void shouldFailValidationWithInvalidFormat() {
      var loggingProperties = new GripdayProperties.ObservabilityProperties.LoggingProperties(
          "INFO",
          "xml",
          true,
          true,
          true,
          true,
          true,
          true,
          true,
          "X-Correlation-ID",
          "X-Request-ID",
          "X-Tenant-ID"
      );

      Set<ConstraintViolation<GripdayProperties.ObservabilityProperties.LoggingProperties>> violations = validator.validate(loggingProperties);

      assertThat(violations).isNotEmpty();
    }
  }
}
