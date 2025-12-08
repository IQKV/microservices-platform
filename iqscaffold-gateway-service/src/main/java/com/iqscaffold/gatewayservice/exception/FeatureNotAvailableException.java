package com.iqscaffold.gatewayservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when a requested feature is not available in the tenant's subscription plan.
 */
public class FeatureNotAvailableException extends ResponseStatusException {

  private final String tenantId;
  private final String featureCode;
  private final String path;

  public FeatureNotAvailableException(final String tenantId, final String featureCode, final String path) {
    super(HttpStatus.FORBIDDEN, buildMessage(featureCode));
    this.tenantId = tenantId;
    this.featureCode = featureCode;
    this.path = path;
  }

  private static String buildMessage(String featureCode) {
    return String.format("Feature '%s' is not available in your subscription plan. Please upgrade to access this feature.", featureCode);
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getFeatureCode() {
    return featureCode;
  }

  public String getPath() {
    return path;
  }
}
