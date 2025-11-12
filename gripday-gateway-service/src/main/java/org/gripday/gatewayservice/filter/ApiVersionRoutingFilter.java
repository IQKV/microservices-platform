package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.service.ApiVersionExtractor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for API version routing and transformation. Extracts API version from requests and adds version headers for downstream services.
 */
@Component
public class ApiVersionRoutingFilter implements GlobalFilter, Ordered {

  private final ApiVersionExtractor versionExtractor;

  public ApiVersionRoutingFilter(final ApiVersionExtractor versionExtractor) {
    this.versionExtractor = versionExtractor;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var version = versionExtractor.extractVersion(request);

    // Add version header for downstream services
    var modifiedRequest = request.mutate()
        .header("X-API-Version", version)
        .header("X-Requested-Version", version)
        .build();

    // Transform path if needed for version routing
    var transformedPath = transformPathForVersion(request.getPath().value(), version);
    if (!transformedPath.equals(request.getPath().value())) {
      modifiedRequest = modifiedRequest.mutate()
          .path(transformedPath)
          .build();
    }

    var modifiedExchange = exchange.mutate()
        .request(modifiedRequest)
        .build();

    return chain.filter(modifiedExchange);
  }

  /**
   * Transform path for version routing. For backward compatibility, routes older versions to appropriate endpoints.
   */
  public String transformPathForVersion(String originalPath, String version) {
    if (originalPath == null || !originalPath.startsWith("/api/")) {
      return originalPath;
    }

    // If path already contains version, keep it as is
    if (versionExtractor.extractVersionFromPath(originalPath) != null) {
      return originalPath;
    }

    // For unversioned paths, add the detected version
    if (originalPath.startsWith("/api/") && !originalPath.startsWith("/api/v")) {
      return originalPath.replaceFirst("/api/", "/api/" + version + "/");
    }

    return originalPath;
  }

  /**
   * Check if version routing is needed for the path
   */
  public boolean needsVersionRouting(String path) {
    return path != null
        && path.startsWith("/api/")
        && versionExtractor.extractVersionFromPath(path) == null;
  }

  /**
   * Check if the request is for a supported API version
   */
  public boolean isSupportedApiVersion(ServerHttpRequest request) {
    var version = versionExtractor.extractVersion(request);
    return versionExtractor.isSupportedVersion(version);
  }

  @Override
  public int getOrder() {
    // Execute early in the filter chain, after tenant extraction
    return -100;
  }
}