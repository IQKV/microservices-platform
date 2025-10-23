package org.gripday.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration using programmatic route definitions.
 * Provides centralized routing configuration with environment-specific settings.
 */
@Configuration
public class GatewayConfiguration {

    private final GatewayProperties gatewayProperties;

    public GatewayConfiguration(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * Configures routes for the gateway service using properties-based configuration.
     * Routes are defined programmatically to allow for dynamic configuration and validation.
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        var authServiceConfig = gatewayProperties.routing().services().authService();
        
        return builder.routes()
            // Auth service routes
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .uri(authServiceConfig.uri())
            )
            // Health check route
            .route("health-check", r -> r
                .path("/actuator/health")
                .uri(authServiceConfig.uri())
            )
            // Actuator info route
            .route("actuator-info", r -> r
                .path("/actuator/info")
                .uri(authServiceConfig.uri())
            )
            .build();
    }
}