package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.config.GripdayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Gateway filter for transforming incoming requests before forwarding to downstream services.
 * Handles header enrichment, user context propagation, and request modification based on
 * GripdayProperties configuration.
 */
@Component
public class RequestTransformationFilter extends AbstractGatewayFilterFactory<RequestTransformationFilter.Config> {

  private static final Logger logger = LoggerFactory.getLogger(RequestTransformationFilter.class);

  private final GripdayProperties gripdayProperties;

  public RequestTransformationFilter(final GripdayProperties gripdayProperties) {
    super(Config.class);
    this.gripdayProperties = gripdayProperties;
  }

  @Override
  public GatewayFilter apply(Config config) {
    var transformationConfig = gripdayProperties.gateway().transformation().request();

    if (!transformationConfig.enabled()) {
      logger.debug("Request transformation is disabled");
      return (exchange, chain) -> chain.filter(exchange);
    }

    return (exchange, chain) -> {
      var request = exchange.getRequest();
      var correlationId = MDC.get("correlationId");
      var tenantId = MDC.get("tenantId");
      var userId = MDC.get("userId");

      // Build transformed request with enriched headers
      var requestBuilder = request.mutate();

      requestBuilder.headers(headers -> {
        // Add correlation ID for distributed tracing
        if (correlationId != null && transformationConfig.enableHeaderEnrichment()) {
          headers.set("X-Correlation-ID", correlationId);
        }

        // Add tenant context for multi-tenant support
        if (tenantId != null && transformationConfig.enableTenantContextPropagation()) {
          headers.set("X-Tenant-ID", tenantId);
        }

        // Add user context for authorization
        if (userId != null && transformationConfig.enableUserContextPropagation()) {
          headers.set("X-User-ID", userId);
        }

        // Add service identification headers
        if (transformationConfig.enableHeaderEnrichment()) {
          headers.set("X-Gateway-Service", "gripday-gateway");
          headers.set("X-Request-Source", "gateway");
          headers.set("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()));
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
   * Configuration class for request transformation filter. This is kept for Spring Cloud Gateway filter factory compatibility, but actual configuration comes from GripdayProperties.
   */
  public static class Config {
    // Empty config class - configuration is read from GripdayProperties
  }
}