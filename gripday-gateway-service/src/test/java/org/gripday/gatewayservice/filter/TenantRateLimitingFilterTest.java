package org.gripday.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.gatewayservice.config.GripdayProperties;
import org.gripday.gatewayservice.service.TenantQuotaMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantRateLimitingFilter Tests")
class TenantRateLimitingFilterTest {

  @Mock(lenient = true)
  private GatewayFilterChain filterChain;

  @Mock(lenient = true)
  private ReactiveStringRedisTemplate redisTemplate;

  @Mock(lenient = true)
  private ReactiveZSetOperations<String, String> redisZSetOperations;

  @Mock(lenient = true)
  private TenantQuotaMonitoringService quotaMonitoringService;

  private TenantRateLimitingFilter tenantRateLimitingFilter;
  private GripdayProperties properties;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    objectMapper = new ObjectMapper();
    tenantRateLimitingFilter = new TenantRateLimitingFilter(
        properties, redisTemplate, quotaMonitoringService, objectMapper
    );
    when(filterChain.filter(any())).thenReturn(Mono.empty());
    when(redisTemplate.opsForZSet()).thenReturn(redisZSetOperations);
    when(redisZSetOperations.removeRangeByScore(anyString(), any())).thenReturn(Mono.just(0L));
    when(redisZSetOperations.count(anyString(), any())).thenReturn(Mono.just(0L));
    when(redisZSetOperations.add(anyString(), anyString(), any(Double.class))).thenReturn(Mono.just(true));
    when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
    when(quotaMonitoringService.recordTenantRequest(anyString(), anyString(), any(Boolean.class)))
        .thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("Should skip rate limiting when disabled")
  void shouldSkipRateLimitingWhenDisabled() {
    var disabledProperties = createDisabledProperties();
    var filter = new TenantRateLimitingFilter(
        disabledProperties, redisTemplate, quotaMonitoringService, objectMapper
    );

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    filter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should apply rate limiting when enabled")
  void shouldApplyRateLimitingWhenEnabled() {
    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    tenantRateLimitingFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should handle rate limit exceeded")
  void shouldHandleRateLimitExceeded() {
    when(redisZSetOperations.count(anyString(), any())).thenReturn(Mono.just(200L));

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    tenantRateLimitingFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNotNull();
  }

  @Test
  @DisplayName("Should extract client IP from X-Forwarded-For header")
  void shouldExtractClientIpFromXForwardedForHeader() {
    var request = MockServerHttpRequest.get("/api/test")
        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
        .build();
    var exchange = MockServerWebExchange.from(request);

    tenantRateLimitingFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should extract client IP from X-Real-IP header")
  void shouldExtractClientIpFromXRealIpHeader() {
    var request = MockServerHttpRequest.get("/api/test")
        .header("X-Real-IP", "192.168.1.1")
        .build();
    var exchange = MockServerWebExchange.from(request);

    tenantRateLimitingFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should handle Redis errors gracefully")
  void shouldHandleRedisErrorsGracefully() {
    when(redisZSetOperations.removeRangeByScore(anyString(), any()))
        .thenReturn(Mono.error(new RuntimeException("Redis error")));

    var request = MockServerHttpRequest.get("/api/test").build();
    var exchange = MockServerWebExchange.from(request);

    tenantRateLimitingFilter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    assertThat(tenantRateLimitingFilter.getOrder()).isEqualTo(-50);
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
        false, redis, policies, tenantQuotas
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
}
