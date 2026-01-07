package com.iqscaffold.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import com.iqscaffold.userservice.shared.UserServiceConstants;
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
      MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);
      MDC.put(UserServiceConstants.MDC.REQUEST_ID, requestId);

      // Add to response headers
      response.setHeader(UserServiceConstants.Headers.X_CORRELATION_ID, correlationId);
      response.setHeader(UserServiceConstants.Headers.X_REQUEST_ID, requestId);

      // Continue filter chain
      filterChain.doFilter(request, response);

    } finally {
      // Clean up MDC
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
      MDC.remove(UserServiceConstants.MDC.REQUEST_ID);
    }
  }

  /**
   * Get correlation ID from request header or generate new one.
   */
  private String getOrGenerateCorrelationId(HttpServletRequest request) {
    var correlationId = request.getHeader(UserServiceConstants.Headers.X_CORRELATION_ID);

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
