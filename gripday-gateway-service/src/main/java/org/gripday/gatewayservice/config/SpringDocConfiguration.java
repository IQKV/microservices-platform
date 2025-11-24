package org.gripday.gatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc configuration for dynamic service discovery and Swagger UI setup.
 * <p>
 * Automatically configures Swagger UI to display all enabled downstream services
 * in the API dropdown selector.
 */
@Configuration
public class SpringDocConfiguration {

  private static final Logger log = LoggerFactory.getLogger(SpringDocConfiguration.class);

  private final GripdayProperties properties;

  public SpringDocConfiguration(final GripdayProperties properties) {
    this.properties = properties;
  }

  /**
   * Dynamically registers OpenAPI documentation URLs for all enabled services.
   * This runs at application startup to configure the Swagger UI dropdown.
   */
  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(SwaggerUiConfigParameters.class)
  public CommandLineRunner openApiUrlsRegistrar(SwaggerUiConfigParameters swaggerUiConfig) {
    return args -> {
      var services = properties.gateway().routing().services();

      if (services == null || services.isEmpty()) {
        log.warn("No downstream services configured for OpenAPI documentation");
        return;
      }

      var registeredCount = 0;

      for (final var entry : services.entrySet()) {
        var serviceName = entry.getKey();
        var serviceConfig = entry.getValue();

        if (!serviceConfig.enabled()) {
          log.debug("Skipping OpenAPI URL registration for disabled service: {}", serviceName);
          continue;
        }

        var openApiConfig = serviceConfig.openapi();
        if (openApiConfig == null || !openApiConfig.enabled()) {
          log.debug("OpenAPI documentation disabled for service: {}", serviceName);
          continue;
        }

        var contextPath = openApiConfig.contextPath();
        if (contextPath.isBlank()) {
          contextPath = serviceName;
        }

        // Remove leading slash if present
        if (contextPath.startsWith("/")) {
          contextPath = contextPath.substring(1);
        }

        var apiDocsUrl = "/" + contextPath + "/api-docs";
        var displayName = openApiConfig.displayName();

        swaggerUiConfig.addGroup(displayName);
        swaggerUiConfig.addUrl(apiDocsUrl);

        log.info("Registered OpenAPI documentation: {} -> {}", displayName, apiDocsUrl);
        registeredCount++;
      }

      log.info("Successfully registered {} OpenAPI documentation URLs", registeredCount);
    };
  }
}
