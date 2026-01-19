package com.iqscaffold.gatewayservice.config.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BillingRouteConfigTest {

  @Autowired
  private RouteLocator billingServiceRoutes;

  @Test
  @DisplayName("Should configure billing service routes")
  void shouldConfigureBillingServiceRoutes() {
    // Given & When
    var routes = billingServiceRoutes.getRoutes().collectList().block();

    // Then
    assertThat(routes).isNotNull();
    assertThat(routes).isNotEmpty();

    // Verify feature routes are configured
    var featureRoutes = routes.stream()
        .filter(route -> route.getId().contains("features"))
        .toList();
    
    assertThat(featureRoutes).hasSize(2);
    assertThat(featureRoutes).anyMatch(route -> route.getId().equals("billing-service-features-my-features"));
    assertThat(featureRoutes).anyMatch(route -> route.getId().equals("billing-service-features-enabled"));

    // Verify billing routes are configured
    var billingRoutes = routes.stream()
        .filter(route -> route.getId().contains("billing-service"))
        .toList();
    
    assertThat(billingRoutes).hasSizeGreaterThanOrEqualTo(8); // At least 8 billing routes

    // Verify specific route IDs exist
    var routeIds = routes.stream().map(route -> route.getId()).toList();
    assertThat(routeIds).contains(
        "billing-service-features-my-features",
        "billing-service-features-enabled",
        "billing-service-subscriptions",
        "billing-service-payments",
        "billing-service-invoices",
        "billing-service-payouts",
        "billing-service-subscription-plans",
        "billing-service-admin",
        "billing-service-webhooks",
        "billing-service-internal"
    );
  }

  @Test
  @DisplayName("Should configure routes with proper URIs")
  void shouldConfigureRoutesWithProperUris() {
    // Given & When
    var routes = billingServiceRoutes.getRoutes().collectList().block();

    // Then
    assertThat(routes).isNotNull();
    
    // All billing service routes should point to the same URI
    var billingRoutes = routes.stream()
        .filter(route -> route.getId().startsWith("billing-service"))
        .toList();
    
    assertThat(billingRoutes).allMatch(route -> 
        route.getUri().toString().contains("localhost:8082") || 
        route.getUri().toString().contains("billing-service"));
  }

  @Test
  @DisplayName("Should configure routes with filters")
  void shouldConfigureRoutesWithFilters() {
    // Given & When
    var routes = billingServiceRoutes.getRoutes().collectList().block();

    // Then
    assertThat(routes).isNotNull();
    
    // All routes should have filters configured
    var billingRoutes = routes.stream()
        .filter(route -> route.getId().startsWith("billing-service"))
        .toList();
    
    assertThat(billingRoutes).allMatch(route -> !route.getFilters().isEmpty());
    
    // Verify that routes have filters configured
    billingRoutes.forEach(route -> {
      var filters = route.getFilters();
      
      // Each route should have at least one filter (StripPrefix is always present)
      assertThat(filters).isNotEmpty();
      
      // Verify that filters are properly configured
      // Note: The actual filter implementations are added at runtime
      // We can only verify that filters exist, not their specific types
      assertThat(filters.size()).isGreaterThan(0);
    });
  }
}