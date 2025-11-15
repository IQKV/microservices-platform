package org.gripday.gatewayservice.config;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic OpenAPI configuration for Gateway Service with automatic aggregation of downstream service documentation.
 * 
 * This configuration automatically discovers and configures OpenAPI documentation for all enabled downstream services
 * based on the gripday.gateway.routing.services configuration properties.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Gripday API Gateway",
        version = "1.0.0",
        description = """
            API Gateway for the Gripday microservices platform with intelligent routing, 
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
            name = "Gripday Platform Team",
            email = "api-support@gripday.com",
            url = "https://docs.gripday.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "https://api.gripday.com",
            description = "Production Server"
        ),
        @Server(
            url = "https://api.gripday.website",
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

  private final GripdayProperties properties;

  public OpenApiConfig(GripdayProperties properties) {
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

      var pathPattern = contextPath + "/**";
      
      log.info("Configuring OpenAPI documentation for service '{}': displayName='{}', pathPattern='{}'",
          serviceName, openApiConfig.displayName(), pathPattern);

      var groupedApi = GroupedOpenApi.builder()
          .group(serviceName)
          .displayName(openApiConfig.displayName())
          .pathsToMatch(pathPattern)
          .build();

      serviceApis.add(groupedApi);
    });

    log.info("Configured OpenAPI documentation for {} downstream services", serviceApis.size());
    return serviceApis;
  }
}
