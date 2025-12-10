package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.exception.FeatureNotAvailableException;
import com.iqscaffold.gatewayservice.service.BillingServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FeatureAccessFilterTest {

  @Mock
  private IqScaffoldProperties properties;

  @Mock
  private IqScaffoldProperties.GatewayProperties gatewayProperties;

  @Mock
  private IqScaffoldProperties.GatewayProperties.FeatureAccessProperties featureAccessProperties;

  @Mock
  private BillingServiceClient billingServiceClient;

  @Mock
  private GatewayFilterChain filterChain;

  private FeatureAccessFilter featureAccessFilter;

  @BeforeEach
  void setUp() {
    lenient().when(properties.gateway()).thenReturn(gatewayProperties);
    featureAccessFilter = new FeatureAccessFilter(properties, billingServiceClient);
  }

  @Test
  @DisplayName("Should skip feature check when feature access is disabled")
  void shouldSkipFeatureCheckWhenDisabled() {
    // Arrange
    when(gatewayProperties.featureAccess()).thenReturn(null);
    when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

    var request = MockServerHttpRequest.get("/api/v1/test").build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = featureAccessFilter.filter(exchange, filterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(billingServiceClient, never()).checkFeatureAccess(anyString(), anyString());
  }

  @Test
  @DisplayName("Should skip feature check when no tenant context")
  void shouldSkipFeatureCheckWhenNoTenantContext() {
    // Arrange
    when(gatewayProperties.featureAccess()).thenReturn(featureAccessProperties);
    when(featureAccessProperties.enabled()).thenReturn(true);
    when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

    var request = MockServerHttpRequest.get("/api/v1/test").build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = featureAccessFilter.filter(exchange, filterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(billingServiceClient, never()).checkFeatureAccess(anyString(), anyString());
  }

  @Test
  @DisplayName("Should allow access when feature is available")
  void shouldAllowAccessWhenFeatureAvailable() {
    // Arrange
    when(gatewayProperties.featureAccess()).thenReturn(featureAccessProperties);
    when(featureAccessProperties.enabled()).thenReturn(true);
    when(featureAccessProperties.endpointFeatureMapping())
        .thenReturn(Map.of("/api/v1/crm/bulk-import", "CRM.BULK_IMPORT"));
    when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

    var featureResponse = new BillingServiceClient.FeatureAccessResponse(true, "PRO", "Feature available");
    when(billingServiceClient.checkFeatureAccess("tenant-123", "CRM.BULK_IMPORT"))
        .thenReturn(Mono.just(featureResponse));

    var request = MockServerHttpRequest.get("/api/v1/crm/bulk-import").build();
    var exchange = MockServerWebExchange.from(request);

    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put("tenantContext", tenantContext);

    // Act
    var result = featureAccessFilter.filter(exchange, filterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(billingServiceClient).checkFeatureAccess("tenant-123", "CRM.BULK_IMPORT");
  }

  @Test
  @DisplayName("Should deny access when feature is not available")
  void shouldDenyAccessWhenFeatureNotAvailable() {
    // Arrange
    when(gatewayProperties.featureAccess()).thenReturn(featureAccessProperties);
    when(featureAccessProperties.enabled()).thenReturn(true);
    when(featureAccessProperties.endpointFeatureMapping())
        .thenReturn(Map.of("/api/v1/crm/bulk-import", "CRM.BULK_IMPORT"));

    var featureResponse = new BillingServiceClient.FeatureAccessResponse(false, "FREE", "Feature not available");
    when(billingServiceClient.checkFeatureAccess("tenant-123", "CRM.BULK_IMPORT"))
        .thenReturn(Mono.just(featureResponse));

    var request = MockServerHttpRequest.get("/api/v1/crm/bulk-import").build();
    var exchange = MockServerWebExchange.from(request);

    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put("tenantContext", tenantContext);

    // Act
    var result = featureAccessFilter.filter(exchange, filterChain);

    // Assert
    StepVerifier.create(result)
        .expectError(FeatureNotAvailableException.class)
        .verify();

    verify(billingServiceClient).checkFeatureAccess("tenant-123", "CRM.BULK_IMPORT");
  }

  @Test
  @DisplayName("Should skip feature check when no feature mapping for endpoint")
  void shouldSkipFeatureCheckWhenNoMapping() {
    // Arrange
    when(gatewayProperties.featureAccess()).thenReturn(featureAccessProperties);
    when(featureAccessProperties.enabled()).thenReturn(true);
    when(featureAccessProperties.endpointFeatureMapping())
        .thenReturn(Map.of("/api/v1/crm/bulk-import", "CRM.BULK_IMPORT"));
    when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

    var request = MockServerHttpRequest.get("/api/v1/other/endpoint").build();
    var exchange = MockServerWebExchange.from(request);

    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put("tenantContext", tenantContext);

    // Act
    var result = featureAccessFilter.filter(exchange, filterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(billingServiceClient, never()).checkFeatureAccess(anyString(), anyString());
  }

  @Test
  @DisplayName("Should match wildcard patterns")
  void shouldMatchWildcardPatterns() {
    // Arrange
    when(gatewayProperties.featureAccess()).thenReturn(featureAccessProperties);
    when(featureAccessProperties.enabled()).thenReturn(true);
    when(featureAccessProperties.endpointFeatureMapping())
        .thenReturn(Map.of("/api/v1/crm/**", "CRM.ADVANCED"));
    when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

    var featureResponse = new BillingServiceClient.FeatureAccessResponse(true, "ENTERPRISE", "Feature available");
    when(billingServiceClient.checkFeatureAccess("tenant-123", "CRM.ADVANCED"))
        .thenReturn(Mono.just(featureResponse));

    var request = MockServerHttpRequest.get("/api/v1/crm/bulk-import/execute").build();
    var exchange = MockServerWebExchange.from(request);

    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put("tenantContext", tenantContext);

    // Act
    var result = featureAccessFilter.filter(exchange, filterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(billingServiceClient).checkFeatureAccess("tenant-123", "CRM.ADVANCED");
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    // Assert
    assertThat(featureAccessFilter.getOrder()).isEqualTo(-40);
  }
}
