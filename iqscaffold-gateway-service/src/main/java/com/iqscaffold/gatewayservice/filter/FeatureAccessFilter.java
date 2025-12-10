package com.iqscaffold.gatewayservice.filter;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.exception.FeatureNotAvailableException;
import com.iqscaffold.gatewayservice.service.BillingServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter to check feature access based on tenant subscription plan.
 * Integrates with billing service to verify if the tenant has access to features required by the endpoint.
 */
@Component
public class FeatureAccessFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(FeatureAccessFilter.class);

  private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";

  private final IqScaffoldProperties properties;
  private final BillingServiceClient billingServiceClient;

  public FeatureAccessFilter(final IqScaffoldProperties properties, final BillingServiceClient billingServiceClient) {
    this.properties = properties;
    this.billingServiceClient = billingServiceClient;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!isEnabled()) {
      return chain.filter(exchange);
    }

    var request = exchange.getRequest();
    var path = request.getPath().value();

    // Get tenant context from exchange attributes
    var tenantContext = (TenantExtractionFilter.TenantContext)
        exchange.getAttributes().get(TENANT_CONTEXT_ATTRIBUTE);

    if (tenantContext == null) {
      logger.debug("No tenant context found, skipping feature access check for path: {}", path);
      return chain.filter(exchange);
    }

    var tenantId = tenantContext.tenantId();

    // Determine required feature for the endpoint
    var requiredFeature = determineRequiredFeature(path);

    if (requiredFeature == null) {
      // No feature requirement for this endpoint
      logger.debug("No feature requirement for path: {}", path);
      return chain.filter(exchange);
    }

    logger.debug("Checking feature access for tenant: {}, path: {}, required feature: {}",
        tenantId, path, requiredFeature);

    // Check feature access with billing service
    return billingServiceClient.checkFeatureAccess(tenantId, requiredFeature)
        .flatMap(response -> {
          if (response.available()) {
            logger.debug("Feature access granted for tenant: {}, feature: {}", tenantId, requiredFeature);
            return chain.filter(exchange);
          } else {
            logger.warn("Feature access denied for tenant: {}, feature: {}, plan: {}",
                tenantId, requiredFeature, response.planTier());
            return Mono.error(new FeatureNotAvailableException(tenantId, requiredFeature, path));
          }
        });
  }

  /**
   * Determine the required feature code for a given endpoint path.
   * Maps endpoint paths to feature codes that must be checked.
   *
   * @param path the request path
   * @return the required feature code, or null if no feature check is needed
   */
  private String determineRequiredFeature(String path) {
    if (!isEnabled() || properties.gateway().featureAccess() == null) {
      return null;
    }

    var featureMapping = properties.gateway().featureAccess().endpointFeatureMapping();
    if (featureMapping == null || featureMapping.isEmpty()) {
      return null;
    }

    // Check for exact path match first
    var exactMatch = featureMapping.get(path);
    if (exactMatch != null) {
      return exactMatch;
    }

    // Check for pattern matches
    for (final var entry : featureMapping.entrySet()) {
      var pattern = entry.getKey();
      if (pathMatches(path, pattern)) {
        return entry.getValue();
      }
    }

    return null;
  }

  private boolean pathMatches(String path, String pattern) {
    if (pattern.endsWith("/**")) {
      var prefix = pattern.substring(0, pattern.length() - 3);
      return path.startsWith(prefix);
    }
    if (pattern.contains("*")) {
      // Simple wildcard matching
      var regex = pattern.replace("*", ".*");
      return path.matches(regex);
    }
    return path.equals(pattern);
  }

  private boolean isEnabled() {
    return properties.gateway().featureAccess() != null
           && properties.gateway().featureAccess().enabled();
  }

  @Override
  public int getOrder() {
    return -40; // Execute after authentication (-100) and rate limiting (-50), but before routing
  }
}
