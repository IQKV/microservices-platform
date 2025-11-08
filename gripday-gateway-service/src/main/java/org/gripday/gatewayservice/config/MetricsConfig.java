package org.gripday.gatewayservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for Prometheus metrics collection and customization in the gateway service. Provides environment-specific metric tags and filtering for reactive applications.
 */
@Configuration
@EnableConfigurationProperties(GripdayGatewayObservabilityProperties.class)
public class MetricsConfig {

  private final GripdayGatewayObservabilityProperties observabilityProperties;
  private final Environment environment;

  public MetricsConfig(GripdayGatewayObservabilityProperties observabilityProperties, Environment environment) {
    this.observabilityProperties = observabilityProperties;
    this.environment = environment;
  }

  /**
   * Customizes the meter registry with common tags and filters for gateway metrics.
   */
  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    var metricsProps = observabilityProperties.metrics();

    return registry -> {
      // Add common tags
      if (metricsProps.includeApplicationTag()) {
        registry.config().commonTags("application", "gripday-gateway-service");
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

      // Add metric filters for performance and relevance
      registry.config().meterFilter(MeterFilter.deny(id -> {
        var name = id.getName();
        // Filter out noisy metrics in production
        if ("production".equals(getActiveProfile())) {
          return name.startsWith("jvm.gc.pause")
              || name.startsWith("process.")
              || name.startsWith("system.cpu.count")
              || name.startsWith("reactor.netty.connection.provider");
        }
        return false;
      }));

      // Enable only configured metrics
      registry.config().meterFilter(MeterFilter.accept(id -> {
        var name = id.getName();
        return metricsProps.enabledMetrics().stream()
            .anyMatch(enabledMetric -> name.contains(enabledMetric))
            || name.startsWith("gripday.gateway")
            || name.startsWith("http.server.requests")
            || name.startsWith("spring.cloud.gateway");
      }));

      // Add service tag
      registry.config().commonTags("service", "gripday-gateway");
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