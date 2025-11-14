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
public class GatewayConfig {

  private final GripdayProperties gripdayProperties;
  private final RequestTransformationFilter requestTransformationFilter;
  private final ResponseTransformationFilter responseTransformationFilter;
  private final LoadBalancingFilter loadBalancingFilter;

  public GatewayConfig(
      final GripdayProperties gripdayProperties,
      final RequestTransformationFilter requestTransformationFilter,
      final ResponseTransformationFilter responseTransformationFilter,
      final LoadBalancingFilter loadBalancingFilter
  ) {
    this.gripdayProperties = gripdayProperties;
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
    var services = gripdayProperties.gateway().routing().services();
    var authServiceConfig = services.get("user-service");

    return builder.routes()
        // User service routes - all authentication, user, organization, and tenant management endpoints
        .route("user-service", r -> r
            .path("/api/v1/auth/**", "/api/v1/password/**", "/api/v1/users/**", 
                  "/api/v1/organizations/**", "/api/v1/admin/**")
            .filters(f -> f
                .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
                .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
                .filter(loadBalancingFilter.apply(createLoadBalancingConfig("user-service")))
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
    config.setEnableLoadBalancing(gripdayProperties.gateway().routing().loadBalancing().enableHealthCheck());
    config.setStrategy(gripdayProperties.gateway().routing().loadBalancing().strategy());
    return config;
  }
}