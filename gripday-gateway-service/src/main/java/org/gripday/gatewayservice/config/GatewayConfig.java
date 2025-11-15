package org.gripday.gatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway configuration class.
 * 
 * Route definitions are configured in application.yml for clarity and maintainability.
 * This class can be used for additional gateway-related beans and configuration if needed.
 * 
 * All routing, filtering, rate limiting, and security configurations are managed
 * through application.yml and the respective filter/security configuration classes.
 */
@Configuration
public class GatewayConfig {

  private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

  private final GripdayProperties gripdayProperties;

  public GatewayConfig(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
    logGatewayConfiguration();
  }

  /**
   * Log gateway configuration on startup for visibility.
   */
  private void logGatewayConfiguration() {
    var routing = gripdayProperties.gateway().routing();
    var services = routing.services();
    
    logger.info("Gateway Configuration:");
    logger.info("  API Prefix: {}", routing.apiPrefix().prefix());
    logger.info("  Strip Count: {}", routing.apiPrefix().stripCount());
    
    services.forEach((name, config) -> {
      if (config.enabled()) {
        logger.info("  Service '{}' enabled: {} -> {}", name, config.path(), config.uri());
      } else {
        logger.info("  Service '{}' disabled", name);
      }
    });
    
    logger.info("  Rate Limiting: {}", gripdayProperties.gateway().rateLimiting().enabled() ? "enabled" : "disabled");
    logger.info("  Circuit Breaker: {}", gripdayProperties.gateway().circuitBreaker().enabled() ? "enabled" : "disabled");
    logger.info("  CORS: {}", gripdayProperties.gateway().cors().enabled() ? "enabled" : "disabled");
  }
}
