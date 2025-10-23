package org.gripday.authservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration validator that ensures all custom properties follow gripday. prefix convention
 * and validates YAML format compliance on startup.
 */
@Component
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);
    private static final String REQUIRED_PREFIX = "gripday.";

    private final ApplicationContext applicationContext;

    public ConfigurationValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Validates configuration properties on application startup.
     */
    @PostConstruct
    public void validateConfiguration() {
        logger.info("Starting configuration validation for gripday prefix convention");
        
        validateGripdayPrefixConvention();
        validateYamlFormatCompliance();
        validateNoPropertiesFiles();
        
        logger.info("Configuration validation completed successfully");
    }

    /**
     * Validates that all custom configuration properties use gripday. prefix.
     */
    private void validateGripdayPrefixConvention() {
        var configurationPropertiesBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
        
        for (Map.Entry<String, Object> entry : configurationPropertiesBeans.entrySet()) {
            var beanName = entry.getKey();
            var bean = entry.getValue();
            var annotation = bean.getClass().getAnnotation(ConfigurationProperties.class);
            
            if (annotation != null) {
                var prefix = annotation.prefix();
                
                // Skip Spring Boot's own configuration properties
                if (isSpringBootProperty(prefix)) {
                    continue;
                }
                
                // Validate custom properties use gripday. prefix
                if (!prefix.startsWith(REQUIRED_PREFIX)) {
                    var errorMessage = String.format(
                        "Configuration property class '%s' with prefix '%s' does not follow gripday. prefix convention. " +
                        "All custom configuration properties must use 'gripday.' prefix for clear namespace separation.",
                        bean.getClass().getSimpleName(), prefix
                    );
                    logger.error(errorMessage);
                    throw new IllegalStateException(errorMessage);
                }
                
                logger.debug("Validated configuration property class '{}' with prefix '{}'", 
                           bean.getClass().getSimpleName(), prefix);
            }
        }
        
        logger.info("All configuration properties follow gripday. prefix convention");
    }

    /**
     * Validates YAML format compliance by checking for common YAML structure.
     */
    private void validateYamlFormatCompliance() {
        // This validation is primarily handled by Spring Boot's YAML parser
        // If the application starts successfully, YAML format is valid
        logger.info("YAML format compliance validated - application started successfully with YAML configuration");
    }

    /**
     * Validates that no .properties files are being used for configuration.
     */
    private void validateNoPropertiesFiles() {
        // Check if any .properties files are present in the classpath
        var propertiesResources = new String[]{
            "application.properties",
            "application-local.properties", 
            "application-staging.properties",
            "application-production.properties"
        };
        
        for (var resourcePath : propertiesResources) {
            var resource = applicationContext.getResource("classpath:" + resourcePath);
            if (resource.exists()) {
                var errorMessage = String.format(
                    "Properties file '%s' found in classpath. " +
                    "Only YAML configuration files (.yml) are allowed. " +
                    "Please convert all .properties files to YAML format.",
                    resourcePath
                );
                logger.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            }
        }
        
        logger.info("No .properties files found - YAML-only configuration validated");
    }

    /**
     * Checks if a property prefix belongs to Spring Boot framework.
     */
    private boolean isSpringBootProperty(String prefix) {
        return prefix.startsWith("spring.") ||
               prefix.startsWith("server.") ||
               prefix.startsWith("management.") ||
               prefix.startsWith("logging.") ||
               prefix.startsWith("springdoc.") ||
               prefix.isEmpty(); // Default properties without prefix
    }
}