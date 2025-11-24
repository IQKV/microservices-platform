package org.gripday.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.gripday.gatewayservice.config.GripdayProperties;
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
  private GripdayProperties properties;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    objectMapper = new ObjectMapper();
    circuitBreakerFilter = new CircuitBreakerFilter(properties, circuitBreakerRegistry, objectMapper);
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
    var filter = new CircuitBreakerFilter(disabledProperties, circuitBreakerRegistry, objectMapper);

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

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNotNull();
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

    circuitBreakerFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNotNull();
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    assertThat(circuitBreakerFilter.getOrder()).isEqualTo(-25);
  }

  private GripdayProperties createTestProperties() {
    var serviceProps = new GripdayProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080", "/users/**", true, 5000, 30000, null
    );

    var apiPrefix = new GripdayProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
        true, "/api", 0
    );

    var loadBalancing = new GripdayProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
        "round-robin", true, Duration.ofSeconds(30)
    );

    var routing = new GripdayProperties.GatewayProperties.RoutingProperties(
        apiPrefix, Map.of("user-service", serviceProps), true, loadBalancing
    );

    var jwt = new GripdayProperties.GatewayProperties.SecurityProperties.JwtProperties(
        Duration.ofMinutes(15), Duration.ofDays(7), "issuer", "audience", "RS256", "http://jwks"
    );

    var auth = new GripdayProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
        true, "http://user-service", Duration.ofSeconds(5), true
    );

    var security = new GripdayProperties.GatewayProperties.SecurityProperties(
        jwt, auth, List.of("/health")
    );

    var redis = new GripdayProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
        "rate-limit:", Duration.ofMinutes(1)
    );

    var policies = new GripdayProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
        100, 200, Map.of()
    );

    var tenantQuotas = new GripdayProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
        true, 1000, Map.of()
    );

    var rateLimiting = new GripdayProperties.GatewayProperties.RateLimitingProperties(
        true, redis, policies, tenantQuotas
    );

    var circuitBreaker = new GripdayProperties.GatewayProperties.CircuitBreakerProperties(
        true, 50, 50, Duration.ofSeconds(5), 10, Duration.ofSeconds(60), 100, "COUNT_BASED"
    );

    var cors = new GripdayProperties.GatewayProperties.CorsProperties(
        true, List.of("*"), List.of("GET"), List.of("*"), true, 3600
    );

    var requestTransform = new GripdayProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, List.of(), Map.of()
    );

    var responseTransform = new GripdayProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new GripdayProperties.GatewayProperties.TransformationProperties(
        requestTransform, responseTransform
    );

    var gateway = new GripdayProperties.GatewayProperties(
        routing, security, rateLimiting, circuitBreaker, cors, transformation
    );

    var cacheRedis = new GripdayProperties.CacheProperties.RedisProperties(
        "localhost", 6379, null, 0, Duration.ofSeconds(5),
        new GripdayProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(3)),
        "cache:", Duration.ofMinutes(10), false
    );

    var cache = new GripdayProperties.CacheProperties(cacheRedis);

    var tracing = new GripdayProperties.ObservabilityProperties.TracingProperties(
        true, "gateway", 0.1, "http://jaeger", Duration.ofSeconds(5), Duration.ofSeconds(10), 100
    );

    var metrics = new GripdayProperties.ObservabilityProperties.MetricsProperties(
        true, "/metrics", "gateway", true, true, true, Map.of(), List.of()
    );

    var logging = new GripdayProperties.ObservabilityProperties.LoggingProperties(
        "INFO", "json", true, true, true, true, true, true, true,
        "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID"
    );

    var observability = new GripdayProperties.ObservabilityProperties(tracing, metrics, logging);

    return new GripdayProperties(cache, gateway, observability);
  }

  private GripdayProperties createDisabledProperties() {
    var serviceProps = new GripdayProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080", "/users/**", true, 5000, 30000, null
    );

    var apiPrefix = new GripdayProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
        true, "/api", 0
    );

    var loadBalancing = new GripdayProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
        "round-robin", true, Duration.ofSeconds(30)
    );

    var routing = new GripdayProperties.GatewayProperties.RoutingProperties(
        apiPrefix, Map.of("user-service", serviceProps), true, loadBalancing
    );

    var jwt = new GripdayProperties.GatewayProperties.SecurityProperties.JwtProperties(
        Duration.ofMinutes(15), Duration.ofDays(7), "issuer", "audience", "RS256", "http://jwks"
    );

    var auth = new GripdayProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
        true, "http://user-service", Duration.ofSeconds(5), true
    );

    var security = new GripdayProperties.GatewayProperties.SecurityProperties(
        jwt, auth, List.of("/health")
    );

    var redis = new GripdayProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
        "rate-limit:", Duration.ofMinutes(1)
    );

    var policies = new GripdayProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
        100, 200, Map.of()
    );

    var tenantQuotas = new GripdayProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
        true, 1000, Map.of()
    );

    var rateLimiting = new GripdayProperties.GatewayProperties.RateLimitingProperties(
        true, redis, policies, tenantQuotas
    );

    var circuitBreaker = new GripdayProperties.GatewayProperties.CircuitBreakerProperties(
        false, 50, 50, Duration.ofSeconds(5), 10, Duration.ofSeconds(60), 100, "COUNT_BASED"
    );

    var cors = new GripdayProperties.GatewayProperties.CorsProperties(
        true, List.of("*"), List.of("GET"), List.of("*"), true, 3600
    );

    var requestTransform = new GripdayProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, List.of(), Map.of()
    );

    var responseTransform = new GripdayProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new GripdayProperties.GatewayProperties.TransformationProperties(
        requestTransform, responseTransform
    );

    var gateway = new GripdayProperties.GatewayProperties(
        routing, security, rateLimiting, circuitBreaker, cors, transformation
    );

    var cacheRedis = new GripdayProperties.CacheProperties.RedisProperties(
        "localhost", 6379, null, 0, Duration.ofSeconds(5),
        new GripdayProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(3)),
        "cache:", Duration.ofMinutes(10), false
    );

    var cache = new GripdayProperties.CacheProperties(cacheRedis);

    var tracing = new GripdayProperties.ObservabilityProperties.TracingProperties(
        true, "gateway", 0.1, "http://jaeger", Duration.ofSeconds(5), Duration.ofSeconds(10), 100
    );

    var metrics = new GripdayProperties.ObservabilityProperties.MetricsProperties(
        true, "/metrics", "gateway", true, true, true, Map.of(), List.of()
    );

    var logging = new GripdayProperties.ObservabilityProperties.LoggingProperties(
        "INFO", "json", true, true, true, true, true, true, true,
        "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID"
    );

    var observability = new GripdayProperties.ObservabilityProperties(tracing, metrics, logging);

    return new GripdayProperties(cache, gateway, observability);
  }
}
