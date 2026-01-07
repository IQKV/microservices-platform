package com.iqscaffold.gatewayservice.config;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dynamic OpenAPI configuration for Gateway Service with automatic aggregation of downstream service documentation.
 * <p>
 * This configuration automatically discovers and configures OpenAPI documentation for all enabled downstream services
 * based on the iqscaffold.gateway.routing.services configuration properties.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "IQ Scaffold API Gateway",
        version = "1.0.0",
        description = """
            API Gateway for the IQ Scaffold microservices platform with intelligent routing, 
            authentication, rate limiting, and circuit breaker patterns.
            
            ## Features
            - Centralized API routing and load balancing
            - JWT authentication enforcement
            - Redis-backed distributed rate limiting
            - Circuit breaker patterns with Resilience4j
            - CORS handling and request/response transformation
            - Aggregated API documentation from all services
            
            ## Authentication
            Most endpoints require JWT authentication. Include the Bearer token in the Authorization header:
            ```
            Authorization: Bearer <your-jwt-token>
            ```
            
            ## Service Documentation
            This gateway automatically aggregates API documentation from all configured downstream services.
            Use the dropdown in the top-right corner to switch between different service APIs.
            """,
        contact = @Contact(
            name = "IQ Scaffold Platform Team",
            email = "api-support@iqscaffold.com",
            url = "https://docs.iqscaffold.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "https://api.iqscaffold.com",
            description = "Production Server"
        ),
        @Server(
            url = "https://api.iqscaffold.website",
            description = "Staging Server"
        ),
        @Server(
            url = "http://localhost:8080",
            description = "Local Development Server"
        )
    },
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecuritySchemes({
    @SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = """
            JWT Bearer token authentication. 
            
            Obtain a token by calling the `/api/v1/auth/login` endpoint with valid credentials.
            The token should be included in the Authorization header for protected endpoints.
            
            Example: `Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
            """
    )
})
public class OpenApiConfig {

  private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

  private final IqScaffoldProperties properties;

  public OpenApiConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  /**
   * Gateway-specific API documentation group.
   */
  @Bean
  public GroupedOpenApi gatewayApi() {
    log.info("Configuring Gateway API documentation group");
    return GroupedOpenApi.builder()
        .group("gateway")
        .displayName("Gateway APIs")
        .pathsToMatch("/actuator/**", "/fallback/**", "/api/v1/docs")
        .build();
  }

  /**
   * Dynamically creates OpenAPI groups for all enabled downstream services.
   * This bean uses a factory method to generate multiple GroupedOpenApi beans at runtime.
   */
  @Bean
  public List<GroupedOpenApi> downstreamServiceApis() {
    var serviceApis = new ArrayList<GroupedOpenApi>();
    var services = properties.gateway().routing().services();

    if (services == null || services.isEmpty()) {
      log.warn("No downstream services configured for OpenAPI documentation");
      return serviceApis;
    }

    services.forEach((serviceName, serviceConfig) -> {
      if (!serviceConfig.enabled()) {
        log.debug("Skipping OpenAPI configuration for disabled service: {}", serviceName);
        return;
      }

      var openApiConfig = serviceConfig.openapi();
      if (openApiConfig == null || !openApiConfig.enabled()) {
        log.debug("OpenAPI documentation disabled for service: {}", serviceName);
        return;
      }

      var contextPath = openApiConfig.contextPath();
      if (contextPath.isBlank()) {
        contextPath = "/" + serviceName;
      }

      // Create main service API group
      var pathPattern = contextPath + "/**";

      log.info("Configuring OpenAPI documentation for service '{}': displayName='{}', pathPattern='{}'",
          serviceName, openApiConfig.displayName(), pathPattern);

      var groupedApi = GroupedOpenApi.builder()
          .group(serviceName)
          .displayName(openApiConfig.displayName())
          .pathsToMatch(pathPattern)
          .build();

      serviceApis.add(groupedApi);

      // Special handling for billing service with grouped APIs
      if ("billing-service".equals(serviceName)) {
        log.info("Configuring specialized API groups for billing service");

        // Payment APIs group
        var paymentApi = GroupedOpenApi.builder()
            .group("billing-payments")
            .displayName("💳 Payment APIs")
            .pathsToMatch("/api/v1/billing/payments/**")
            .build();
        serviceApis.add(paymentApi);

        // Webhook APIs group
        var webhookApi = GroupedOpenApi.builder()
            .group("billing-webhooks")
            .displayName("🔗 Webhook APIs")
            .pathsToMatch("/api/v1/billing/webhooks/**")
            .build();
        serviceApis.add(webhookApi);

        // Admin APIs group
        var adminApi = GroupedOpenApi.builder()
            .group("billing-admin")
            .displayName("🛠️ Billing Admin APIs")
            .pathsToMatch("/api/v1/admin/billing/**")
            .build();
        serviceApis.add(adminApi);

        log.info("Added {} specialized API groups for billing service", 3);
      }
    });

    log.info("Configured OpenAPI documentation for {} downstream services with {} total groups",
        services.size(), serviceApis.size());
    return serviceApis;
  }
}
