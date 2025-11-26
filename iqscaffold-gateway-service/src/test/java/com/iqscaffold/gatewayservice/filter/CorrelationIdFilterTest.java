package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

  @Mock(lenient = true)
  private GatewayFilterChain filterChain;

  private CorrelationIdFilter correlationIdFilter;

  @BeforeEach
  void setUp() {
    correlationIdFilter = new CorrelationIdFilter();
    when(filterChain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("Should generate correlation ID when not present")
  void shouldGenerateCorrelationIdWhenNotPresent() {
    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    correlationIdFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getAttributes()).containsKey(GatewayConstants.Attributes.CORRELATION_ID);
  }

  @Test
  @DisplayName("Should use existing correlation ID when present")
  void shouldUseExistingCorrelationIdWhenPresent() {
    var existingId = "existing-correlation-id";
    var request = MockServerHttpRequest.get("/api/test")
        .header(GatewayConstants.Headers.X_CORRELATION_ID, existingId)
        .build();
    var exchange = MockServerWebExchange.from(request);

    correlationIdFilter.filter(exchange, filterChain).block();

    var correlationId = exchange.getAttributes().get(GatewayConstants.Attributes.CORRELATION_ID);
    assertThat(correlationId).isEqualTo(existingId);
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    assertThat(correlationIdFilter.getOrder())
        .isEqualTo(GatewayConstants.FilterOrder.CORRELATION_ID_FILTER);
  }
}
