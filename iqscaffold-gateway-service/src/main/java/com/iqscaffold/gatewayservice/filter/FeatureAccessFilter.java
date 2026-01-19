package com.iqscaffold.gatewayservice.filter;

import java.util.List;
import java.util.Set;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.service.FeatureValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that enforces feature-based access control.
 *
 * <p>This filter validates that tenants have access to features required by specific endpoints.
 * It runs after tenant extraction and JWT authentication but before rate limiting to ensure
 * feature access is validated early in the request pipeline.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Map endpoints to required features</li>
 *   <li>Validate tenant has access to required features</li>
 *   <li>Block requests to disabled features with 403 Forbidden</li>
 *   <li>Propagate feature context to downstream services</li>
 *   <li>Track feature usage for analytics</li>
 * </ul>
 */
@Component
public class FeatureAccessFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(FeatureAccessFilter.class);

  private final FeatureValidationService featureValidationService;
  private final IqScaffoldProperties properties;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public FeatureAccessFilter(
      final FeatureValidationService featureValidationService,
      final IqScaffoldProperties properties) {
    this.featureValidationService = featureValidationService;
    this.properties = properties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var featureAccessConfig = properties.gateway().featureAccess();

    // Skip feature validation if disabled
    if (!featureAccessConfig.enabled()) {
      logger.debug("Feature access validation is disabled");
      return chain.filter(exchange);
    }

    var request = exchange.getRequest();
    var path = request.getPath().value();
    var method = request.getMethod();

    // Skip validation for excluded paths
    if (isPathExcluded(path, featureAccessConfig.excludedPaths())) {
      logger.debug("Path {} is excluded from feature validation", path);
      return chain.filter(exchange);
    }

    // Get tenant context
    var tenantContext = exchange.getAttribute(GatewayConstants.Attributes.TENANT_CONTEXT);
    if (tenantContext == null) {
      logger.debug("No tenant context found, skipping feature validation for path: {}", path);
      return chain.filter(exchange);
    }

    var tenantId = ((TenantExtractionFilter.TenantContext) tenantContext).tenantId();
    if (!StringUtils.hasText(tenantId)) {
      logger.debug("No tenant ID found, skipping feature validation for path: {}", path);
      return chain.filter(exchange);
    }

    // Find required features for this endpoint
    var requiredFeatures = findRequiredFeatures(path, method != null ? method.name() : "GET", featureAccessConfig);
    if (requiredFeatures.isEmpty()) {
      logger.debug("No feature requirements found for path: {}", path);
      return chain.filter(exchange);
    }

    logger.debug("Validating features {} for tenant {} on path {}", requiredFeatures, tenantId, path);

    // Validate feature access
    return featureValidationService.validateFeatureAccess(tenantId, requiredFeatures, path)
        .flatMap(validationResult -> {
          if (!validationResult.isAllowed()) {
            logger.warn("Feature access denied for tenant {} on path {}: missing features {}",
                tenantId, path, validationResult.getMissingFeatures());
            return handleFeatureAccessDenied(exchange, validationResult);
          }

          // Add feature context to exchange for downstream services
          exchange.getAttributes().put(GatewayConstants.Attributes.FEATURE_CONTEXT, validationResult.getFeatureContext());

          // Add feature context to MDC for logging
          MDC.put(GatewayConstants.MdcKeys.ENABLED_FEATURES,
              String.join(",", validationResult.getFeatureContext().getEnabledFeatures()));

          logger.debug("Feature access granted for tenant {} on path {}", tenantId, path);
          return chain.filter(exchange);
        })
        .onErrorResume(error -> {
          logger.error("Feature validation service error for tenant {} on path {}: {}",
              tenantId, path, error.getMessage(), error);
          // On validation service error, allow the request to proceed (fail-open policy)
          logger.warn("Allowing request to proceed due to feature validation service error");
          return chain.filter(exchange);
        })
        .doFinally(signalType -> {
          // Clean up MDC
          MDC.remove(GatewayConstants.MdcKeys.ENABLED_FEATURES);
        });
  }

  /**
   * Checks if a path is excluded from feature validation.
   */
  private boolean isPathExcluded(String path, List<String> excludedPaths) {
    return excludedPaths.stream()
        .anyMatch(excludedPath -> pathMatcher.match(excludedPath, path));
  }

  /**
   * Finds required features for a given path and method.
   */
  private Set<String> findRequiredFeatures(String path, String method, IqScaffoldProperties.GatewayProperties.FeatureAccessProperties config) {
    return config.mappings().stream()
        .filter(mapping -> matchesPathAndMethod(path, method, mapping))
        .findFirst()
        .map(IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.FeatureMapping::requiredFeatures)
        .orElse(Set.of());
  }

  /**
   * Checks if a path and method match a feature mapping.
   */
  private boolean matchesPathAndMethod(String path, String method,
                                       IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.FeatureMapping mapping) {

    // Check path pattern
    boolean pathMatches = pathMatcher.match(mapping.pathPattern(), path);
    if (!pathMatches) {
      return false;
    }

    // Check method if specified
    if (mapping.methods() != null && !mapping.methods().isEmpty()) {
      return mapping.methods().contains(method.toUpperCase());
    }

    return true;
  }

  /**
   * Handles feature access denied scenarios.
   */
  private Mono<Void> handleFeatureAccessDenied(ServerWebExchange exchange, FeatureValidationService.ValidationResult validationResult) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

    // Create error response
    var errorResponse = createFeatureAccessErrorResponse(validationResult);
    var buffer = response.bufferFactory().wrap(errorResponse.getBytes());

    return response.writeWith(Mono.just(buffer));
  }

  /**
   * Creates a JSON error response for feature access denied.
   */
  private String createFeatureAccessErrorResponse(FeatureValidationService.ValidationResult validationResult) {
    return String.format("""
            {
              "error": "FEATURE_ACCESS_DENIED",
              "message": "Access to this feature is not available in your current subscription plan",
              "details": {
                "missingFeatures": %s,
                "currentPlan": "%s",
                "upgradeRequired": true
              },
              "timestamp": "%s"
            }""",
        formatFeatureList(validationResult.getMissingFeatures()),
        validationResult.getFeatureContext().getPlanName(),
        java.time.Instant.now().toString()
    );
  }

  /**
   * Formats a set of features as a JSON array string.
   */
  private String formatFeatureList(Set<String> features) {
    if (features.isEmpty()) {
      return "[]";
    }
    return "[\"" + String.join("\", \"", features) + "\"]";
  }

  @Override
  public int getOrder() {
    return GatewayConstants.FilterOrder.FEATURE_ACCESS_FILTER;
  }
}
