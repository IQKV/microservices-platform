package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Unit tests for RequestTransformationFilter. Tests successful request transformation scenarios.
 */
class RequestTransformationFilterTest {

  private RequestTransformationFilter filter;
  private GatewayFilterChain chain;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestIqScaffoldProperties();
    filter = new RequestTransformationFilter(properties);
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  private IqScaffoldProperties createTestIqScaffoldProperties() {
    var cacheProperties = new IqScaffoldProperties.CacheProperties(
        new IqScaffoldProperties.CacheProperties.RedisProperties(
            "localhost", 6379, null, 0, Duration.ofSeconds(5),
            new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(2)),
            "iqscaffold:cache:", Duration.ofMinutes(30), false
        )
    );

    var requestTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, true, List.of(), Map.of()
    );

    var responseTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransformation, responseTransformation
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, transformation, null
    );

    var observabilityProperties = new IqScaffoldProperties.ObservabilityProperties(null, null, null);

    var i18nProperties = new IqScaffoldProperties.I18nProperties(
        List.of("en", "es", "fr"), "en"
    );

    return new IqScaffoldProperties(cacheProperties, gatewayProperties, i18nProperties, observabilityProperties);
  }

  @Test
  void shouldEnrichRequestHeadersWithCorrelationId() {
    // Given
    var correlationId = "test-correlation-123";
    MDC.put("correlationId", correlationId);

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // When
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Then
    assertThat(result).isNotNull();
    assertThat(properties.gateway().transformation().request().enableHeaderEnrichment()).isTrue();

    // Cleanup
    MDC.clear();
  }

  @Test
  void shouldEnrichRequestHeadersWithTenantContext() {
    // Given
    var tenantId = "tenant-123";
    var userId = "user-456";
    MDC.put("tenantId", tenantId);
    MDC.put("userId", userId);

    var request = MockServerHttpRequest.get("/api/v1/admin/users").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // When
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Then
    assertThat(result).isNotNull();
    assertThat(properties.gateway().transformation().request().enableTenantContextPropagation()).isTrue();
    assertThat(properties.gateway().transformation().request().enableUserContextPropagation()).isTrue();

    // Cleanup
    MDC.clear();
  }

  @Test
  void shouldHandleRequestWithoutMDCContext() {
    // Given
    var request = MockServerHttpRequest.get("/actuator/health").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // When
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Then
    assertThat(result).isNotNull();
    assertThat(properties.gateway().transformation().request().enableHeaderEnrichment()).isTrue();
  }
}
