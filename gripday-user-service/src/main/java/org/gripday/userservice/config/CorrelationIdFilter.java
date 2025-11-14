package org.gripday.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter to add correlation ID to all requests for distributed tracing. Supports both incoming correlation IDs and generates new ones.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";
  private static final String REQUEST_ID_MDC_KEY = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      // Get or generate correlation ID
      var correlationId = getOrGenerateCorrelationId(request);
      var requestId = generateRequestId();

      // Add to MDC for logging
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
      MDC.put(REQUEST_ID_MDC_KEY, requestId);

      // Add to response headers
      response.setHeader(CORRELATION_ID_HEADER, correlationId);
      response.setHeader(REQUEST_ID_HEADER, requestId);

      // Continue filter chain
      filterChain.doFilter(request, response);

    } finally {
      // Clean up MDC
      MDC.remove(CORRELATION_ID_MDC_KEY);
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }

  /**
   * Get correlation ID from request header or generate new one.
   */
  private String getOrGenerateCorrelationId(HttpServletRequest request) {
    var correlationId = request.getHeader(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.trim().isEmpty()) {
      correlationId = generateCorrelationId();
    }

    return correlationId;
  }

  /**
   * Generate new correlation ID.
   */
  private String generateCorrelationId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Generate new request ID.
   */
  private String generateRequestId() {
    return "req-" + UUID.randomUUID().toString().substring(0, 8);
  }
}