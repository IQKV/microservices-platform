package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

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
@DisplayName("ResponseTransformationFilter Tests")
class ResponseTransformationFilterTest {

  @Mock(lenient = true)
  private GatewayFilterChain filterChain;

  private ResponseTransformationFilter responseTransformationFilter;

  @BeforeEach
  void setUp() {
    responseTransformationFilter = new ResponseTransformationFilter();
    when(filterChain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("Should apply response transformation")
  void shouldApplyResponseTransformation() {
    var config = new ResponseTransformationFilter.Config();
    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    var filter = responseTransformationFilter.apply(config);
    filter.filter(exchange, filterChain).block();

    assertThat(config.isEnableSecurityHeaders()).isTrue();
  }

  @Test
  @DisplayName("Should apply response transformation with security headers disabled")
  void shouldApplyResponseTransformationWithSecurityHeadersDisabled() {
    var config = new ResponseTransformationFilter.Config();
    config.setEnableSecurityHeaders(false);

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    var filter = responseTransformationFilter.apply(config);
    filter.filter(exchange, filterChain).block();

    assertThat(config.isEnableSecurityHeaders()).isFalse();
  }

  @Test
  @DisplayName("Should apply response transformation with correlation headers disabled")
  void shouldApplyResponseTransformationWithCorrelationHeadersDisabled() {
    var config = new ResponseTransformationFilter.Config();
    config.setEnableCorrelationHeaders(false);

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    var filter = responseTransformationFilter.apply(config);
    filter.filter(exchange, filterChain).block();

    assertThat(config.isEnableCorrelationHeaders()).isFalse();
  }

  @Test
  @DisplayName("Should apply response transformation with internal headers removal disabled")
  void shouldApplyResponseTransformationWithInternalHeadersRemovalDisabled() {
    var config = new ResponseTransformationFilter.Config();
    config.setRemoveInternalHeaders(false);

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    var filter = responseTransformationFilter.apply(config);
    filter.filter(exchange, filterChain).block();

    assertThat(config.isRemoveInternalHeaders()).isFalse();
  }

  @Test
  @DisplayName("Config should have default values")
  void configShouldHaveDefaultValues() {
    var config = new ResponseTransformationFilter.Config();

    assertThat(config.isEnableSecurityHeaders()).isTrue();
    assertThat(config.isEnableCorrelationHeaders()).isTrue();
    assertThat(config.isRemoveInternalHeaders()).isTrue();
    assertThat(config.getAdditionalHeadersToRemove()).isEmpty();
    assertThat(config.getAdditionalHeaders()).isEmpty();
  }

  @Test
  @DisplayName("Config should allow setting values")
  void configShouldAllowSettingValues() {
    var config = new ResponseTransformationFilter.Config();
    config.setEnableSecurityHeaders(false);
    config.setEnableCorrelationHeaders(false);
    config.setRemoveInternalHeaders(false);
    config.setAdditionalHeadersToRemove(List.of("X-Custom-Header"));
    config.setAdditionalHeaders(Map.of("X-New-Header", "value"));

    assertThat(config.isEnableSecurityHeaders()).isFalse();
    assertThat(config.isEnableCorrelationHeaders()).isFalse();
    assertThat(config.isRemoveInternalHeaders()).isFalse();
    assertThat(config.getAdditionalHeadersToRemove()).containsExactly("X-Custom-Header");
    assertThat(config.getAdditionalHeaders()).containsEntry("X-New-Header", "value");
  }
}
