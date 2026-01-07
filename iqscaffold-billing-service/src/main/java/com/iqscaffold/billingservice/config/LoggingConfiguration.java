package com.iqscaffold.billingservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Configuration for structured logging with correlation ID tracking.
 * Enables JSON logging with MDC context for distributed tracing.
 */
@Configuration
@EnableConfigurationProperties(IqScaffoldProperties.class)
public class LoggingConfiguration {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingConfiguration.class);

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String TENANT_ID_HEADER = "X-Tenant-ID";
  private static final String USER_ID_HEADER = "X-User-ID";

  /**
   * Filter to add correlation ID and other context to MDC for structured logging.
   */
  @Bean
  @Order(1)
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  /**
   * Custom filter to handle correlation ID and MDC context.
   */
  public static class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
      try {
        // Extract or generate correlation ID
        String correlationId = extractOrGenerateCorrelationId(request);
        String requestId = extractOrGenerateRequestId(request);
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);

        // Add to MDC for structured logging
        MDC.put("correlationId", correlationId);
        MDC.put("requestId", requestId);

        if (StringUtils.hasText(tenantId)) {
          MDC.put("tenantId", tenantId);
        }

        if (StringUtils.hasText(userId)) {
          MDC.put("userId", userId);
        }

        // Add request context
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        MDC.put("remoteAddr", getClientIpAddress(request));

        // Add correlation ID to response headers
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        filterChain.doFilter(request, response);
      } finally {
        // Clean up MDC to prevent memory leaks
        MDC.clear();
      }
    }

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
      String correlationId = request.getHeader(CORRELATION_ID_HEADER);
      if (!StringUtils.hasText(correlationId)) {
        correlationId = UUID.randomUUID().toString();
      }
      return correlationId;
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
      String requestId = request.getHeader(REQUEST_ID_HEADER);
      if (!StringUtils.hasText(requestId)) {
        requestId = UUID.randomUUID().toString();
      }
      return requestId;
    }

    private String getClientIpAddress(HttpServletRequest request) {
      String xforwardedfor = request.getHeader("X-Forwarded-For");
      if (StringUtils.hasText(xforwardedfor)) {
        return xforwardedfor.split(",")[0].trim();
      }

      String xrealip = request.getHeader("X-Real-IP");
      if (StringUtils.hasText(xrealip)) {
        return xrealip;
      }

      return request.getRemoteAddr();
    }
  }

  /**
   * Utility class for structured logging in business logic.
   */
  public static class StructuredLogger {

    /**
     * Log security events with structured context.
     */
    public static void logSecurityEvent(String event, String userId, String tenantId, Object details) {
      org.slf4j.Logger securityLogger = org.slf4j.LoggerFactory.getLogger("SECURITY");

      MDC.put("eventType", "security");
      MDC.put("securityEvent", event);
      if (userId != null) {
        MDC.put("userId", userId);
      }
      if (tenantId != null) {
        MDC.put("tenantId", tenantId);
      }

      securityLogger.info("Security event: {} - Details: {}", event, details);
    }

    /**
     * Log payment events with structured context.
     */
    public static void logPaymentEvent(String event, String paymentId, String tenantId, Object details) {
      org.slf4j.Logger paymentLogger = org.slf4j.LoggerFactory.getLogger("PAYMENT");

      MDC.put("eventType", "payment");
      MDC.put("paymentEvent", event);
      MDC.put("paymentId", paymentId);
      if (tenantId != null) {
        MDC.put("tenantId", tenantId);
      }

      paymentLogger.info("Payment event: {} - Payment ID: {} - Details: {}", event, paymentId, details);
    }

    /**
     * Log billing events with structured context.
     */
    public static void logBillingEvent(String event, String entityId, String tenantId, Object details) {
      org.slf4j.Logger billingLogger = org.slf4j.LoggerFactory.getLogger("BILLING");

      MDC.put("eventType", "billing");
      MDC.put("billingEvent", event);
      MDC.put("entityId", entityId);
      if (tenantId != null) {
        MDC.put("tenantId", tenantId);
      }

      billingLogger.info("Billing event: {} - Entity ID: {} - Details: {}", event, entityId, details);
    }

    /**
     * Log performance metrics with structured context.
     */
    public static void logPerformanceMetric(String operation, long durationMs, Object context) {
      org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("PERFORMANCE");

      MDC.put("eventType", "performance");
      MDC.put("operation", operation);
      MDC.put("durationMs", String.valueOf(durationMs));

      logger.info("Performance metric - Operation: {} - Duration: {}ms - Context: {}",
          operation, durationMs, context);
    }
  }
}
