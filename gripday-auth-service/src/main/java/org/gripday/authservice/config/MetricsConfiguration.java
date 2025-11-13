package org.gripday.authservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for Prometheus metrics collection and customization. Provides environment-specific metric tags and filtering.
 */
@Configuration
@EnableConfigurationProperties(GripdayProperties.class)
public class MetricsConfiguration {

  private final GripdayProperties.Observability observabilityProperties;
  private final Environment environment;

  public MetricsConfiguration(final GripdayProperties gripdayProperties, final Environment environment) {
    this.observabilityProperties = gripdayProperties.observability();
    this.environment = environment;
  }

  /**
   * Customizes the meter registry with common tags and filters.
   */
  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    var metricsProps = observabilityProperties.metrics();

    return registry -> {
      // Add common tags
      if (metricsProps.includeApplicationTag()) {
        registry.config().commonTags("application", "gripday-auth-service");
      }

      if (metricsProps.includeEnvironmentTag()) {
        registry.config().commonTags("environment", getActiveProfile());
      }

      if (metricsProps.includeHostTag()) {
        var hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
          hostname = System.getProperty("user.name", "unknown");
        }
        registry.config().commonTags("host", hostname);
      }

      // Add custom tags from configuration
      metricsProps.customTags().forEach((key, value) ->
          registry.config().commonTags(key, value));

      // Add metric filters for performance
      registry.config().meterFilter(MeterFilter.deny(id -> {
        var name = id.getName();
        // Filter out noisy metrics in production
        if ("production".equals(getActiveProfile())) {
          return name.startsWith("jvm.gc.pause")
              || name.startsWith("process.")
              || name.startsWith("system.cpu.count");
        }
        return false;
      }));

      // Add service tag
      registry.config().commonTags("service", "gripday-auth");
    };
  }

  /**
   * Gets the active Spring profile for environment identification.
   */
  private String getActiveProfile() {
    var activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length > 0 ? activeProfiles[0] : "default";
  }
}