package org.gripday.gatewayservice.service;

import org.gripday.gatewayservice.config.GripdayProperties;
import org.springframework.stereotype.Service;

/**
 * Service for handling API prefix operations and path transformations.
 * Provides utilities for working with environment-specific API prefixes.
 */
@Service
public class ApiPrefixService {

  private final GripdayProperties gripdayProperties;

  public ApiPrefixService(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }

  /**
   * Get the configured API prefix.
   *
   * @return the API prefix (e.g., "/api" or empty string)
   */
  public String getPrefix() {
    return gripdayProperties.gateway().routing().apiPrefix().prefix();
  }

  /**
   * Get the strip count for the API prefix.
   *
   * @return number of path segments to strip
   */
  public int getStripCount() {
    return gripdayProperties.gateway().routing().apiPrefix().stripCount();
  }

  /**
   * Check if API prefix handling is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return gripdayProperties.gateway().routing().apiPrefix().enabled();
  }

  /**
   * Build a full path by combining the prefix with a service path.
   *
   * @param servicePath the service path (e.g., "/v1/auth/**")
   * @return the full path with prefix (e.g., "/api/v1/auth/**")
   */
  public String buildFullPath(String servicePath) {
    if (!isEnabled() || getPrefix().isEmpty()) {
      return servicePath;
    }
    return getPrefix() + servicePath;
  }

  /**
   * Strip the prefix from a path if configured.
   *
   * @param path the full path
   * @return the path with prefix stripped
   */
  public String stripPrefix(String path) {
    if (!isEnabled() || getStripCount() == 0 || getPrefix().isEmpty()) {
      return path;
    }

    var segments = path.split("/");
    var stripCount = Math.min(getStripCount(), segments.length - 1);
    
    if (stripCount <= 0) {
      return path;
    }

    var result = new StringBuilder();
    for (var i = stripCount + 1; i < segments.length; i++) {
      result.append("/").append(segments[i]);
    }

    return result.length() == 0 ? "/" : result.toString();
  }

  /**
   * Check if a path matches the configured prefix.
   *
   * @param path the path to check
   * @return true if the path starts with the configured prefix
   */
  public boolean hasPrefix(String path) {
    if (!isEnabled() || getPrefix().isEmpty()) {
      return false;
    }
    return path.startsWith(getPrefix());
  }

  /**
   * Normalize a path by ensuring it starts with a forward slash.
   *
   * @param path the path to normalize
   * @return the normalized path
   */
  public String normalizePath(String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  /**
   * Get the API prefix configuration details as a formatted string.
   *
   * @return configuration details
   */
  public String getConfigurationDetails() {
    return String.format("ApiPrefix[enabled=%s, prefix='%s', stripCount=%d]",
        isEnabled(), getPrefix(), getStripCount());
  }
}
