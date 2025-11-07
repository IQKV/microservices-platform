package org.gripday.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Early tenant context establishment filter that extracts tenant information from various sources and establishes tenant context for downstream processing.
 */
@Component
public class TenantExtractionFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(TenantExtractionFilter.class);

  private static final String X_TENANT_ID_HEADER = "X-Tenant-ID";
  private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();

    // Extract tenant ID from various sources
    var tenantId = extractTenantId(request);

    if (StringUtils.hasText(tenantId)) {
      // Add tenant context to MDC for structured logging
      MDC.put("tenantId", tenantId);

      // Store tenant context in exchange attributes for downstream filters
      var tenantContext = new TenantContext(tenantId);
      exchange.getAttributes().put(TENANT_CONTEXT_ATTRIBUTE, tenantContext);

      logger.debug("Established tenant context: {}", tenantId);
    } else {
      logger.debug("No tenant context found in request");
    }

    return chain.filter(exchange)
        .doFinally(signalType -> {
          // Clean up MDC after request processing
          MDC.remove("tenantId");
        });
  }

  private String extractTenantId(org.springframework.http.server.reactive.ServerHttpRequest request) {
    // Priority order for tenant extraction:
    // 1. X-Tenant-ID header (explicit tenant specification)
    // 2. Subdomain extraction (tenant1.api.pynity.com -> tenant1)
    // 3. Query parameter (for development/testing)

    // 1. Check X-Tenant-ID header
    var tenantIdHeader = request.getHeaders().getFirst(X_TENANT_ID_HEADER);
    if (StringUtils.hasText(tenantIdHeader)) {
      logger.debug("Tenant ID extracted from header: {}", tenantIdHeader);
      return tenantIdHeader;
    }

    // 2. Extract from subdomain
    var subdomainTenant = extractTenantFromSubdomain(request);
    if (StringUtils.hasText(subdomainTenant)) {
      logger.debug("Tenant ID extracted from subdomain: {}", subdomainTenant);
      return subdomainTenant;
    }

    // 3. Check query parameter (for development/testing)
    var queryTenant = request.getQueryParams().getFirst("tenantId");
    if (StringUtils.hasText(queryTenant)) {
      logger.debug("Tenant ID extracted from query parameter: {}", queryTenant);
      return queryTenant;
    }

    return null;
  }

  private String extractTenantFromSubdomain(org.springframework.http.server.reactive.ServerHttpRequest request) {
    var host = request.getHeaders().getFirst(HttpHeaders.HOST);
    if (StringUtils.hasText(host)) {
      var parts = host.split("\\.");
      if (parts.length > 2) {
        var subdomain = parts[0];
        // Validate subdomain format (alphanumeric and hyphens only)
        if (subdomain.matches("^[a-zA-Z0-9-]+$") && !subdomain.equals("www") && !subdomain.equals("api")) {
          return subdomain;
        }
      }
    }
    return null;
  }

  @Override
  public int getOrder() {
    return -200; // Execute before JWT authentication filter
  }

  /**
   * Tenant context information extracted from request.
   */
  public record TenantContext(
      String tenantId
  ) {

    public boolean isPresent() {
      return StringUtils.hasText(tenantId);
    }
  }
}