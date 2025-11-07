package org.gripday.authservice.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gripday observability features. Provides centralized configuration for tracing, metrics, and logging.
 */
@ConfigurationProperties(prefix = "gripday.observability")
public record GripdayObservabilityProperties(
    TracingProperties tracing,
    MetricsProperties metrics,
    LoggingProperties logging
) {

  public GripdayObservabilityProperties() {
    this(new TracingProperties(), new MetricsProperties(), new LoggingProperties());
  }

  /**
   * OpenTelemetry tracing configuration.
   */
  public record TracingProperties(
      boolean enabled,
      String serviceName,
      double samplingRate,
      String endpoint,
      Duration timeout,
      Duration exportTimeout,
      int batchSize,
      Map<String, String> headers
  ) {

    public TracingProperties() {
      this(true, "gripday-auth-service", 1.0, "http://localhost:4317",
          Duration.ofSeconds(10), Duration.ofSeconds(30), 512, Map.of());
    }
  }

  /**
   * Prometheus metrics configuration.
   */
  public record MetricsProperties(
      boolean enabled,
      String path,
      String prefix,
      boolean includeHostTag,
      boolean includeApplicationTag,
      boolean includeEnvironmentTag,
      Map<String, String> customTags
  ) {

    public MetricsProperties() {
      this(true, "/actuator/prometheus", "gripday_auth", true, true, true, Map.of());
    }
  }

  /**
   * Structured logging configuration.
   */
  public record LoggingProperties(
      boolean includeCorrelationId,
      boolean includeTraceId,
      boolean includeSpanId,
      boolean includeUserId,
      boolean includeTenantId,
      String correlationIdHeader,
      String requestIdHeader,
      String tenantIdHeader
  ) {

    public LoggingProperties() {
      this(true, true, true, true, true, "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID");
    }
  }
}