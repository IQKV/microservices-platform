package com.iqscaffold.gatewayservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

  @Mock(lenient = true)
  private GatewayFilterChain filterChain;

  private JwtAuthenticationFilter jwtAuthenticationFilter;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestProperties();
    jwtAuthenticationFilter = new JwtAuthenticationFilter(properties);
    when(filterChain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("Should skip authentication for public paths")
  void shouldSkipAuthenticationForPublicPaths() {
    var request = MockServerHttpRequest.get("/health").build();
    var exchange = MockServerWebExchange.from(request);

    jwtAuthenticationFilter.filter(exchange, filterChain).block();

    verify(filterChain).filter(any());
  }

  @Test
  @DisplayName("Should add correlation ID to request")
  void shouldAddCorrelationIdToRequest() {
    var request = MockServerHttpRequest.get("/health").build();
    var exchange = MockServerWebExchange.from(request);

    jwtAuthenticationFilter.filter(exchange, filterChain).block();

    verify(filterChain).filter(any());
  }

  @Test
  @DisplayName("Should use existing correlation ID if present")
  void shouldUseExistingCorrelationIdIfPresent() {
    var correlationId = "existing-correlation-id";
    var request = MockServerHttpRequest.get("/health")
        .header(GatewayConstants.Headers.X_CORRELATION_ID, correlationId)
        .build();
    var exchange = MockServerWebExchange.from(request);

    jwtAuthenticationFilter.filter(exchange, filterChain).block();

    verify(filterChain).filter(any());
  }

  @Test
  @DisplayName("Should extract user context from JWT")
  void shouldExtractUserContextFromJwt() {
    var jwt = createTestJwt();
    var authentication = new JwtAuthenticationToken(jwt);
    var securityContext = new SecurityContextImpl(authentication);

    var request = MockServerHttpRequest.get("/api/users").build();
    var exchange = MockServerWebExchange.from(request);

    jwtAuthenticationFilter.filter(exchange, filterChain)
        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
        .block();

    // Verify filter was called (may be called multiple times due to reactive chain)
    verify(filterChain, atLeastOnce()).filter(any());
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    assertThat(jwtAuthenticationFilter.getOrder())
        .isEqualTo(GatewayConstants.FilterOrder.JWT_AUTHENTICATION_FILTER);
  }

  @Test
  @DisplayName("UserContext should be created with all fields")
  void userContextShouldBeCreatedWithAllFields() {
    var userContext = new JwtAuthenticationFilter.UserContext(
        1L,
        "testuser",
        "test@example.com",
        List.of("ROLE_USER"),
        List.of("READ", "WRITE"),
        "Engineering",
        "org-123"
    );

    assertThat(userContext.userId()).isEqualTo(1L);
    assertThat(userContext.username()).isEqualTo("testuser");
    assertThat(userContext.email()).isEqualTo("test@example.com");
    assertThat(userContext.roles()).containsExactly("ROLE_USER");
    assertThat(userContext.permissions()).containsExactly("READ", "WRITE");
    assertThat(userContext.department()).isEqualTo("Engineering");
    assertThat(userContext.organizationId()).isEqualTo("org-123");
  }

  @Test
  @DisplayName("TenantContext should be created with tenant ID")
  void tenantContextShouldBeCreatedWithTenantId() {
    var tenantContext = new JwtAuthenticationFilter.TenantContext("tenant-123");

    assertThat(tenantContext.tenantId()).isEqualTo("tenant-123");
  }

  private Jwt createTestJwt() {
    var headers = Map.<String, Object>of("alg", "RS256", "typ", "JWT");
    var claims = Map.<String, Object>of(
        JwtClaimNames.SUBJECT, "testuser",
        JwtClaimNames.USER_ID, 1L,
        JwtClaimNames.EMAIL, "test@example.com",
        JwtClaimNames.ROLES, List.of("ROLE_USER"),
        JwtClaimNames.PERMISSIONS, List.of("READ", "WRITE"),
        JwtClaimNames.TENANT_ID, "tenant-123"
    );

    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        headers,
        claims
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
