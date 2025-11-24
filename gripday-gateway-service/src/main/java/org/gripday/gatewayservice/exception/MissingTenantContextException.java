package org.gripday.gatewayservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when tenant context is required but missing from the request.
 */
public class MissingTenantContextException extends ResponseStatusException {

  private final String path;
  private final String reason;

  public MissingTenantContextException(final String path, final String reason) {
    super(HttpStatus.BAD_REQUEST,
        String.format("Missing tenant context for path: %s. %s", path, reason));
    this.path = path;
    this.reason = reason;
  }

  public MissingTenantContextException(final String path) {
    this(path, "X-Tenant-ID header or tenant query parameter is required");
  }

  public String getPath() {
    return path;
  }

  public String getReasonDetail() {
    return reason;
  }
}
