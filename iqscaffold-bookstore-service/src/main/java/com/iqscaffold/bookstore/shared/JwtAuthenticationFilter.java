package com.iqscaffold.bookstore.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final UserContextExtractor userContextExtractor;

  public JwtAuthenticationFilter(final UserContextExtractor userContextExtractor) {
    this.userContextExtractor = userContextExtractor;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Generate correlation ID for request tracing
    var correlationId = generateCorrelationId(request);
    MDC.put(BookstoreConstants.MdcKeys.CORRELATION_ID, correlationId);
    response.setHeader(BookstoreConstants.Headers.X_CORRELATION_ID, correlationId);

    try {
      // Extract user context from JWT if authenticated
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication instanceof JwtAuthenticationToken jwtToken) {
        try {
          var userContext = userContextExtractor.extractFromJwt(jwtToken.getToken());
          request.setAttribute(BookstoreConstants.Attributes.USER_CONTEXT, userContext);

          // Add user info to MDC for logging
          MDC.put(BookstoreConstants.MdcKeys.USER_ID, String.valueOf(userContext.userId()));
          MDC.put(BookstoreConstants.MdcKeys.USERNAME, userContext.username());

          logger.debug("User context extracted for user: {} with roles: {}",
              userContext.username(), userContext.roles());

        } catch (final Exception e) {
          logger.warn("Failed to extract user context from JWT", e);
          // Continue without user context for public endpoints
        }
      }

      filterChain.doFilter(request, response);

    } finally {
      // Clean up MDC
      MDC.remove(BookstoreConstants.MdcKeys.CORRELATION_ID);
      MDC.remove(BookstoreConstants.MdcKeys.USER_ID);
      MDC.remove(BookstoreConstants.MdcKeys.USERNAME);
    }
  }

  private String generateCorrelationId(HttpServletRequest request) {
    // Check if correlation ID is already provided in headers
    var existingCorrelationId = request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID);
    if (existingCorrelationId != null && !existingCorrelationId.trim().isEmpty()) {
      return existingCorrelationId;
    }

    // Generate new correlation ID
    return UUID.randomUUID().toString();
  }
}