package org.gripday.gatewayservice.filter;

import java.util.List;
import java.util.Map;

import org.gripday.gatewayservice.common.GatewayConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Gateway filter for transforming outgoing responses before returning to clients. Handles header enrichment, security headers, and response modification.
 */
@Component
public class ResponseTransformationFilter extends AbstractGatewayFilterFactory<ResponseTransformationFilter.Config> {

  private static final Logger logger = LoggerFactory.getLogger(ResponseTransformationFilter.class);

  public ResponseTransformationFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      return chain.filter(exchange).then(Mono.fromRunnable(() -> {
        var response = exchange.getResponse();
        var correlationId = MDC.get(GatewayConstants.MdcKeys.CORRELATION_ID);
        var requestId = MDC.get(GatewayConstants.MdcKeys.REQUEST_ID);

        // Add security headers
        addSecurityHeaders(response);

        // Add correlation and request tracking headers
        if (correlationId != null) {
          response.getHeaders().set(GatewayConstants.Headers.X_CORRELATION_ID, correlationId);
        }

        if (requestId != null) {
          response.getHeaders().set(GatewayConstants.Headers.X_REQUEST_ID, requestId);
        }

        // Add gateway identification
        response.getHeaders().set(GatewayConstants.Headers.X_GATEWAY_SERVICE, GatewayConstants.ServiceInfo.SERVICE_NAME);
        response.getHeaders().set(GatewayConstants.Headers.X_RESPONSE_SOURCE, GatewayConstants.ServiceInfo.RESPONSE_SOURCE_VALUE);

        // Add response timestamp for monitoring
        response.getHeaders().set(GatewayConstants.Headers.X_RESPONSE_TIMESTAMP, String.valueOf(System.currentTimeMillis()));

        // Remove internal headers that shouldn't be exposed
        removeInternalHeaders(response);

        logger.debug("Response headers enriched for status: {}", response.getStatusCode());
      }));
    };
  }

  /**
   * Add security headers to the response.
   */
  private void addSecurityHeaders(ServerHttpResponse response) {
    var headers = response.getHeaders();

    // Content Security Policy
    headers.set(GatewayConstants.Headers.CONTENT_SECURITY_POLICY, GatewayConstants.SecurityHeaderValues.CSP_DEFAULT);

    // X-Frame-Options to prevent clickjacking
    headers.set(GatewayConstants.Headers.X_FRAME_OPTIONS, GatewayConstants.SecurityHeaderValues.X_FRAME_OPTIONS_DENY);

    // X-Content-Type-Options to prevent MIME sniffing
    headers.set(GatewayConstants.Headers.X_CONTENT_TYPE_OPTIONS, GatewayConstants.SecurityHeaderValues.X_CONTENT_TYPE_OPTIONS_NOSNIFF);

    // X-XSS-Protection
    headers.set(GatewayConstants.Headers.X_XSS_PROTECTION, GatewayConstants.SecurityHeaderValues.X_XSS_PROTECTION_BLOCK);

    // Referrer Policy
    headers.set(GatewayConstants.Headers.REFERRER_POLICY, GatewayConstants.SecurityHeaderValues.REFERRER_POLICY_STRICT);

    // Strict Transport Security (HTTPS only)
    if (isHttpsRequest()) {
      headers.set(GatewayConstants.Headers.STRICT_TRANSPORT_SECURITY, GatewayConstants.SecurityHeaderValues.HSTS_MAX_AGE);
    }

    // Cache control for sensitive endpoints
    if (isSensitiveEndpoint()) {
      headers.set(GatewayConstants.Headers.CACHE_CONTROL, GatewayConstants.SecurityHeaderValues.CACHE_CONTROL_NO_CACHE);
      headers.set(GatewayConstants.Headers.PRAGMA, GatewayConstants.SecurityHeaderValues.PRAGMA_NO_CACHE);
      headers.set(GatewayConstants.Headers.EXPIRES, GatewayConstants.SecurityHeaderValues.EXPIRES_IMMEDIATE);
    }
  }

  /**
   * Remove internal headers that shouldn't be exposed to clients.
   */
  private void removeInternalHeaders(ServerHttpResponse response) {
    var headers = response.getHeaders();

    // Remove internal service headers
    headers.remove(GatewayConstants.Headers.X_INTERNAL_SERVICE);
    headers.remove(GatewayConstants.Headers.X_INTERNAL_VERSION);
    headers.remove(GatewayConstants.Headers.X_DATABASE_QUERY_TIME);
    headers.remove(GatewayConstants.Headers.X_CACHE_STATUS_INTERNAL);

    // Remove server information for security
    headers.remove(GatewayConstants.Headers.SERVER);
    headers.remove(GatewayConstants.Headers.X_POWERED_BY);
  }

  /**
   * Check if the current request is over HTTPS.
   */
  private boolean isHttpsRequest() {
    // In a real implementation, this would check the actual request scheme
    // For now, return false as we're in development mode
    return false;
  }

  /**
   * Check if the current endpoint is sensitive and requires strict caching headers.
   */
  private boolean isSensitiveEndpoint() {
    // Check if the endpoint contains sensitive data
    var path = MDC.get(GatewayConstants.MdcKeys.REQUEST_PATH);
    if (path != null) {
      return path.contains(GatewayConstants.SensitivePaths.AUTH_PATH)
             || path.contains(GatewayConstants.SensitivePaths.USERS_PATH)
             || path.contains(GatewayConstants.SensitivePaths.ADMIN_PATH);
    }
    return false;
  }

  /**
   * Configuration class for response transformation filter.
   */
  public static class Config {

    private boolean enableSecurityHeaders = true;
    private boolean enableCorrelationHeaders = true;
    private boolean removeInternalHeaders = true;
    private List<String> additionalHeadersToRemove = List.of();
    private Map<String, String> additionalHeaders = Map.of();

    public boolean isEnableSecurityHeaders() {
      return enableSecurityHeaders;
    }

    public void setEnableSecurityHeaders(boolean enableSecurityHeaders) {
      this.enableSecurityHeaders = enableSecurityHeaders;
    }

    public boolean isEnableCorrelationHeaders() {
      return enableCorrelationHeaders;
    }

    public void setEnableCorrelationHeaders(boolean enableCorrelationHeaders) {
      this.enableCorrelationHeaders = enableCorrelationHeaders;
    }

    public boolean isRemoveInternalHeaders() {
      return removeInternalHeaders;
    }

    public void setRemoveInternalHeaders(boolean removeInternalHeaders) {
      this.removeInternalHeaders = removeInternalHeaders;
    }

    public List<String> getAdditionalHeadersToRemove() {
      return additionalHeadersToRemove;
    }

    public void setAdditionalHeadersToRemove(List<String> additionalHeadersToRemove) {
      this.additionalHeadersToRemove = additionalHeadersToRemove;
    }

    public Map<String, String> getAdditionalHeaders() {
      return additionalHeaders;
    }

    public void setAdditionalHeaders(Map<String, String> additionalHeaders) {
      this.additionalHeaders = additionalHeaders;
    }
  }
}
