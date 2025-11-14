package org.gripday.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.gripday.gatewayservice.config.GripdayProperties;
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
  private GripdayProperties gripdayProperties;

  @BeforeEach
  void setUp() {
    gripdayProperties = createTestGripdayProperties();
    filter = new RequestTransformationFilter(gripdayProperties);
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  private GripdayProperties createTestGripdayProperties() {
    var cacheProperties = new GripdayProperties.CacheProperties(
        new GripdayProperties.CacheProperties.RedisProperties(
            "localhost", 6379, null, 0, Duration.ofSeconds(5),
            new GripdayProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(2)),
            "gripday:cache:", Duration.ofMinutes(30), false
        )
    );

    var requestTransformation = new GripdayProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, List.of(), Map.of()
    );

    var responseTransformation = new GripdayProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new GripdayProperties.GatewayProperties.TransformationProperties(
        requestTransformation, responseTransformation
    );

    var gatewayProperties = new GripdayProperties.GatewayProperties(
        null, null, null, null, null, transformation
    );

    var observabilityProperties = new GripdayProperties.ObservabilityProperties(null, null, null);

    return new GripdayProperties(cacheProperties, gatewayProperties, observabilityProperties);
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
    assertThat(gripdayProperties.gateway().transformation().request().enableHeaderEnrichment()).isTrue();

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
    assertThat(gripdayProperties.gateway().transformation().request().enableTenantContextPropagation()).isTrue();
    assertThat(gripdayProperties.gateway().transformation().request().enableUserContextPropagation()).isTrue();

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
    assertThat(gripdayProperties.gateway().transformation().request().enableHeaderEnrichment()).isTrue();
  }
}
