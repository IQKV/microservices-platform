package com.iqscaffold.gatewayservice.presentation.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ApiDocumentationResource Tests")
class ApiDocumentationResourceTest {

  private ApiDocumentationResource apiDocumentationResource;
  private IqScaffoldProperties properties;

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
    assertThat(body).isNotNull();
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
    assertThat(body).isNotNull();

    @SuppressWarnings("unchecked")
    var services = (List<Map<String, Object>>) body.get("services");
    assertThat(services).hasSize(1);

    var service = services.get(0);
    assertThat(service).containsEntry("name", "user-service");
    assertThat(service).containsEntry("enabled", true);
  }

  private IqScaffoldProperties createTestProperties() {
    var openApiProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
        true, "User Service", "User API", "/users"
    );

    var serviceProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080", "/users/**", true, 5000, 30000, openApiProps
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
        true, true, true, true, true, List.of(), Map.of()
    );

    var responseTransform = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransform, responseTransform
    );

    var gateway = new IqScaffoldProperties.GatewayProperties(
        routing, security, rateLimiting, circuitBreaker, cors, transformation, null);

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

    var i18nProperties = new IqScaffoldProperties.I18nProperties(
        List.of("en", "es", "fr"), "en"
    );

    return new IqScaffoldProperties(cache, gateway, i18nProperties, observability);
  }
}


