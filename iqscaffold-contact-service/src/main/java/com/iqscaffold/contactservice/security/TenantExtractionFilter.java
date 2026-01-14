package com.iqscaffold.contactservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.iqscaffold.contactservice.tenancy.TenantContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tenant Extraction Filter that sets tenant context from X-Tenant-ID header.
 *
 * <p>This filter runs with highest precedence to ensure tenant context is set
 * before any other processing occurs. It extracts the tenant ID from the
 * X-Tenant-ID header and establishes the tenant context for the request.
 *
 * <p>This filter is essential for multi-tenant data isolation and runs
 * independently of authentication, making it work in both production and
 * test environments.
 *
 * @see TenantContext
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantExtractionFilter extends OncePerRequestFilter {

  private static final String TENANT_ID_HEADER = "X-Tenant-ID";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    try {
      // Extract tenant ID from X-Tenant-ID header
      String tenantId = request.getHeader(TENANT_ID_HEADER);

      if (StringUtils.hasText(tenantId)) {
        TenantContext.setCurrentTenantId(tenantId.trim());
      }

      filterChain.doFilter(request, response);
    } finally {
      // Clear context to prevent memory leaks in thread pool
      TenantContext.clear();
    }
  }
}
