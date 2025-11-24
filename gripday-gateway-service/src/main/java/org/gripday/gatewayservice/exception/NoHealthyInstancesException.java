package org.gripday.gatewayservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when no healthy service instances are available for load balancing.
 */
public class NoHealthyInstancesException extends ResponseStatusException {

  private final String serviceName;
  private final int totalInstances;

  public NoHealthyInstancesException(final String serviceName, final int totalInstances) {
    super(HttpStatus.SERVICE_UNAVAILABLE,
        String.format("No healthy instances available for service: %s (total instances: %d)", serviceName, totalInstances));
    this.serviceName = serviceName;
    this.totalInstances = totalInstances;
  }

  public NoHealthyInstancesException(final String serviceName) {
    this(serviceName, 0);
  }

  public String getServiceName() {
    return serviceName;
  }

  public int getTotalInstances() {
    return totalInstances;
  }
}
