package com.iqscaffold.gatewayservice.config.module;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("billing")
public class BillingRouteConfig {

    private final IqScaffoldProperties properties;

    public BillingRouteConfig(final IqScaffoldProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RouteLocator billingRoutes(final RouteLocatorBuilder builder) {
        var stripCount = properties.gateway().routing().apiPrefix().stripCount();
        var billingServiceUri = properties.gateway().routing().services().get("billing-service").uri();
        var defaultReplenishRate = properties.gateway().rateLimiting().policies().defaultRequestsPerMinute();
        var defaultBurstCapacity = properties.gateway().rateLimiting().policies().defaultBurstCapacity();

        return builder.routes()
                .route("billing-service-admin", r -> r
                        .path("/api/v1/admin/billing/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(new RedisRateLimiter(30, 50))))
                        .uri(billingServiceUri))

                .route("billing-service-webhooks", r -> r
                        .path("/api/v1/billing/webhooks/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(new RedisRateLimiter(200, 500))))
                        .uri(billingServiceUri))

                .route("billing-service-public-plans", r -> r
                        .path("/api/v1/billing/subscription-plans", "/api/v1/billing/subscription-plans/active")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(new RedisRateLimiter(60, 100))))
                        .uri(billingServiceUri))

                .route("billing-service-payments", r -> r
                        .path("/api/v1/billing/payments/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(
                                        new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity))))
                        .uri(billingServiceUri))

                .route("billing-service-subscriptions", r -> r
                        .path("/api/v1/billing/subscriptions/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(
                                        new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity))))
                        .uri(billingServiceUri))

                .route("billing-service-invoices", r -> r
                        .path("/api/v1/billing/invoices/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(
                                        new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity))))
                        .uri(billingServiceUri))

                .route("billing-service-payouts", r -> r
                        .path("/api/v1/billing/payouts/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(
                                        new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity))))
                        .uri(billingServiceUri))

                .route("billing-service", r -> r
                        .path("/api/v1/billing/**")
                        .filters(f -> f
                                .stripPrefix(stripCount)
                                .requestRateLimiter(c -> c.setRateLimiter(
                                        new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity))))
                        .uri(billingServiceUri))
                .build();
    }
}
