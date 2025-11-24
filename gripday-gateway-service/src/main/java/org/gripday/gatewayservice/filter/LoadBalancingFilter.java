package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.service.LoadBalancingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Gateway filter for load balancing requests across multiple service instances. Integrates with LoadBalancingService to select healthy service instances.
 */
@Component
public class LoadBalancingFilter extends AbstractGatewayFilterFactory<LoadBalancingFilter.Config> {

  private static final Logger logger = LoggerFactory.getLogger(LoadBalancingFilter.class);

  private final LoadBalancingService loadBalancingService;

  public LoadBalancingFilter(final LoadBalancingService loadBalancingService) {
    super(Config.class);
    this.loadBalancingService = loadBalancingService;
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      var request = exchange.getRequest();
      var serviceName = config.getServiceName();

      if (serviceName != null && config.isEnableLoadBalancing()) {
        try {
          // Get next available service instance (throws NoHealthyInstancesException if none available)
          var serviceUri = loadBalancingService.getNextServiceInstance(serviceName);

          // Modify the request URI to point to the selected instance
          var modifiedRequest = request.mutate()
              .uri(serviceUri)
              .build();

          logger.debug("Load balanced request to service {} at URI {}", serviceName, serviceUri);

          return chain.filter(exchange.mutate().request(modifiedRequest).build())
              .doOnError(throwable -> {
                // Mark instance as unhealthy on error
                loadBalancingService.markInstanceUnhealthy(serviceName, serviceUri);
                logger.error("Request failed for service {} at URI {}, marking as unhealthy",
                    serviceName, serviceUri, throwable);
              });
        } catch (final Exception e) {
          // Propagate NoHealthyInstancesException to global exception handler
          logger.error("Load balancing failed for service: {}", serviceName, e);
          throw e;
        }
      }

      return chain.filter(exchange);
    };
  }

  /**
   * Configuration class for load balancing filter.
   */
  public static class Config {

    private String serviceName;
    private boolean enableLoadBalancing = true;
    private String strategy = "round-robin";

    public String getServiceName() {
      return serviceName;
    }

    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }

    public boolean isEnableLoadBalancing() {
      return enableLoadBalancing;
    }

    public void setEnableLoadBalancing(boolean enableLoadBalancing) {
      this.enableLoadBalancing = enableLoadBalancing;
    }

    public String getStrategy() {
      return strategy;
    }

    public void setStrategy(String strategy) {
      this.strategy = strategy;
    }
  }
}