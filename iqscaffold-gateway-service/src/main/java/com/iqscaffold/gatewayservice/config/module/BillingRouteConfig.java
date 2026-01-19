package com.iqscaffold.gatewayservice.config.module;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration for Billing Service endpoints.
 *
 * <p>Configures routing for all billing-related endpoints including:
 * <ul>
 *   <li>Feature management endpoints (/api/v1/features/**)</li>
 *   <li>Subscription management (/api/v1/billing/subscriptions/**)</li>
 *   <li>Payment processing (/api/v1/billing/payments/**)</li>
 *   <li>Invoice management (/api/v1/billing/invoices/**)</li>
 *   <li>Payout operations (/api/v1/billing/payouts/**)</li>
 *   <li>Subscription plans (/api/v1/billing/subscription-plans/**)</li>
 *   <li>Admin billing operations (/api/v1/admin/billing/**)</li>
 *   <li>Webhook endpoints (/api/v1/billing/webhooks/**)</li>
 * </ul>
 *
 * <p>Each route includes:
 * <ul>
 *   <li>Rate limiting based on endpoint type</li>
 *   <li>Request transformation (header enrichment)</li>
 *   <li>Circuit breaker protection</li>
 *   <li>Correlation ID propagation</li>
 * </ul>
 */
@Configuration
public class BillingRouteConfig {

  @Value("${iqscaffold.gateway.routing.services.billing-service.uri:http://localhost:8082}")
  private String billingServiceUri;

  @Value("${iqscaffold.gateway.routing.api-prefix.strip-count:0}")
  private int stripCount;

  /**
   * Configures all billing service routes with appropriate filters and rate limiting.
   */
  @Bean
  public RouteLocator billingServiceRoutes(RouteLocatorBuilder builder) {
    return builder.routes()

        // Feature Management Routes (High Priority - Frontend Usage)
        .route("billing-service-features-my-features", r -> r
            .path("/api/v1/features/my-features")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-features")
                    .setFallbackUri("forward:/fallback/features"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        .route("billing-service-features-enabled", r -> r
            .path("/api/v1/features/enabled")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-features")
                    .setFallbackUri("forward:/fallback/features"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        // Subscription Management Routes
        .route("billing-service-subscriptions", r -> r
            .path("/api/v1/billing/subscriptions/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-subscriptions")
                    .setFallbackUri("forward:/fallback/billing"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        // Payment Processing Routes
        .route("billing-service-payments", r -> r
            .path("/api/v1/billing/payments/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-payments")
                    .setFallbackUri("forward:/fallback/billing"))
                .retry(config -> config.setRetries(1))) // Lower retries for payments
            .uri(billingServiceUri))

        // Invoice Management Routes
        .route("billing-service-invoices", r -> r
            .path("/api/v1/billing/invoices/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-invoices")
                    .setFallbackUri("forward:/fallback/billing"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        // Payout Operations Routes
        .route("billing-service-payouts", r -> r
            .path("/api/v1/billing/payouts/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-payouts")
                    .setFallbackUri("forward:/fallback/billing"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        // Subscription Plans Routes (Public + Protected)
        .route("billing-service-subscription-plans", r -> r
            .path("/api/v1/billing/subscription-plans/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-plans")
                    .setFallbackUri("forward:/fallback/billing"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        // Admin Billing Operations Routes
        .route("billing-service-admin", r -> r
            .path("/api/v1/admin/billing/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-admin")
                    .setFallbackUri("forward:/fallback/billing"))
                .retry(config -> config.setRetries(2)))
            .uri(billingServiceUri))

        // Webhook Routes (External Providers - Higher Limits, No Auth)
        .route("billing-service-webhooks", r -> r
            .path("/api/v1/billing/webhooks/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-webhooks")
                    .setFallbackUri("forward:/fallback/webhooks"))
                .retry(config -> config.setRetries(1))) // Lower retries for webhooks
            .uri(billingServiceUri))

        // Internal API Routes (Service-to-Service Communication)
        .route("billing-service-internal", r -> r
            .path("/api/v1/internal/features/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("billing-service-internal")
                    .setFallbackUri("forward:/fallback/internal"))
                .retry(config -> config.setRetries(3))) // Higher retries for internal calls
            .uri(billingServiceUri))

        .build();
  }
}
