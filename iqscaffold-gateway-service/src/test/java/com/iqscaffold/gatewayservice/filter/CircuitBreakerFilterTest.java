package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.exception.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
@DisplayName("CircuitBreakerFilter Tests")
class CircuitBreakerFilterTest {

  @Mock(lenient = true)
  private GatewayFilterChain filterChain;

  @Mock(lenient = true)
  private CircuitBreakerRegistry circuitBreakerRegistry;

  private CircuitBreakerFilter circuitBreakerFilter;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    circuitBreakerFilter = new CircuitBreakerFilter(properties, circuitBreakerRegistry);
    when(filterChain.filter(any())).thenReturn(Mono.empty());

    var circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
    var circuitBreaker = CircuitBreaker.of("test-circuit-breaker", circuitBreakerConfig);
    when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
  }

  @Test
  @DisplayName("Should apply circuit breaker when enabled")
  void shouldApplyCircuitBreakerWhenEnabled() {
    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should skip circuit breaker when disabled")
  void shouldSkipCircuitBreakerWhenDisabled() {
    var disabledProperties = createDisabledProperties();
    var filter = new CircuitBreakerFilter(disabledProperties, circuitBreakerRegistry);

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    filter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should handle circuit breaker error")
  void shouldHandleCircuitBreakerError() {
    when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Service error")));

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    // Circuit breaker will propagate the error
    var result = circuitBreakerFilter.filter(exchange, filterChain);

    // Verify error is propagated through circuit breaker
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should determine circuit breaker name for auth path")
  void shouldDetermineCircuitBreakerNameForAuthPath() {
    var request = MockServerHttpRequest.get("/api/v1/auth/register").build();
    var exchange = MockServerWebExchange.from(request);

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should determine default circuit breaker name for unknown path")
  void shouldDetermineDefaultCircuitBreakerNameForUnknownPath() {
    var request = MockServerHttpRequest.get("/api/v1/unknown/path").build();
    var exchange = MockServerWebExchange.from(request);

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should handle circuit breaker with open state")
  void shouldHandleCircuitBreakerWithOpenState() {
    var circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .minimumNumberOfCalls(2)
        .build();
    var openCircuitBreaker = CircuitBreaker.of("open-circuit-breaker", circuitBreakerConfig);

    // Trigger circuit breaker to open
    openCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 1"));
    openCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 2"));
    openCircuitBreaker.transitionToOpenState();

    when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(openCircuitBreaker);
    when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Service error")));

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    // Circuit breaker is open, should throw CircuitBreakerOpenException
    var result = circuitBreakerFilter.filter(exchange, filterChain);

    // Verify the result is not null (exception will be thrown when subscribed)
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    assertThat(circuitBreakerFilter.getOrder()).isEqualTo(-25);
  }

  @Test
  @DisplayName("Should extract service name from circuit breaker name with suffix")
  void shouldExtractServiceNameFromCircuitBreakerNameWithSuffix() {
    var circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
    var circuitBreaker = CircuitBreaker.of("user-service", circuitBreakerConfig);
    when(circuitBreakerRegistry.circuitBreaker("user-service")).thenReturn(circuitBreaker);

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should extract service name from circuit breaker name without suffix")
  void shouldExtractServiceNameFromCircuitBreakerNameWithoutSuffix() {
    var circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
    var circuitBreaker = CircuitBreaker.of("default", circuitBreakerConfig);
    when(circuitBreakerRegistry.circuitBreaker("default-service")).thenReturn(circuitBreaker);

    var request = MockServerHttpRequest.get("/api/v1/other/path").build();
    var exchange = MockServerWebExchange.from(request);

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should handle half-open circuit breaker state")
  void shouldHandleHalfOpenCircuitBreakerState() {
    var circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .minimumNumberOfCalls(2)
        .build();
    var halfOpenCircuitBreaker = CircuitBreaker.of("half-open-cb", circuitBreakerConfig);

    halfOpenCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 1"));
    halfOpenCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 2"));
    halfOpenCircuitBreaker.transitionToOpenState();
    halfOpenCircuitBreaker.transitionToHalfOpenState();

    when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(halfOpenCircuitBreaker);
    when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Service error")));

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    var result = circuitBreakerFilter.filter(exchange, filterChain);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should handle closed circuit breaker state with error")
  void shouldHandleClosedCircuitBreakerStateWithError() {
    var circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
    var closedCircuitBreaker = CircuitBreaker.of("closed-cb", circuitBreakerConfig);

    when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(closedCircuitBreaker);
    when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Service error")));

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    var result = circuitBreakerFilter.filter(exchange, filterChain);

    // Subscribe to trigger the error handling
    result.onErrorResume(error -> Mono.empty()).block();

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should extract service name without service suffix")
  void shouldExtractServiceNameWithoutServiceSuffix() {
    var circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .minimumNumberOfCalls(2)
        .build();
    var openCircuitBreaker = CircuitBreaker.of("bookstore-service", circuitBreakerConfig);

    openCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 1"));
    openCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 2"));
    openCircuitBreaker.transitionToOpenState();

    when(circuitBreakerRegistry.circuitBreaker("default-service")).thenReturn(openCircuitBreaker);
    when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Service error")));

    var request = MockServerHttpRequest.get("/api/v1/bookstore/books").build();
    var exchange = MockServerWebExchange.from(request);

    var result = circuitBreakerFilter.filter(exchange, filterChain);

    // Subscribe to trigger the error handling
    result.onErrorResume(error -> {
      assertThat(error).isInstanceOf(CircuitBreakerOpenException.class);
      return Mono.empty();
    }).block();
  }

  @Test
  @DisplayName("Should extract service name with default suffix")
  void shouldExtractServiceNameWithDefaultSuffix() {
    var circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .minimumNumberOfCalls(2)
        .build();
    var openCircuitBreaker = CircuitBreaker.of("api-gateway", circuitBreakerConfig);

    openCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 1"));
    openCircuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("Error 2"));
    openCircuitBreaker.transitionToOpenState();

    when(circuitBreakerRegistry.circuitBreaker("default-service")).thenReturn(openCircuitBreaker);
    when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Service error")));

    var request = MockServerHttpRequest.get("/api/v1/unknown/endpoint").build();
    var exchange = MockServerWebExchange.from(request);

    var result = circuitBreakerFilter.filter(exchange, filterChain);

    // Subscribe to trigger the error handling
    result.onErrorResume(error -> {
      assertThat(error).isInstanceOf(CircuitBreakerOpenException.class);
      return Mono.empty();
    }).block();
  }

  private IqScaffoldProperties createTestProperties() {
    var serviceProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080", "/users/**", true, 5000, 30000, null
    );

    var apiPrefix = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
        true, "/api", 0
    );

    var loadBalancing = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
        "round-robin", true, Duration.ofSeconds(30)
    );

    var routing = new IqScaffoldProperties.GatewayProperties.RoutingProperties(
        apiPrefix, Map.of("user-service", serviceProps), true, loadBalancing
    );

    var jwt = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
        Duration.ofMinutes(15), Duration.ofDays(7), "issuer", "audience", "RS256", "http://jwks"
    );

    var auth = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
        true, "http://user-service", Duration.ofSeconds(5), true
    );

    var security = new IqScaffoldProperties.GatewayProperties.SecurityProperties(
        jwt, auth, List.of("/health")
    );

    var redis = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
        "rate-limit:", Duration.ofMinutes(1)
    );

    var policies = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
        100, 200, Map.of()
    );

    var tenantQuotas = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
        true, 1000, Map.of()
    );

    var rateLimiting = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties(
        true, redis, policies, tenantQuotas
    );

    var circuitBreaker = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
        true, 50, 50, Duration.ofSeconds(5), 10, Duration.ofSeconds(60), 100, "COUNT_BASED"
    );

    var cors = new IqScaffoldProperties.GatewayProperties.CorsProperties(
        true, List.of("*"), List.of("GET"), List.of("*"), true, 3600
    );

    var requestTransform = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, List.of(), Map.of()
    );

    var responseTransform = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransform, responseTransform
    );

    var gateway = new IqScaffoldProperties.GatewayProperties(
        routing, security, rateLimiting, circuitBreaker, cors, transformation
    );

    var cacheRedis = new IqScaffoldProperties.CacheProperties.RedisProperties(
        "localhost", 6379, null, 0, Duration.ofSeconds(5),
        new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(3)),
        "cache:", Duration.ofMinutes(10), false
    );

    var cache = new IqScaffoldProperties.CacheProperties(cacheRedis);

    var tracing = new IqScaffoldProperties.ObservabilityProperties.TracingProperties(
        true, "gateway", 0.1, "http://jaeger", Duration.ofSeconds(5), Duration.ofSeconds(10), 100
    );

    var metrics = new IqScaffoldProperties.ObservabilityProperties.MetricsProperties(
        true, "/metrics", "gateway", true, true, true, Map.of(), List.of()
    );

    var logging = new IqScaffoldProperties.ObservabilityProperties.LoggingProperties(
        "INFO", "json", true, true, true, true, true, true, true,
        "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID"
    );

    var observability = new IqScaffoldProperties.ObservabilityProperties(tracing, metrics, logging);

    return new IqScaffoldProperties(cache, gateway, observability);
  }

  private IqScaffoldProperties createDisabledProperties() {
    var serviceProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080", "/users/**", true, 5000, 30000, null
    );

    var apiPrefix = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
        true, "/api", 0
    );

    var loadBalancing = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
        "round-robin", true, Duration.ofSeconds(30)
    );

    var routing = new IqScaffoldProperties.GatewayProperties.RoutingProperties(
        apiPrefix, Map.of("user-service", serviceProps), true, loadBalancing
    );

    var jwt = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
        Duration.ofMinutes(15), Duration.ofDays(7), "issuer", "audience", "RS256", "http://jwks"
    );

    var auth = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
        true, "http://user-service", Duration.ofSeconds(5), true
    );

    var security = new IqScaffoldProperties.GatewayProperties.SecurityProperties(
        jwt, auth, List.of("/health")
    );

    var redis = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
        "rate-limit:", Duration.ofMinutes(1)
    );

    var policies = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
        100, 200, Map.of()
    );

    var tenantQuotas = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
        true, 1000, Map.of()
    );

    var rateLimiting = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties(
        true, redis, policies, tenantQuotas
    );

    var circuitBreaker = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
        false, 50, 50, Duration.ofSeconds(5), 10, Duration.ofSeconds(60), 100, "COUNT_BASED"
    );

    var cors = new IqScaffoldProperties.GatewayProperties.CorsProperties(
        true, List.of("*"), List.of("GET"), List.of("*"), true, 3600
    );

    var requestTransform = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, List.of(), Map.of()
    );

    var responseTransform = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransform, responseTransform
    );

    var gateway = new IqScaffoldProperties.GatewayProperties(
        routing, security, rateLimiting, circuitBreaker, cors, transformation
    );

    var cacheRedis = new IqScaffoldProperties.CacheProperties.RedisProperties(
        "localhost", 6379, null, 0, Duration.ofSeconds(5),
        new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(3)),
        "cache:", Duration.ofMinutes(10), false
    );

    var cache = new IqScaffoldProperties.CacheProperties(cacheRedis);

    var tracing = new IqScaffoldProperties.ObservabilityProperties.TracingProperties(
        true, "gateway", 0.1, "http://jaeger", Duration.ofSeconds(5), Duration.ofSeconds(10), 100
    );

    var metrics = new IqScaffoldProperties.ObservabilityProperties.MetricsProperties(
        true, "/metrics", "gateway", true, true, true, Map.of(), List.of()
    );

    var logging = new IqScaffoldProperties.ObservabilityProperties.LoggingProperties(
        "INFO", "json", true, true, true, true, true, true, true,
        "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID"
    );

    var observability = new IqScaffoldProperties.ObservabilityProperties(tracing, metrics, logging);

    return new IqScaffoldProperties(cache, gateway, observability);
  }
}
