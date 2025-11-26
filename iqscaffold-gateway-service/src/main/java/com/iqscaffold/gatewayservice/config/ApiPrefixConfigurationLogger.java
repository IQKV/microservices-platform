package com.iqscaffold.gatewayservice.config;

import com.iqscaffold.gatewayservice.service.ApiPrefixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs API prefix configuration details on application startup.
 * Helps verify correct environment-specific prefix configuration.
 */
@Component
public class ApiPrefixConfigurationLogger {

  private static final Logger logger = LoggerFactory.getLogger(ApiPrefixConfigurationLogger.class);

  private final ApiPrefixService apiPrefixService;
  private final IqScaffoldProperties systemProperties;

  public ApiPrefixConfigurationLogger(final ApiPrefixService apiPrefixService, final IqScaffoldProperties systemProperties) {
    this.apiPrefixService = apiPrefixService;
    this.systemProperties = systemProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logApiPrefixConfiguration() {
    logger.info("=".repeat(80));
    logger.info("API Prefix Configuration");
    logger.info("=".repeat(80));
    logger.info("Enabled: {}", apiPrefixService.isEnabled());
    logger.info("Prefix: '{}'", apiPrefixService.getPrefix());
    logger.info("Strip Count: {}", apiPrefixService.getStripCount());
    logger.info("-".repeat(80));

    if (apiPrefixService.isEnabled()) {
      var exampleServicePath = "/v1/auth/login";
      var fullPath = apiPrefixService.buildFullPath(exampleServicePath);

      logger.info("Example Route Configuration:");
      logger.info("  Service Path: {}", exampleServicePath);
      logger.info("  Full Path: {}", fullPath);

      if (apiPrefixService.getStripCount() > 0) {
        var strippedPath = apiPrefixService.stripPrefix(fullPath);
        logger.info("  Backend Receives: {}", strippedPath);
      } else {
        logger.info("  Backend Receives: {} (no stripping)", fullPath);
      }
    } else {
      logger.info("API prefix handling is disabled");
    }

    logger.info("-".repeat(80));
    logServiceRoutes();
    logger.info("=".repeat(80));
  }

  private void logServiceRoutes() {
    logger.info("Configured Service Routes:");

    var services = systemProperties.gateway().routing().services();
    if (services != null && !services.isEmpty()) {
      services.forEach((serviceName, serviceConfig) -> {
        if (serviceConfig.enabled()) {
          var fullPath = apiPrefixService.buildFullPath(serviceConfig.path());
          logger.info("  {}:", serviceName);
          logger.info("    URI: {}", serviceConfig.uri());
          logger.info("    Path Pattern: {}", fullPath);
          logger.info("    Connect Timeout: {}ms", serviceConfig.connectTimeout());
          logger.info("    Response Timeout: {}ms", serviceConfig.responseTimeout());
        }
      });
    }
  }
}
