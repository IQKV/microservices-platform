package com.iqscaffold.gatewayservice.security;

import java.util.List;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.PlatformConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that enforces unified microservice and feature access control.
 * 
 * <p>This filter provides comprehensive access control for all microservices and features
 * by checking user authorities against route-based requirements. It replaces multiple
 * feature-specific filters with a single, configurable, and scalable approach.
 * 
 * <h3>Access Control Features</h3>
 * <ul>
 *   <li><strong>Route-Based Protection</strong> - Maps URL patterns to required authorities</li>
 *   <li><strong>Microservice-Aware</strong> - Understands service boundaries and access</li>
 *   <li><strong>Feature Integration</strong> - Integrates with composable feature system</li>
 *   <li><strong>Admin Override</strong> - ADMIN and SUPER_ADMIN have universal access</li>
 *   <li><strong>Public Routes</strong> - Configurable public access patterns</li>
 * </ul>
 * 
 * <h3>Protected Microservices</h3>
 * <ul>
 *   <li><strong>CRM Services</strong> - contact-service, lead-service, pipeline-service</li>
 *   <li><strong>Billing Service</strong> - billing-service</li>
 *   <li><strong>User Service</strong> - user-service (admin endpoints)</li>
 *   <li><strong>Gateway Service</strong> - API access control</li>
 * </ul>
 * 
 * <h3>Route Protection Patterns</h3>
 * <ul>
 *   <li><strong>CRM Routes</strong> - /api/&#42;/crm/&#42;&#42;, /api/&#42;/leads/&#42;&#42;, /api/&#42;/contacts/&#42;&#42;</li>
 *   <li><strong>Billing Routes</strong> - /api/&#42;/billing/&#42;&#42;, /api/&#42;/payments/&#42;&#42;</li>
 *   <li><strong>Admin Routes</strong> - /api/&#42;/admin/&#42;&#42;, /actuator/&#42;&#42;</li>
 * </ul>
 * 
 * <h3>Authority Hierarchy</h3>
 * <ul>
 *   <li><strong>SUPER_ADMIN</strong> - Universal access to all microservices</li>
 *   <li><strong>ADMIN</strong> - Access to all tenant microservices</li>
 *   <li><strong>Feature Authorities</strong> - Access to specific microservices</li>
 *   <li><strong>USER</strong> - Basic access (user-service only)</li>
 * </ul>
 */
@Component
public class UnifiedMicroserviceAccessFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(UnifiedMicroserviceAccessFilter.class);

  private final PlatformConfigurationProperties platformConfig;

  public UnifiedMicroserviceAccessFilter(final PlatformConfigurationProperties platformConfig) {
    this.platformConfig = platformConfig;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var path = request.getPath().value();

    // Skip access check for public paths
    if (isPublicPath(path)) {
      logger.trace("Skipping access check for public path: {}", path);
      return chain.filter(exchange);
    }

    // Determine required authorities for this route
    var requiredAuthorities = getRequiredAuthorities(path);
    if (requiredAuthorities.isEmpty()) {
      logger.trace("Path does not require specific authorities, continuing: {}", path);
      return chain.filter(exchange);
    }

    // Extract user authorities from headers (set by JWT authentication filter)
    var authoritiesHeader = request.getHeaders().getFirst(GatewayConstants.Headers.X_USER_AUTHORITIES);
    
    if (!StringUtils.hasText(authoritiesHeader)) {
      logger.warn("No user authorities found for protected route: {}", path);
      return unauthorizedResponse(exchange, "Authentication required", "AUTHENTICATION_REQUIRED");
    }

    var userAuthorities = List.of(authoritiesHeader.split(","));
    
    // Check if user has required access
    if (hasRequiredAccess(userAuthorities, requiredAuthorities)) {
      var username = request.getHeaders().getFirst(GatewayConstants.Headers.X_USERNAME);
      logger.debug("Access granted for user: {} on path: {}", username, path);
      return chain.filter(exchange);
    }

    // Access denied
    var username = request.getHeaders().getFirst(GatewayConstants.Headers.X_USERNAME);
    var accessType = getAccessType(path);
    logger.warn("Access denied for user: {} on path: {} (authorities: {})", 
        username, path, userAuthorities);
    
    return unauthorizedResponse(exchange, 
        accessType + " access required", 
        accessType.toUpperCase().replace(" ", "_") + "_REQUIRED");
  }

  /**
   * Check if the path is a public path that should skip access control.
   */
  private boolean isPublicPath(String path) {
    return platformConfig.security().routeProtection().isPublicPath(path);
  }

  /**
   * Get required authorities for a given path based on route patterns from configuration.
   */
  private List<String> getRequiredAuthorities(String path) {
    return platformConfig.security().routeProtection().getRequiredAuthorities(path);
  }

  /**
   * Check if user authorities include any of the required authorities.
   */
  private boolean hasRequiredAccess(List<String> userAuthorities, List<String> requiredAuthorities) {
    return userAuthorities.stream()
        .anyMatch(authority -> requiredAuthorities.contains(authority.trim()));
  }

  /**
   * Extract access type from path for error messages.
   */
  private String getAccessType(String path) {
    if (path.contains("/crm/") || path.contains("/leads/") 
        || path.contains("/contacts/") || path.contains("/pipeline/")) {
      return "CRM";
    }
    if (path.contains("/billing/") || path.contains("/payments/") 
        || path.contains("/subscriptions/") || path.contains("/invoices/")) {
      return "Billing";
    }
    if (path.contains("/admin/") || path.contains("/actuator/")) {
      return "Admin";
    }
    return "Feature";
  }

  /**
   * Return unauthorized response for access denial.
   */
  private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message, String code) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response.getHeaders().add("Content-Type", "application/json");
    
    var body = String.format("""
        {
          "error": "Access Denied",
          "message": "%s",
          "code": "%s",
          "timestamp": "%s"
        }
        """, message, code, java.time.Instant.now());
    
    var buffer = response.bufferFactory().wrap(body.getBytes());
    return response.writeWith(Mono.just(buffer));
  }

  @Override
  public int getOrder() {
    // Run after JWT authentication filter
    return GatewayConstants.FilterOrder.JWT_AUTHENTICATION_FILTER + 10;
  }
}