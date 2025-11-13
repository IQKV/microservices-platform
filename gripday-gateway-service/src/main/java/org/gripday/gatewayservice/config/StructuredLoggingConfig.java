package org.gripday.gatewayservice.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for structured logging utilities and helpers in the gateway service. Provides consistent logging patterns for reactive applications and request/response correlation.
 */
@Configuration
@EnableConfigurationProperties(GripdayProperties.class)
public class StructuredLoggingConfig {

  /**
   * Structured logging utility for consistent log formatting in reactive context.
   */
  @Bean
  public StructuredLogger structuredLogger() {
    return new StructuredLogger();
  }

  /**
   * Request/Response logger for gateway operations.
   */
  @Bean
  public RequestLogger requestLogger() {
    return new RequestLogger();
  }

  /**
   * Performance logger for gateway metrics.
   */
  @Bean
  public PerformanceLogger performanceLogger() {
    return new PerformanceLogger();
  }

  /**
   * Structured logging utility implementation for gateway service.
   */
  public static class StructuredLogger {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);

    public void logGatewayRequest(String method, String path, String route, String clientIp,
        String userAgent, String tenantId) {
      try {
        MDC.put("event", "gateway_request");
        MDC.put("method", method);
        MDC.put("path", path);
        MDC.put("route", route);
        MDC.put("clientIp", clientIp);
        MDC.put("userAgent", userAgent);
        if (tenantId != null) {
          MDC.put("tenantId", tenantId);
        }

        logger.info("Gateway request: {} {} -> {}", method, path, route);
      } finally {
        MDC.remove("event");
        MDC.remove("method");
        MDC.remove("path");
        MDC.remove("route");
        MDC.remove("clientIp");
        MDC.remove("userAgent");
        MDC.remove("tenantId");
      }
    }

    public void logGatewayResponse(String method, String path, String route, int statusCode,
        long durationMs, String result) {
      try {
        MDC.put("event", "gateway_response");
        MDC.put("method", method);
        MDC.put("path", path);
        MDC.put("route", route);
        MDC.put("statusCode", String.valueOf(statusCode));
        MDC.put("durationMs", String.valueOf(durationMs));
        MDC.put("result", result);

        if (statusCode >= 400) {
          logger.warn("Gateway response: {} {} -> {} returned {} in {}ms",
              method, path, route, statusCode, durationMs);
        } else {
          logger.info("Gateway response: {} {} -> {} returned {} in {}ms",
              method, path, route, statusCode, durationMs);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("method");
        MDC.remove("path");
        MDC.remove("route");
        MDC.remove("statusCode");
        MDC.remove("durationMs");
        MDC.remove("result");
      }
    }

    public void logAuthenticationEvent(String username, String result, String reason, String route) {
      try {
        MDC.put("event", "gateway_authentication");
        MDC.put("username", username);
        MDC.put("result", result);
        MDC.put("route", route);

        if ("success".equals(result)) {
          logger.info("Gateway authentication successful for user: {} on route: {}", username, route);
        } else {
          MDC.put("reason", reason);
          logger.warn("Gateway authentication failed for user: {} on route: {} - {}",
              username, route, reason);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("username");
        MDC.remove("result");
        MDC.remove("reason");
        MDC.remove("route");
      }
    }

    public void logRateLimitEvent(String endpoint, String tenantId, String result, int currentCount, int limit) {
      try {
        MDC.put("event", "rate_limit");
        MDC.put("endpoint", endpoint);
        MDC.put("tenantId", tenantId != null ? tenantId : "unknown");
        MDC.put("result", result);
        MDC.put("currentCount", String.valueOf(currentCount));
        MDC.put("limit", String.valueOf(limit));

        if ("exceeded".equals(result)) {
          logger.warn("Rate limit exceeded for endpoint: {} tenant: {} ({}/{})",
              endpoint, tenantId, currentCount, limit);
        } else {
          logger.debug("Rate limit check for endpoint: {} tenant: {} ({}/{})",
              endpoint, tenantId, currentCount, limit);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("endpoint");
        MDC.remove("tenantId");
        MDC.remove("result");
        MDC.remove("currentCount");
        MDC.remove("limit");
      }
    }

    public void logCircuitBreakerEvent(String service, String state, String reason) {
      try {
        MDC.put("event", "circuit_breaker");
        MDC.put("service", service);
        MDC.put("state", state);
        MDC.put("reason", reason);

        if ("open".equals(state)) {
          logger.warn("Circuit breaker opened for service: {} - {}", service, reason);
        } else if ("closed".equals(state)) {
          logger.info("Circuit breaker closed for service: {}", service);
        } else {
          logger.info("Circuit breaker state change for service: {} to {} - {}",
              service, state, reason);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("service");
        MDC.remove("state");
        MDC.remove("reason");
      }
    }
  }

  /**
   * Request/Response logger for detailed request tracking.
   */
  public static class RequestLogger {

    private static final Logger requestLogger = LoggerFactory.getLogger("org.gripday.gatewayservice.request");

    public void logIncomingRequest(String correlationId, String method, String path,
        Map<String, String> headers, String body) {
      try {
        MDC.put("requestType", "incoming");
        MDC.put("correlationId", correlationId);
        MDC.put("method", method);
        MDC.put("path", path);

        if (headers != null && !headers.isEmpty()) {
          headers.forEach((key, value) -> MDC.put("header_" + key.toLowerCase(java.util.Locale.ROOT), value));
        }

        requestLogger.info("Incoming request: {} {} with {} headers", method, path,
            headers != null ? headers.size() : 0);

        if (body != null && !body.isEmpty()) {
          requestLogger.debug("Request body: {}", body);
        }
      } finally {
        MDC.remove("requestType");
        MDC.remove("correlationId");
        MDC.remove("method");
        MDC.remove("path");

        if (headers != null) {
          headers.keySet().forEach(key -> MDC.remove("header_" + key.toLowerCase(java.util.Locale.ROOT)));
        }
      }
    }

    public void logOutgoingRequest(String correlationId, String method, String uri,
        String targetService, Map<String, String> headers) {
      try {
        MDC.put("requestType", "outgoing");
        MDC.put("correlationId", correlationId);
        MDC.put("method", method);
        MDC.put("uri", uri);
        MDC.put("targetService", targetService);

        requestLogger.info("Outgoing request: {} {} to service: {}", method, uri, targetService);
      } finally {
        MDC.remove("requestType");
        MDC.remove("correlationId");
        MDC.remove("method");
        MDC.remove("uri");
        MDC.remove("targetService");
      }
    }

    public void logResponse(String correlationId, int statusCode, long durationMs,
        Map<String, String> headers, String body) {
      try {
        MDC.put("responseType", "outgoing");
        MDC.put("correlationId", correlationId);
        MDC.put("statusCode", String.valueOf(statusCode));
        MDC.put("durationMs", String.valueOf(durationMs));

        requestLogger.info("Response: {} in {}ms", statusCode, durationMs);

        if (body != null && !body.isEmpty() && statusCode >= 400) {
          requestLogger.debug("Error response body: {}", body);
        }
      } finally {
        MDC.remove("responseType");
        MDC.remove("correlationId");
        MDC.remove("statusCode");
        MDC.remove("durationMs");
      }
    }
  }

  /**
   * Performance logger for gateway metrics and monitoring.
   */
  public static class PerformanceLogger {

    private static final Logger performanceLogger = LoggerFactory.getLogger("org.gripday.gatewayservice.performance");

    public void logSlowRequest(String method, String path, String route, long durationMs, String reason) {
      try {
        MDC.put("performanceEvent", "slow_request");
        MDC.put("method", method);
        MDC.put("path", path);
        MDC.put("route", route);
        MDC.put("durationMs", String.valueOf(durationMs));
        MDC.put("reason", reason);

        performanceLogger.warn("Slow request detected: {} {} -> {} took {}ms - {}",
            method, path, route, durationMs, reason);
      } finally {
        MDC.remove("performanceEvent");
        MDC.remove("method");
        MDC.remove("path");
        MDC.remove("route");
        MDC.remove("durationMs");
        MDC.remove("reason");
      }
    }

    public void logResourceUsage(String resource, double usage, double threshold, String unit) {
      try {
        MDC.put("performanceEvent", "resource_usage");
        MDC.put("resource", resource);
        MDC.put("usage", String.valueOf(usage));
        MDC.put("threshold", String.valueOf(threshold));
        MDC.put("unit", unit);

        if (usage > threshold) {
          performanceLogger.warn("High resource usage: {} at {}{} (threshold: {}{})",
              resource, usage, unit, threshold, unit);
        } else {
          performanceLogger.debug("Resource usage: {} at {}{}", resource, usage, unit);
        }
      } finally {
        MDC.remove("performanceEvent");
        MDC.remove("resource");
        MDC.remove("usage");
        MDC.remove("threshold");
        MDC.remove("unit");
      }
    }

    public void logThroughputMetrics(String endpoint, int requestsPerSecond, int averageResponseTime) {
      try {
        MDC.put("performanceEvent", "throughput_metrics");
        MDC.put("endpoint", endpoint);
        MDC.put("requestsPerSecond", String.valueOf(requestsPerSecond));
        MDC.put("averageResponseTime", String.valueOf(averageResponseTime));

        performanceLogger.info("Throughput metrics for {}: {} req/s, avg {}ms",
            endpoint, requestsPerSecond, averageResponseTime);
      } finally {
        MDC.remove("performanceEvent");
        MDC.remove("endpoint");
        MDC.remove("requestsPerSecond");
        MDC.remove("averageResponseTime");
      }
    }
  }
}