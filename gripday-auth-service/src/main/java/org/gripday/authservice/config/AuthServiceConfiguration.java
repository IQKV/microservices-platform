package org.gripday.authservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration class for Auth Service.
 * Enables configuration properties and sets up core application configuration.
 */
@Configuration
@EnableConfigurationProperties(GripdayProperties.class)
public class AuthServiceConfiguration {
    // Configuration beans will be added in subsequent tasks
}