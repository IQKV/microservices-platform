package org.gripday.gatewayservice.presentation.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.gripday.gatewayservice.config.GripdayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ApiDocumentationResource Tests")
class ApiDocumentationResourceTest {

  private ApiDocumentationResource apiDocumentationResource;
  private GripdayProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    apiDocumentationResource = new ApiDocumentationResource(properties);
    ReflectionTestUtils.setField(apiDocumentationResource, "serverPort", 8080);
  }

  @Test
  @DisplayName("Should return API documentation with gateway and services")
  void shouldReturnApiDocumentationWithGatewayAndServices() {
    var response = apiDocumentationResource.getApiDocumentation();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    var body = response.getBody();
    assertThat(body).containsKeys("gateway", "services", "totalServices", "message");

    @SuppressWarnings("unchecked")
    var gateway = (Map<String, String>) body.get("gateway");
    assertThat(gateway).containsEntry("swaggerUi", "http://localhost:8080/swagger-ui.html");
    assertThat(gateway).containsEntry("apiDocs", "http://localhost:8080/api-docs");
  }

  @Test
  @DisplayName("Should include enabled services with OpenAPI configuration")
  void shouldIncludeEnabledServicesWithOpenApiConfiguration() {
    var response = apiDocumentationResource.getApiDocumentation();
    var body = response.getBody();

    @SuppressWarnings("unchecked")
    var services = (List<Map<String, Object>>) body.get("services");
    assertThat(services).hasSize(1);

    var service = services.get(0);
    assertThat(service).containsEntry("name", "user-service");
    assertThat(service).containsEntry("enabled", true);
  }

  private GripdayProperties createTestProperties() {
    var openApiProps = new GripdayProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
        true, "User Service", "User API", "/users"
    );

    var serviceProps = new GripdayProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080", "/users/**", true, 5000, 30000, openApiProps
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
        jwt, auth, List.of()
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
}
