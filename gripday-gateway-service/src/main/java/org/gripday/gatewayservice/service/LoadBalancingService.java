package org.gripday.gatewayservice.service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.gripday.gatewayservice.exception.NoHealthyInstancesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Load balancing service for distributing requests across multiple service instances. Implements round-robin and health-check based load balancing strategies.
 */
@Service
public class LoadBalancingService {

  private static final Logger logger = LoggerFactory.getLogger(LoadBalancingService.class);

  private final Map<String, List<ServiceInstance>> serviceInstances = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
  private final Map<String, Instant> lastHealthCheck = new ConcurrentHashMap<>();

  /**
   * Get the next available service instance using round-robin strategy.
   *
   * @throws NoHealthyInstancesException if no healthy instances are available
   */
  public URI getNextServiceInstance(String serviceName) {
    var instances = serviceInstances.get(serviceName);
    if (instances == null || instances.isEmpty()) {
      logger.warn("No instances registered for service: {}", serviceName);
      throw new NoHealthyInstancesException(serviceName, 0);
    }

    // Filter healthy instances
    var healthyInstances = instances.stream()
        .filter(ServiceInstance::isHealthy)
        .toList();

    if (healthyInstances.isEmpty()) {
      logger.warn("No healthy instances available for service: {}", serviceName);
      throw new NoHealthyInstancesException(serviceName, instances.size());
    }

    // Round-robin selection
    var counter = roundRobinCounters.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
    var index = counter.getAndIncrement() % healthyInstances.size();
    var selectedInstance = healthyInstances.get(index);

    logger.debug("Selected instance {} for service {}", selectedInstance.getUri(), serviceName);
    return selectedInstance.getUri();
  }

  /**
   * Register service instances for load balancing.
   */
  public void registerServiceInstances(String serviceName, List<URI> instanceUris) {
    var instances = instanceUris.stream()
        .map(uri -> new ServiceInstance(uri, true, Instant.now()))
        .toList();

    serviceInstances.put(serviceName, instances);
    logger.info("Registered {} instances for service {}", instances.size(), serviceName);
  }

  /**
   * Mark a service instance as unhealthy.
   */
  public void markInstanceUnhealthy(String serviceName, URI instanceUri) {
    var instances = serviceInstances.get(serviceName);
    if (instances != null) {
      instances.stream()
          .filter(instance -> instance.getUri().equals(instanceUri))
          .findFirst()
          .ifPresent(instance -> {
            instance.setHealthy(false);
            instance.setLastHealthCheck(Instant.now());
            logger.warn("Marked instance {} as unhealthy for service {}", instanceUri, serviceName);
          });
    }
  }

  /**
   * Service instance representation.
   */
  public static class ServiceInstance {

    private final URI uri;
    private boolean healthy;
    private Instant lastHealthCheck;

    public ServiceInstance(final URI uri, final boolean healthy, final Instant lastHealthCheck) {
      this.uri = uri;
      this.healthy = healthy;
      this.lastHealthCheck = lastHealthCheck;
    }

    public URI getUri() {
      return uri;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public void setHealthy(boolean healthy) {
      this.healthy = healthy;
    }

    public Instant getLastHealthCheck() {
      return lastHealthCheck;
    }

    public void setLastHealthCheck(Instant lastHealthCheck) {
      this.lastHealthCheck = lastHealthCheck;
    }
  }
}