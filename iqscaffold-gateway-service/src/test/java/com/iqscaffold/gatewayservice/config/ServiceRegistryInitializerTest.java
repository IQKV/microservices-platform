package com.iqscaffold.gatewayservice.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.service.LoadBalancingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceRegistryInitializer Tests")
class ServiceRegistryInitializerTest {

  @Mock
  private LoadBalancingService loadBalancingService;

  private ServiceRegistryInitializer serviceRegistryInitializer;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    serviceRegistryInitializer = new ServiceRegistryInitializer(loadBalancingService, properties);
  }

  @Test
  @DisplayName("Should initialize service instances on application ready")
  void shouldInitializeServiceInstancesOnApplicationReady() {
    serviceRegistryInitializer.initializeServiceInstances();

    verify(loadBalancingService).registerServiceInstances(
        eq("user-service"),
        any()
    );
  }

  @Test
  @DisplayName("Should register enabled services only")
  void shouldRegisterEnabledServicesOnly() {
    serviceRegistryInitializer.initializeServiceInstances();

    verify(loadBalancingService).registerServiceInstances(
        eq("user-service"),
        eq(List.of(URI.create("http://user-service:8080")))
    );
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
        jwt, auth, List.of()
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
}
