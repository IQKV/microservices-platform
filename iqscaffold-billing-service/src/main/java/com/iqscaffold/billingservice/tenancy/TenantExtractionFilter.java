package com.iqscaffold.billingservice.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for establishing tenant context early in the request processing chain.
 * 
 * <p>Extracts tenant information from the X-Tenant-ID header (set by API Gateway)
 * and establishes tenant context before other filters and controllers execute.
 * The tenant context is automatically cleared after request processing.
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
      // Extract and set tenant context from request
      var tenantSet = tenantExtractionService.extractAndSetTenantContext(request);

      if (tenantSet) {
        var tenantId = TenantContext.getCurrentTenantId();
        logger.debug("Tenant context established: {}", tenantId);

        // Add tenant information to response headers for debugging
        if (logger.isDebugEnabled()) {
          response.setHeader("X-Resolved-Tenant-ID", tenantId);
        }
      } else {
        logger.debug("No tenant context established for request: {} {}",
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
