package org.gripday.gatewayservice.config;

import java.net.URI;
import java.util.List;

import org.gripday.gatewayservice.service.LoadBalancingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes service registry with available service instances for load balancing. Registers service instances based on configuration properties.
 */
@Component
public class ServiceRegistryInitializer {

  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryInitializer.class);

  private final LoadBalancingService loadBalancingService;
  private final GripdayProperties gripdayProperties;

  public ServiceRegistryInitializer(
      final LoadBalancingService loadBalancingService,
      final GripdayProperties gripdayProperties
  ) {
    this.loadBalancingService = loadBalancingService;
    this.gripdayProperties = gripdayProperties;
  }

  /**
   * Initialize service instances when the application is ready.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void initializeServiceInstances() {
    logger.info("Initializing service instances for load balancing");

    // Register service instances from configuration
    var services = gripdayProperties.gateway().routing().services();
    if (services != null) {
      services.forEach((serviceName, serviceConfig) -> {
        if (serviceConfig.enabled()) {
          var instances = List.of(URI.create(serviceConfig.uri()));
          loadBalancingService.registerServiceInstances(serviceName, instances);
          logger.info("Registered {} instances: {}", serviceName, instances);
        }
      });
    }

    // In a real implementation, this would discover services from:
    // - Service registry (Consul, Eureka)
    // - Kubernetes service discovery
    // - Configuration files
    // - Environment variables

    logger.info("Service instance initialization completed");
  }
}