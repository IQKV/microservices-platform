package org.gripday.gatewayservice.config;

import org.gripday.gatewayservice.filter.LoadBalancingFilter;
import org.gripday.gatewayservice.filter.RequestTransformationFilter;
import org.gripday.gatewayservice.filter.ResponseTransformationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration using programmatic route definitions.
 * Provides centralized routing configuration with environment-specific settings.
 * Routes are dynamically created based on service enabled status.
 */
@Configuration
public class GatewayConfig {

  private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

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
   * Configures routes for the gateway service using properties-based configuration.
   * Routes are defined programmatically to allow for dynamic configuration and validation.
   * Only enabled services will have routes created. All filter configurations are driven
   * by GripdayProperties.
   */
  @Bean
  public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    var services = gripdayProperties.gateway().routing().services();
    var routesBuilder = builder.routes();

    // User service routes - authentication, user, organization, and tenant management
    var userServiceConfig = services.get("user-service");
    if (userServiceConfig != null && userServiceConfig.enabled()) {
      logger.info("Enabling user-service routes: {}", userServiceConfig.uri());
      routesBuilder.route("user-service", r -> r
          .path("/api/v1/auth/**", "/api/v1/password/**", "/api/v1/admin/users/**",
              "/api/v1/admin/organizations/**", "/api/v1/admin/**")
          .filters(f -> f
              .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
              .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
              .filter(loadBalancingFilter.apply(createLoadBalancingConfig("user-service")))
          )
          .uri(userServiceConfig.uri())
      );
    } else {
      logger.warn("User service is disabled or not configured");
    }

    // Bookstore service routes - book catalog and inventory management
    var bookstoreServiceConfig = services.get("bookstore-service");
    if (bookstoreServiceConfig != null && bookstoreServiceConfig.enabled()) {
      logger.info("Enabling bookstore-service routes: {}", bookstoreServiceConfig.uri());
      routesBuilder.route("bookstore-service", r -> r
          .path("/api/v1/bookstore/**")
          .filters(f -> f
              .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
              .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
              .filter(loadBalancingFilter.apply(createLoadBalancingConfig("bookstore-service")))
          )
          .uri(bookstoreServiceConfig.uri())
      );
    } else {
      logger.info("Bookstore service is disabled or not configured");
    }

    // Health check route - always enabled if user-service is available
    if (userServiceConfig != null && userServiceConfig.enabled()) {
      routesBuilder.route("health-check", r -> r
          .path("/actuator/health")
          .filters(f -> f
              .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
          )
          .uri(userServiceConfig.uri())
      );

      // Actuator info route - always enabled if user-service is available
      routesBuilder.route("actuator-info", r -> r
          .path("/actuator/info")
          .filters(f -> f
              .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
          )
          .uri(userServiceConfig.uri())
      );
    }

    return routesBuilder.build();
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
