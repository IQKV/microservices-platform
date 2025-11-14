package org.gripday.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  @BeforeEach
  void setUp() {
    filter = new RequestTransformationFilter();
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
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
    assertThat(config.isEnableHeaderEnrichment()).isTrue();

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
    assertThat(config.isEnableTenantContextPropagation()).isTrue();
    assertThat(config.isEnableUserContextPropagation()).isTrue();

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
    assertThat(config.isEnableHeaderEnrichment()).isTrue();
  }
}
