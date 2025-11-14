package org.gripday.gatewayservice.service;

import java.util.regex.Pattern;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for extracting API version from HTTP requests. Supports URL path-based and header-based version detection.
 */
@Service
public class ApiVersionExtractor {

  private static final String DEFAULT_VERSION = "v1";
  private static final Pattern URL_VERSION_PATTERN = Pattern.compile("/api/(v\\d+)/.*");
  private static final String API_VERSION_HEADER = "API-Version";
  private static final String ACCEPT_HEADER = "Accept";
  private static final Pattern ACCEPT_VERSION_PATTERN = Pattern.compile("application/vnd\\.gripday\\.(v\\d+)\\+json");

  /**
   * Extract API version from the request using multiple strategies:
   * 1. URL path-based versioning (/api/v1/admin/users)
   * 2. API-Version header
   * 3. Accept header with vendor media type
   * 4. Default to v1 if no version found
   *
   */
  public String extractVersion(ServerHttpRequest request) {
    // Try URL path-based version first
    var pathVersion = extractVersionFromPath(request.getPath().value());
    if (StringUtils.hasText(pathVersion)) {
      return pathVersion;
    }

    // Try API-Version header
    var headerVersion = extractVersionFromHeader(request);
    if (StringUtils.hasText(headerVersion)) {
      return headerVersion;
    }

    // Try Accept header with vendor media type
    var acceptVersion = extractVersionFromAcceptHeader(request);
    if (StringUtils.hasText(acceptVersion)) {
      return acceptVersion;
    }

    // Default version
    return DEFAULT_VERSION;
  }

  /**
   * Extract version from URL path like /api/v1/admin/users -> v1
   */
  public String extractVersionFromPath(String path) {
    if (!StringUtils.hasText(path)) {
      return null;
    }

    var matcher = URL_VERSION_PATTERN.matcher(path);
    if (matcher.matches()) {
      return matcher.group(1);
    }

    return null;
  }

  /**
   * Extract version from API-Version header
   */
  public String extractVersionFromHeader(ServerHttpRequest request) {
    var apiVersionHeader = request.getHeaders().getFirst(API_VERSION_HEADER);
    if (StringUtils.hasText(apiVersionHeader)) {
      // Support formats like "1.0", "2.0" -> convert to "v1", "v2"
      if (apiVersionHeader.matches("\\d+\\.\\d+")) {
        var majorVersion = apiVersionHeader.split("\\.")[0];
        return "v" + majorVersion;
      }
      // Support direct format like "v1", "v2"
      if (apiVersionHeader.matches("v\\d+")) {
        return apiVersionHeader;
      }
    }

    return null;
  }

  /**
   * Extract version from Accept header with vendor media type Example: application/vnd.gripday.v1+json -> v1
   */
  public String extractVersionFromAcceptHeader(ServerHttpRequest request) {
    var acceptHeader = request.getHeaders().getFirst(ACCEPT_HEADER);
    if (StringUtils.hasText(acceptHeader)) {
      var matcher = ACCEPT_VERSION_PATTERN.matcher(acceptHeader);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    return null;
  }

  /**
   * Check if the version is supported
   */
  public boolean isSupportedVersion(String version) {
    return StringUtils.hasText(version)
        && (version.equals("v1") || version.equals("v2"));
  }

  /**
   * Get the default version
   */
  public String getDefaultVersion() {
    return DEFAULT_VERSION;
  }
}
