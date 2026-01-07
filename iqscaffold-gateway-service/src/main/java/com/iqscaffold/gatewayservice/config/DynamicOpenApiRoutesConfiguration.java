package com.iqscaffold.gatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dynamic configuration for OpenAPI documentation routes.
 * <p>
 * Automatically creates gateway routes for Swagger UI and OpenAPI JSON endpoints
 * for all enabled downstream services based on configuration.
 * <p>
 * This configuration is only active when Spring Cloud Gateway is enabled.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", havingValue = "true", matchIfMissing = true)
public class DynamicOpenApiRoutesConfiguration {

  private static final Logger log = LoggerFactory.getLogger(DynamicOpenApiRoutesConfiguration.class);

  private final IqScaffoldProperties properties;

  public DynamicOpenApiRoutesConfiguration(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  /**
   * Creates dynamic routes for OpenAPI documentation endpoints.
   * <p>
   * For each enabled service with OpenAPI enabled, creates two routes:
   * 1. Swagger UI route: /{service-name}/swagger-ui/** -> /swagger-ui/**
   * 2. API Docs route: /{service-name}/api-docs/** -> /api-docs/**
   */
  @Bean
  public RouteLocator openApiRoutes(RouteLocatorBuilder builder) {
    var routeBuilder = builder.routes();
    var services = properties.gateway().routing().services();

    if (services == null || services.isEmpty()) {
      log.warn("No downstream services configured for OpenAPI routes");
      return routeBuilder.build();
    }

    services.forEach((serviceName, serviceConfig) -> {
      if (!serviceConfig.enabled()) {
        log.debug("Skipping OpenAPI routes for disabled service: {}", serviceName);
        return;
      }

      var openApiConfig = serviceConfig.openapi();
      if (openApiConfig == null || !openApiConfig.enabled()) {
        log.debug("OpenAPI routes disabled for service: {}", serviceName);
        return;
      }

      var contextPath = openApiConfig.contextPath();
      if (contextPath.isBlank()) {
        contextPath = serviceName;
      }

      // Remove leading slash if present
      if (contextPath.startsWith("/")) {
        contextPath = contextPath.substring(1);
      }

      var serviceUri = serviceConfig.uri();

      // Create Swagger UI route
      var swaggerUiRouteId = serviceName + "-swagger-ui";
      var swaggerUiPath = "/" + contextPath + "/swagger-ui";

      String finalContextPath = contextPath;
      routeBuilder.route(swaggerUiRouteId, r -> r
          .path(swaggerUiPath + ".html", swaggerUiPath + "/**")
          .filters(f -> f.rewritePath(
              "/" + finalContextPath + "/swagger-ui(?<segment>/?.*)",
              "/swagger-ui${segment}"
          ))
          .uri(serviceUri)
      );

      log.info("Created Swagger UI route for '{}': {} -> {}/swagger-ui",
          serviceName, swaggerUiPath, serviceUri);

      // Create API Docs route
      var apiDocsRouteId = serviceName + "-api-docs";
      var apiDocsPath = "/" + contextPath + "/api-docs";

      String finalContextPath1 = contextPath;
      routeBuilder.route(apiDocsRouteId, r -> r
          .path(apiDocsPath, apiDocsPath + "/**")
          .filters(f -> f.rewritePath(
              "/" + finalContextPath1 + "/api-docs(?<segment>/?.*)",
              "/api-docs${segment}"
          ))
          .uri(serviceUri)
      );

      log.info("Created API Docs route for '{}': {} -> {}/api-docs",
          serviceName, apiDocsPath, serviceUri);

      // Special handling for billing service with grouped API docs
      if ("billing-service".equals(serviceName)) {
        log.info("Creating specialized API docs routes for billing service groups");

        // Create routes for specialized billing API groups
        var billingGroups = java.util.List.of("billing-payments", "billing-webhooks", "billing-admin");

        for (final var groupName : billingGroups) {
          var groupApiDocsRouteId = serviceName + "-" + groupName + "-api-docs";
          var groupApiDocsPath = "/" + contextPath + "/api-docs/" + groupName;

          String finalContextPath2 = contextPath;
          routeBuilder.route(groupApiDocsRouteId, r -> r
              .path(groupApiDocsPath)
              .filters(f -> f.rewritePath(
                  "/" + finalContextPath2 + "/api-docs/" + groupName,
                  "/api-docs/" + groupName
              ))
              .uri(serviceUri)
          );

          log.info("Created specialized API Docs route for '{}' group '{}': {} -> {}/api-docs/{}",
              serviceName, groupName, groupApiDocsPath, serviceUri, groupName);
        }
      }
    });

    return routeBuilder.build();
  }
}
