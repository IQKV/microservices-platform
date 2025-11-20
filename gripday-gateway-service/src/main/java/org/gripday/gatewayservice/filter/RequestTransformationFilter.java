package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.common.GatewayConstants;
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
      var correlationId = MDC.get(GatewayConstants.MdcKeys.CORRELATION_ID);
      var tenantId = MDC.get(GatewayConstants.MdcKeys.TENANT_ID);
      var userId = MDC.get(GatewayConstants.MdcKeys.USER_ID);

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
   * Configuration class for request transformation filter. This is kept for Spring Cloud Gateway filter factory compatibility, but actual configuration comes from GripdayProperties.
   */
  public static class Config {
    // Empty config class - configuration is read from GripdayProperties
  }
}