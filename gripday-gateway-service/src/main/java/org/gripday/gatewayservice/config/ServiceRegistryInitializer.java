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
  private final GatewayProperties gatewayProperties;

  public ServiceRegistryInitializer(
      final LoadBalancingService loadBalancingService,
      final GatewayProperties gatewayProperties
  ) {
    this.loadBalancingService = loadBalancingService;
    this.gatewayProperties = gatewayProperties;
  }

  /**
   * Initialize service instances when the application is ready.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void initializeServiceInstances() {
    logger.info("Initializing service instances for load balancing");

    // Register auth service instances
    var authServiceConfig = gatewayProperties.routing().services().authService();
    if (authServiceConfig.enabled()) {
      var authInstances = List.of(URI.create(authServiceConfig.uri()));
      loadBalancingService.registerServiceInstances("auth-service", authInstances);
      logger.info("Registered auth service instances: {}", authInstances);
    }

    // In a real implementation, this would discover services from:
    // - Service registry (Consul, Eureka)
    // - Kubernetes service discovery
    // - Configuration files
    // - Environment variables

    logger.info("Service instance initialization completed");
  }
}