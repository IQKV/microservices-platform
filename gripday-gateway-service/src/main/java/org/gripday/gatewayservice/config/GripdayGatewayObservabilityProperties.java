package org.gripday.gatewayservice.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gripday gateway observability features. Provides centralized configuration for tracing, metrics, and logging in reactive applications.
 */
@ConfigurationProperties(prefix = "gripday.observability")
public record GripdayGatewayObservabilityProperties(
    TracingProperties tracing,
    MetricsProperties metrics,
    LoggingProperties logging
) {

  public GripdayGatewayObservabilityProperties() {
    this(new TracingProperties(), new MetricsProperties(), new LoggingProperties());
  }

  /**
   * OpenTelemetry tracing configuration for reactive applications.
   */
  public record TracingProperties(
      boolean enabled,
      String serviceName,
      double samplingRate,
      String endpoint,
      Duration timeout,
      Duration exportTimeout,
      int batchSize,
      Map<String, String> headers,
      List<String> excludedPaths
  ) {

    public TracingProperties() {
      this(true, "gripday-gateway-service", 1.0, "http://localhost:4317", Duration.ofSeconds(10),
          Duration.ofSeconds(30), 512, Map.of(), List.of("/actuator/**", "/health"));
    }
  }

  /**
   * Prometheus metrics configuration for gateway.
   */
  public record MetricsProperties(
      boolean enabled,
      String path,
      String prefix,
      boolean includeHostTag,
      boolean includeApplicationTag,
      boolean includeEnvironmentTag,
      Map<String, String> customTags,
      List<String> enabledMetrics
  ) {

    public MetricsProperties() {
      this(true, "/actuator/prometheus", "gripday_gateway", true, true, true, Map.of(),
          List.of("gateway.requests", "gateway.response.time", "gateway.circuit.breaker", "gateway.rate.limit"));
    }
  }

  /**
   * Structured logging configuration for reactive applications.
   */
  public record LoggingProperties(
      String level,
      boolean enableRequestLogging,
      boolean enableResponseLogging,
      boolean enableCorrelationId,
      boolean includeTraceId,
      boolean includeSpanId,
      boolean includeUserId,
      boolean includeTenantId,
      String correlationIdHeader,
      String requestIdHeader,
      String tenantIdHeader
  ) {

    public LoggingProperties() {
      this("INFO", true, false, true, true, true, true, true,
          "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID");
    }
  }
}