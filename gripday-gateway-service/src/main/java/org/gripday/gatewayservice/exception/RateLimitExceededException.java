package org.gripday.gatewayservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when rate limit is exceeded for a tenant or global request.
 */
public class RateLimitExceededException extends ResponseStatusException {

  private final String tenantId;
  private final String path;
  private final RateLimitType type;
  private final int retryAfterSeconds;

  public RateLimitExceededException(String tenantId, String path, RateLimitType type, int retryAfterSeconds) {
    super(HttpStatus.TOO_MANY_REQUESTS, buildMessage(tenantId, type));
    this.tenantId = tenantId;
    this.path = path;
    this.type = type;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  private static String buildMessage(String tenantId, RateLimitType type) {
    return switch (type) {
      case GLOBAL -> "Global rate limit exceeded";
      case TENANT -> String.format("Tenant rate limit exceeded for tenant: %s", tenantId);
      case ENDPOINT -> String.format("Endpoint rate limit exceeded for tenant: %s", tenantId);
    };
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getPath() {
    return path;
  }

  public RateLimitType getType() {
    return type;
  }

  public int getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  public enum RateLimitType {
    GLOBAL,
    TENANT,
    ENDPOINT
  }
}
