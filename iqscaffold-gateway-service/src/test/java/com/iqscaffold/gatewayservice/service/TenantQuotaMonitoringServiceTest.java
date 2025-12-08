package com.iqscaffold.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantQuotaMonitoringService Tests")
class TenantQuotaMonitoringServiceTest {

  @Mock(lenient = true)
  private ReactiveStringRedisTemplate redisTemplate;

  @Mock(lenient = true)
  private ReactiveZSetOperations<String, String> redisZSetOperations;

  private TenantQuotaMonitoringService tenantQuotaMonitoringService;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    tenantQuotaMonitoringService = new TenantQuotaMonitoringService(properties, redisTemplate);
    when(redisTemplate.opsForZSet()).thenReturn(redisZSetOperations);
  }

  @Test
  @DisplayName("Should record tenant request")
  void shouldRecordTenantRequest() {
    var result = tenantQuotaMonitoringService.recordTenantRequest("tenant-123", "/api/users", false);

    assertThat(result).isNotNull();
    result.block();
  }

  @Test
  @DisplayName("Should get tenant usage stats")
  void shouldGetTenantUsageStats() {
    tenantQuotaMonitoringService.recordTenantRequest("tenant-123", "/api/users", false).block();

    var stats = tenantQuotaMonitoringService.getTenantUsageStats("tenant-123").block();

    assertThat(stats).isNotNull();
    assertThat(stats.getTenantId()).isEqualTo("tenant-123");
    assertThat(stats.getTotalRequests()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should get all tenant usage stats")
  void shouldGetAllTenantUsageStats() {
    tenantQuotaMonitoringService.recordTenantRequest("tenant-123", "/api/users", false).block();
    tenantQuotaMonitoringService.recordTenantRequest("tenant-456", "/api/orders", false).block();

    var allStats = tenantQuotaMonitoringService.getAllTenantUsageStats().block();

    assertThat(allStats).isNotNull();
    assertThat(allStats).hasSize(2);
    assertThat(allStats).containsKeys("tenant-123", "tenant-456");
  }

  @Test
  @DisplayName("Should reset tenant usage stats")
  void shouldResetTenantUsageStats() {
    tenantQuotaMonitoringService.recordTenantRequest("tenant-123", "/api/users", false).block();
    tenantQuotaMonitoringService.resetTenantUsageStats("tenant-123").block();

    var stats = tenantQuotaMonitoringService.getTenantUsageStats("tenant-123").block();

    assertThat(stats).isNotNull();
    assertThat(stats.getTotalRequests()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should check if tenant is approaching quota")
  void shouldCheckIfTenantIsApproachingQuota() {
    when(redisTemplate.keys(anyString())).thenReturn(Flux.just("key1", "key2"));
    when(redisZSetOperations.count(anyString(), any())).thenReturn(Mono.just(850L));

    var isApproaching = tenantQuotaMonitoringService.isTenantApproachingQuota("tenant-123").block();

    assertThat(isApproaching).isTrue();
  }

  @Test
  @DisplayName("Should return false when tenant is not approaching quota")
  void shouldReturnFalseWhenTenantIsNotApproachingQuota() {
    when(redisTemplate.keys(anyString())).thenReturn(Flux.just("key1"));
    when(redisZSetOperations.count(anyString(), any())).thenReturn(Mono.just(100L));

    var isApproaching = tenantQuotaMonitoringService.isTenantApproachingQuota("tenant-123").block();

    assertThat(isApproaching).isFalse();
  }

  @Test
  @DisplayName("Should return false when no usage data exists")
  void shouldReturnFalseWhenNoUsageDataExists() {
    when(redisTemplate.keys(anyString())).thenReturn(Flux.empty());

    var isApproaching = tenantQuotaMonitoringService.isTenantApproachingQuota("tenant-123").block();

    assertThat(isApproaching).isFalse();
  }

  @Test
  @DisplayName("TenantUsageStats should track requests correctly")
  void tenantUsageStatsShouldTrackRequestsCorrectly() {
    var stats = new TenantQuotaMonitoringService.TenantUsageStats("tenant-123");

    stats.recordRequest("/api/users", false);
    stats.recordRequest("/api/orders", false);
    stats.recordRequest("/api/users", true);

    assertThat(stats.getTenantId()).isEqualTo("tenant-123");
    assertThat(stats.getTotalRequests()).isEqualTo(3);
    assertThat(stats.getRateLimitedRequests()).isEqualTo(1);
    assertThat(stats.getEndpointCounts()).hasSize(2);
    assertThat(stats.getEndpointCounts().get("/api/users")).isEqualTo(2);
    assertThat(stats.getEndpointCounts().get("/api/orders")).isEqualTo(1);
  }

  @Test
  @DisplayName("TenantUsageStats should calculate rate limited percentage")
  void tenantUsageStatsShouldCalculateRateLimitedPercentage() {
    var stats = new TenantQuotaMonitoringService.TenantUsageStats("tenant-123");

    stats.recordRequest("/api/users", false);
    stats.recordRequest("/api/users", false);
    stats.recordRequest("/api/users", true);
    stats.recordRequest("/api/users", true);

    assertThat(stats.getRateLimitedPercentage()).isEqualTo(50.0);
  }

  @Test
  @DisplayName("TenantUsageStats should return zero percentage when no requests")
  void tenantUsageStatsShouldReturnZeroPercentageWhenNoRequests() {
    var stats = new TenantQuotaMonitoringService.TenantUsageStats("tenant-123");

    assertThat(stats.getRateLimitedPercentage()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("TenantUsageStats should track uptime")
  void tenantUsageStatsShouldTrackUptime() {
    var stats = new TenantQuotaMonitoringService.TenantUsageStats("tenant-123");

    var uptime = stats.getUptime();

    assertThat(uptime).isNotNull();
    assertThat(uptime.toMillis()).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("TenantUsageStats should have immutable endpoint counts")
  void tenantUsageStatsShouldHaveImmutableEndpointCounts() {
    var stats = new TenantQuotaMonitoringService.TenantUsageStats("tenant-123");
    stats.recordRequest("/api/users", false);

    var endpointCounts = stats.getEndpointCounts();

    assertThat(endpointCounts).isUnmodifiable();
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
        jwt, auth, List.of("/health", "/actuator/**")
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

    var integration = new IqScaffoldProperties.GatewayProperties.IntegrationProperties(
        false, "http://localhost:8082", Duration.ofSeconds(5)
    );

    var featureAccess = new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties(
        false, Map.of()
    );

    var gateway = new IqScaffoldProperties.GatewayProperties(
        routing, security, rateLimiting, circuitBreaker, cors, transformation, integration, featureAccess
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
