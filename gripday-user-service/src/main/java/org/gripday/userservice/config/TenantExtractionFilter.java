package org.gripday.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.gripday.userservice.domain.service.TenantContext;
import org.gripday.userservice.domain.service.TenantExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for early tenant context establishment in the filter chain. Extracts tenant information from various sources and sets up tenant context before other filters and controllers are
 * executed.
 */
@Component
@Order(1) // Execute early in the filter chain
public class TenantExtractionFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(TenantExtractionFilter.class);

  private final TenantExtractionService tenantExtractionService;

  public TenantExtractionFilter(final TenantExtractionService tenantExtractionService) {
    this.tenantExtractionService = tenantExtractionService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      // Extract tenant context from request
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      var resolutionResult = tenantExtractionService.extractTenantFromRequest(request, authentication);

      if (resolutionResult.isValid()) {
        // Set tenant context for the current request
        tenantExtractionService.setTenantContext(resolutionResult);

        logger.debug("Tenant context established: {} (method: {}, source: {})",
            resolutionResult.tenantId(),
            resolutionResult.resolutionMethod(),
            resolutionResult.sourceValue());

        // Add tenant information to response headers for debugging
        if (logger.isDebugEnabled()) {
          response.setHeader("X-Resolved-Tenant-ID", resolutionResult.tenantId());
          response.setHeader("X-Tenant-Resolution-Method", resolutionResult.resolutionMethod());
        }
      } else {
        logger.debug("No valid tenant context found for request: {} {}",
            request.getMethod(), request.getRequestURI());
      }

      // Continue with the filter chain
      filterChain.doFilter(request, response);

    } finally {
      // Always clear tenant context after request processing
      TenantContext.clear();
      logger.debug("Tenant context cleared after request processing");
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    var requestURI = request.getRequestURI();

    // Skip tenant extraction for certain paths
    return requestURI.startsWith("/actuator/")
        || requestURI.startsWith("/swagger-ui/")
        || requestURI.startsWith("/v3/api-docs/")
        || requestURI.equals("/favicon.ico")
        || requestURI.equals("/error");
  }
}