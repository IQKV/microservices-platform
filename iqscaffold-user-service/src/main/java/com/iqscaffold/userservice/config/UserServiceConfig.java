package com.iqscaffold.userservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration class for User Service.
 * Enables configuration properties and sets up core application configuration.
 */
@Configuration
@EnableConfigurationProperties({
    IqScaffoldProperties.class,
    PlatformConfigurationProperties.class
})
public class UserServiceConfig {
  // Configuration beans will be added in subsequent tasks
}
