package com.iqscaffold.gatewayservice.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when an unsupported API version is requested.
 */
public class UnsupportedApiVersionException extends ResponseStatusException {

  private final String requestedVersion;
  private final List<String> supportedVersions;

  public UnsupportedApiVersionException(final String requestedVersion, final List<String> supportedVersions) {
    super(HttpStatus.BAD_REQUEST,
        String.format("Unsupported API version: %s. Supported versions: %s",
            requestedVersion, String.join(", ", supportedVersions)));
    this.requestedVersion = requestedVersion;
    this.supportedVersions = supportedVersions;
  }

  public String getRequestedVersion() {
    return requestedVersion;
  }

  public List<String> getSupportedVersions() {
    return supportedVersions;
  }
}
