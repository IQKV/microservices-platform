package com.iqscaffold.gatewayservice.config.module;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("crm")
public class CrmRouteConfig {

  private final IqScaffoldProperties properties;

  public CrmRouteConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public RouteLocator crmRoutes(final RouteLocatorBuilder builder) {
    var stripCount = properties.gateway().routing().apiPrefix().stripCount();
    var leadServiceUri = properties.gateway().routing().services().get("lead-service").uri();
    var pipelineServiceUri = properties.gateway().routing().services().get("pipeline-service").uri();
    var contactServiceUri = properties.gateway().routing().services().get("contact-service").uri();
    var defaultReplenishRate = properties.gateway().rateLimiting().policies().defaultRequestsPerMinute();
    var defaultBurstCapacity = properties.gateway().rateLimiting().policies().defaultBurstCapacity();

    return builder.routes()
        .route("lead-service-activities", r -> r
            .path("/api/v1/leads/{leadId}/activities/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(leadServiceUri))

        .route("lead-service-notes", r -> r
            .path("/api/v1/leads/{leadId}/notes/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(leadServiceUri))

        .route("lead-service", r -> r
            .path("/api/v1/leads/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(leadServiceUri))

        .route("pipeline-service-dashboard", r -> r
            .path("/api/v1/pipeline/dashboard/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(pipelineServiceUri))

        .route("pipeline-service-follow-ups", r -> r
            .path("/api/v1/pipeline/follow-ups/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(pipelineServiceUri))

        .route("pipeline-service", r -> r
            .path("/api/v1/pipeline/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(pipelineServiceUri))

        .route("contact-service-webhooks", r -> r
            .path("/api/v1/crm/webhooks/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(contactServiceUri))

        .route("contact-service", r -> r
            .path("/api/v1/contacts/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(contactServiceUri))

        .route("company-service", r -> r
            .path("/api/v1/companies/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    new RedisRateLimiter(
                        defaultReplenishRate,
                        defaultBurstCapacity))))
            .uri(contactServiceUri))
        .build();
  }
}
