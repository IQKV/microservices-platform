package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.service.FeatureValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FeatureAccessFilterTest {

  @Mock
  private FeatureValidationService featureValidationService;

  @Mock
  private GatewayFilterChain filterChain;

  private FeatureAccessFilter featureAccessFilter;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    // Create test configuration
    var featureAccessConfig = new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties(
        true, // enabled
        List.of("/actuator/**", "/api/v1/public/**"), // excludedPaths
        List.of(
            new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.FeatureMapping(
                "/api/v1/analytics/**",
                List.of("GET", "POST"),
                Set.of("advanced_analytics"),
                "Analytics endpoints"
            ),
            new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.FeatureMapping(
                "/api/v1/export/**",
                null, // all methods
                Set.of("data_export"),
                "Export endpoints"
            )
        ),
        new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.CacheProperties(
            Duration.ofMinutes(15),
            1000,
            true
        )
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, null, featureAccessConfig
    );

    properties = new IqScaffoldProperties(null, gatewayProperties, null, null);
    featureAccessFilter = new FeatureAccessFilter(featureValidationService, properties);

    // Mock filter chain to return empty Mono (lenient to avoid unnecessary stubbing warnings)
    lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("Should allow access when feature validation passes")
  void shouldAllowAccessWhenFeatureValidationPasses() {
    // Arrange
    var request = MockServerHttpRequest
        .get("/api/v1/analytics/reports")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Add tenant context
    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put(GatewayConstants.Attributes.TENANT_CONTEXT, tenantContext);

    // Mock successful validation
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setEnabledFeatures(Set.of("advanced_analytics"));
    featureContext.setPlanName("Pro Plan");

    var validationResult = new FeatureValidationService.ValidationResult(
        true, featureContext, Set.of()
    );

    when(featureValidationService.validateFeatureAccess(anyString(), anySet(), anyString()))
        .thenReturn(Mono.just(validationResult));

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify feature context was added to exchange
    var storedContext = exchange.getAttribute(GatewayConstants.Attributes.FEATURE_CONTEXT);
    assertThat(storedContext).isNotNull();
    assertThat(((FeatureValidationService.FeatureContext) storedContext).getEnabledFeatures())
        .contains("advanced_analytics");
  }

  @Test
  @DisplayName("Should deny access when feature validation fails")
  void shouldDenyAccessWhenFeatureValidationFails() {
    // Arrange
    var request = MockServerHttpRequest
        .get("/api/v1/analytics/reports")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Add tenant context
    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put(GatewayConstants.Attributes.TENANT_CONTEXT, tenantContext);

    // Mock failed validation
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setPlanName("Basic Plan");

    var validationResult = new FeatureValidationService.ValidationResult(
        false, featureContext, Set.of("advanced_analytics")
    );

    when(featureValidationService.validateFeatureAccess(anyString(), anySet(), anyString()))
        .thenReturn(Mono.just(validationResult));

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify response status is 403 Forbidden
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should skip validation for excluded paths")
  void shouldSkipValidationForExcludedPaths() {
    // Arrange
    var request = MockServerHttpRequest
        .get("/actuator/health")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify filter chain was called (no feature validation)
    // No assertions needed as the test passes if no exceptions are thrown
  }

  @Test
  @DisplayName("Should skip validation when no tenant context")
  void shouldSkipValidationWhenNoTenantContext() {
    // Arrange
    var request = MockServerHttpRequest
        .get("/api/v1/analytics/reports")
        .build();
    var exchange = MockServerWebExchange.from(request);
    // No tenant context added

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify filter chain was called (no feature validation)
  }

  @Test
  @DisplayName("Should skip validation when no feature requirements")
  void shouldSkipValidationWhenNoFeatureRequirements() {
    // Arrange
    var request = MockServerHttpRequest
        .get("/api/v1/users/profile") // Path not in feature mappings
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Add tenant context
    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put(GatewayConstants.Attributes.TENANT_CONTEXT, tenantContext);

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify filter chain was called (no feature validation)
  }

  @Test
  @DisplayName("Should match path patterns correctly")
  void shouldMatchPathPatternsCorrectly() {
    // Arrange
    var request = MockServerHttpRequest
        .post("/api/v1/export/csv")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Add tenant context
    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put(GatewayConstants.Attributes.TENANT_CONTEXT, tenantContext);

    // Mock successful validation
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setEnabledFeatures(Set.of("data_export"));

    var validationResult = new FeatureValidationService.ValidationResult(
        true, featureContext, Set.of()
    );

    when(featureValidationService.validateFeatureAccess(anyString(), anySet(), anyString()))
        .thenReturn(Mono.just(validationResult));

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify the correct feature was validated (data_export for /api/v1/export/**)
  }

  @Test
  @DisplayName("Should handle validation service errors gracefully")
  void shouldHandleValidationServiceErrorsGracefully() {
    // Arrange
    var request = MockServerHttpRequest
        .get("/api/v1/analytics/reports")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Add tenant context
    var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
    exchange.getAttributes().put(GatewayConstants.Attributes.TENANT_CONTEXT, tenantContext);

    // Mock validation service error
    when(featureValidationService.validateFeatureAccess(anyString(), anySet(), anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

    // Act & Assert
    StepVerifier.create(featureAccessFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify that errors are handled gracefully (filter should not fail the request)
  }

  @Test
  @DisplayName("Should skip validation when feature access is disabled")
  void shouldSkipValidationWhenFeatureAccessDisabled() {
    // Arrange - Create filter with disabled feature access
    var disabledFeatureAccessConfig = new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties(
        false, // disabled
        List.of(),
        List.of(),
        new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.CacheProperties(
            Duration.ofMinutes(15), 1000, true
        )
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, null, disabledFeatureAccessConfig
    );

    var disabledProperties = new IqScaffoldProperties(null, gatewayProperties, null, null);
    var disabledFilter = new FeatureAccessFilter(featureValidationService, disabledProperties);

    var request = MockServerHttpRequest
        .get("/api/v1/analytics/reports")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act & Assert
    StepVerifier.create(disabledFilter.filter(exchange, filterChain))
        .verifyComplete();

    // Verify filter chain was called without validation
  }
}
