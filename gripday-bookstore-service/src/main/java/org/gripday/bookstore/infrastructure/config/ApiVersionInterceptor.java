package org.gripday.bookstore.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(ApiVersionInterceptor.class);

  public static final String DEFAULT_VERSION = "1";
  public static final String VERSION_HEADER = "API-Version";
  public static final String VERSION_PARAM = "version";

  private final ApiDeprecationNotice deprecationNotice;

  public ApiVersionInterceptor(final ApiDeprecationNotice deprecationNotice) {
    this.deprecationNotice = deprecationNotice;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    var requestedVersion = extractVersion(request);

    // Set the resolved version in request attributes for controllers to use
    request.setAttribute("apiVersion", requestedVersion);

    // Add version info to response headers
    response.setHeader(VERSION_HEADER, requestedVersion);
    response.setHeader("Supported-Versions", "1");

    // Check for deprecated versions and add warning
    if (deprecationNotice.isVersionDeprecated(requestedVersion)) {
      // For future use when versions become deprecated
      // var migrationInfo = deprecationNotice.getMigrationInfo(requestedVersion);
      // deprecationNotice.addDeprecationHeaders(response, requestedVersion, null, null, migrationInfo);
      logger.warn("Deprecated API version {} requested for path: {}", requestedVersion, request.getRequestURI());
    } else {
      logger.debug("API version {} requested for path: {}", requestedVersion, request.getRequestURI());
    }

    return true;
  }

  private String extractVersion(HttpServletRequest request) {
    // 1. Check API-Version header
    var headerVersion = request.getHeader(VERSION_HEADER);
    if (headerVersion != null && !headerVersion.trim().isEmpty()) {
      return normalizeVersion(headerVersion);
    }

    // 2. Check Accept header for versioned media type
    var acceptHeader = request.getHeader("Accept");
    if (acceptHeader != null) {
      if (acceptHeader.contains("application/vnd.gripday.bookstore.v1+json")) {
        return "1";
      }
      if (acceptHeader.contains("application/vnd.gripday.bookstore.v2+json")) {
        return "2";
      }
    }

    // 3. Check URL path for version (e.g., /api/v1/bookstore/...)
    var requestURI = request.getRequestURI();
    if (requestURI.contains("/v1/")) {
      return "1";
    }
    if (requestURI.contains("/v2/")) {
      return "2";
    }

    // 4. Check query parameter
    var paramVersion = request.getParameter(VERSION_PARAM);
    if (paramVersion != null && !paramVersion.trim().isEmpty()) {
      return normalizeVersion(paramVersion);
    }

    // 5. Default to version 1
    return DEFAULT_VERSION;
  }

  private String normalizeVersion(String version) {
    // Remove 'v' prefix if present and normalize
    var normalized = version.toLowerCase(Locale.ROOT).replaceFirst("^v", "");

    // Validate version format (should be numeric)
    if (normalized.matches("\\d+")) {
      return normalized;
    }

    logger.warn("Invalid version format: {}, defaulting to {}", version, DEFAULT_VERSION);
    return DEFAULT_VERSION;
  }
}