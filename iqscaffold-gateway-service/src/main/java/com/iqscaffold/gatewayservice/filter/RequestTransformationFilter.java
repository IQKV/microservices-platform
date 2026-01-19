package com.iqscaffold.gatewayservice.filter;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.service.FeatureValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Gateway filter for transforming incoming requests before forwarding to downstream services.
 * Handles header enrichment, user context propagation, and request modification based on
 * IqScaffoldProperties configuration.
 */
@Component
public class RequestTransformationFilter extends AbstractGatewayFilterFactory<RequestTransformationFilter.Config> {

  private static final Logger logger = LoggerFactory.getLogger(RequestTransformationFilter.class);

  private final IqScaffoldProperties properties;

  public RequestTransformationFilter(final IqScaffoldProperties properties) {
    super(Config.class);
    this.properties = properties;
  }

  @Override
  public GatewayFilter apply(Config config) {
    var transformationConfig = properties.gateway().transformation().request();

    if (!transformationConfig.enabled()) {
      logger.debug("Request transformation is disabled");
      return (exchange, chain) -> chain.filter(exchange);
    }

    return (exchange, chain) -> {
      var request = exchange.getRequest();
      var correlationId = MDC.get(GatewayConstants.MdcKeys.CORRELATION_ID);
      var tenantId = MDC.get(GatewayConstants.MdcKeys.TENANT_ID);
      var userId = MDC.get(GatewayConstants.MdcKeys.USER_ID);

      // Get feature context from exchange attributes
      var featureContext = exchange.getAttribute(GatewayConstants.Attributes.FEATURE_CONTEXT);

      // Build transformed request with enriched headers
      var requestBuilder = request.mutate();

      requestBuilder.headers(headers -> {
        // Add correlation ID for distributed tracing
        if (correlationId != null && transformationConfig.enableHeaderEnrichment()) {
          headers.set(GatewayConstants.Headers.X_CORRELATION_ID, correlationId);
        }

        // Add tenant context for multi-tenant support
        if (tenantId != null && transformationConfig.enableTenantContextPropagation()) {
          headers.set(GatewayConstants.Headers.X_TENANT_ID, tenantId);
        }

        // Add user context for authorization
        if (userId != null && transformationConfig.enableUserContextPropagation()) {
          headers.set(GatewayConstants.Headers.X_USER_ID, userId);
        }

        // Add feature context for downstream services
        if (featureContext != null && transformationConfig.enableFeatureContextPropagation()) {
          addFeatureContextHeaders(headers, (FeatureValidationService.FeatureContext) featureContext);
        }

        // Add service identification headers
        if (transformationConfig.enableHeaderEnrichment()) {
          headers.set(GatewayConstants.Headers.X_GATEWAY_SERVICE, GatewayConstants.ServiceInfo.SERVICE_NAME);
          headers.set(GatewayConstants.Headers.X_REQUEST_SOURCE, GatewayConstants.ServiceInfo.REQUEST_SOURCE_VALUE);
          headers.set(GatewayConstants.Headers.X_REQUEST_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        }

        // Remove sensitive headers that shouldn't be forwarded
        transformationConfig.headersToRemove().forEach(headers::remove);

        // Add additional configured headers
        transformationConfig.additionalHeaders().forEach(headers::set);

        logger.debug("Request headers enriched for path: {}", request.getPath());
      });

      // Continue with transformed request
      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    };
  }

  /**
   * Adds feature context headers to the request for downstream services.
   */
  private void addFeatureContextHeaders(HttpHeaders headers, FeatureValidationService.FeatureContext featureContext) {
    // Add enabled features as comma-separated list
    if (!featureContext.getEnabledFeatures().isEmpty()) {
      headers.set(GatewayConstants.Headers.X_ENABLED_FEATURES, 
          String.join(",", featureContext.getEnabledFeatures()));
    }

    // Add plan information
    if (featureContext.getPlanId() != null) {
      headers.set(GatewayConstants.Headers.X_PLAN_ID, featureContext.getPlanId());
    }
    if (featureContext.getPlanName() != null) {
      headers.set(GatewayConstants.Headers.X_PLAN_NAME, featureContext.getPlanName());
    }

    // Add quotas as JSON string
    if (!featureContext.getQuotas().isEmpty()) {
      headers.set(GatewayConstants.Headers.X_FEATURE_QUOTAS, serializeMap(featureContext.getQuotas()));
    }

    // Add limits as JSON string
    if (!featureContext.getLimits().isEmpty()) {
      headers.set(GatewayConstants.Headers.X_FEATURE_LIMITS, serializeMap(featureContext.getLimits()));
    }

    // Add tiers as JSON string
    if (!featureContext.getTiers().isEmpty()) {
      headers.set(GatewayConstants.Headers.X_FEATURE_TIERS, serializeMap(featureContext.getTiers()));
    }
  }

  /**
   * Serializes a map to JSON string for header transmission.
   */
  private String serializeMap(java.util.Map<String, ?> map) {
    try {
      var json = new StringBuilder("{");
      var first = true;
      for (var entry : map.entrySet()) {
        if (!first) {
          json.append(",");
        }
        json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        first = false;
      }
      json.append("}");
      return json.toString();
    } catch (Exception e) {
      logger.warn("Failed to serialize map to JSON: {}", e.getMessage());
      return "{}";
    }
  }

  /**
   * Configuration class for request transformation filter. This is kept for Spring Cloud Gateway filter factory compatibility, but actual configuration comes from IqScaffoldProperties.
   */
  public static class Config {
    // Empty config class - configuration is read from IqScaffoldProperties
  }
}
