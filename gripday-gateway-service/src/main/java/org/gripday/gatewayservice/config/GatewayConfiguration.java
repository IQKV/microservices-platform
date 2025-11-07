package org.gripday.gatewayservice.config;

import org.gripday.gatewayservice.filter.LoadBalancingFilter;
import org.gripday.gatewayservice.filter.RequestTransformationFilter;
import org.gripday.gatewayservice.filter.ResponseTransformationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration using programmatic route definitions. Provides centralized routing configuration with environment-specific settings.
 */
@Configuration
public class GatewayConfiguration {

  private final GatewayProperties gatewayProperties;
  private final RequestTransformationFilter requestTransformationFilter;
  private final ResponseTransformationFilter responseTransformationFilter;
  private final LoadBalancingFilter loadBalancingFilter;

  public GatewayConfiguration(
      GatewayProperties gatewayProperties,
      RequestTransformationFilter requestTransformationFilter,
      ResponseTransformationFilter responseTransformationFilter,
      LoadBalancingFilter loadBalancingFilter
  ) {
    this.gatewayProperties = gatewayProperties;
    this.requestTransformationFilter = requestTransformationFilter;
    this.responseTransformationFilter = responseTransformationFilter;
    this.loadBalancingFilter = loadBalancingFilter;
  }

  /**
   * Configures routes for the gateway service using properties-based configuration. Routes are defined programmatically to allow for dynamic configuration and validation. Includes
   * request/response transformation and load balancing filters.
   */
  @Bean
  public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    var authServiceConfig = gatewayProperties.routing().services().authService();

    return builder.routes()
        // Auth service routes with transformation and load balancing
        .route("auth-service", r -> r
            .path("/api/v1/auth/**")
            .filters(f -> f
                .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
                .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
                .filter(loadBalancingFilter.apply(createLoadBalancingConfig("auth-service")))
            )
            .uri(authServiceConfig.uri())
        )
        // Health check route with minimal transformation
        .route("health-check", r -> r
            .path("/actuator/health")
            .filters(f -> f
                .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
            )
            .uri(authServiceConfig.uri())
        )
        // Actuator info route with minimal transformation
        .route("actuator-info", r -> r
            .path("/actuator/info")
            .filters(f -> f
                .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
            )
            .uri(authServiceConfig.uri())
        )
        .build();
  }

  /**
   * Create load balancing configuration for a specific service.
   */
  private LoadBalancingFilter.Config createLoadBalancingConfig(String serviceName) {
    var config = new LoadBalancingFilter.Config();
    config.setServiceName(serviceName);
    config.setEnableLoadBalancing(gatewayProperties.routing().loadBalancing().enableHealthCheck());
    config.setStrategy(gatewayProperties.routing().loadBalancing().strategy());
    return config;
  }
}