package com.iqscaffold.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GatewayProperties Tests")
class GatewayPropertiesTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Nested
  @DisplayName("GatewayProperties Validation Tests")
  class GatewayPropertiesValidationTests {

    @Test
    @DisplayName("Should validate successfully with all valid properties")
    void shouldValidateSuccessfullyWithValidProperties() {
      var gatewayProperties = createValidGatewayProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties>> violations = validator.validate(gatewayProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when routing is null")
    void shouldFailValidationWhenRoutingIsNull() {
      var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
          null,
          createValidSecurityProperties(),
          createValidRateLimitingProperties(),
          createValidCircuitBreakerProperties(),
          createValidCorsProperties(),
          createValidTransformationProperties(),
          createValidFeatureAccessProperties()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties>> violations = validator.validate(gatewayProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getMessage().contains("must not be null"));
    }
  }

  @Nested
  @DisplayName("RoutingProperties Tests")
  class RoutingPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid routing properties")
    void shouldValidateSuccessfullyWithValidRoutingProperties() {
      var routingProperties = createValidRoutingProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties>> violations = validator.validate(routingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when apiPrefix is null")
    void shouldFailValidationWhenApiPrefixIsNull() {
      var routingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties(
          null,
          Map.of(),
          true,
          createValidLoadBalancingProperties()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties>> violations = validator.validate(routingProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when loadBalancing is null")
    void shouldFailValidationWhenLoadBalancingIsNull() {
      var routingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties(
          createValidApiPrefixProperties(),
          Map.of(),
          true,
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties>> violations = validator.validate(routingProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("ApiPrefixProperties Tests")
  class ApiPrefixPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid API prefix properties")
    void shouldValidateSuccessfullyWithValidApiPrefixProperties() {
      var apiPrefixProperties = createValidApiPrefixProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(apiPrefixProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when prefix is blank")
    void shouldFailValidationWhenPrefixIsBlank() {
      var apiPrefixProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
          true,
          "",
          0
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(apiPrefixProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prefix"));
    }

    @Test
    @DisplayName("Should fail validation when stripCount is negative")
    void shouldFailValidationWhenStripCountIsNegative() {
      var apiPrefixProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
          true,
          "/api",
          -1
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(apiPrefixProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stripCount"));
    }

    @Test
    @DisplayName("Should fail validation when stripCount exceeds maximum")
    void shouldFailValidationWhenStripCountExceedsMaximum() {
      var apiPrefixProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
          true,
          "/api",
          6
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(apiPrefixProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stripCount"));
    }
  }

  @Nested
  @DisplayName("ServiceProperties Tests")
  class ServicePropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid service properties")
    void shouldValidateSuccessfullyWithValidServiceProperties() {
      var serviceProperties = createValidServiceProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(serviceProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when uri is blank")
    void shouldFailValidationWhenUriIsBlank() {
      var serviceProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "",
          "/users/**",
          true,
          5000,
          30000,
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(serviceProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("uri"));
    }

    @Test
    @DisplayName("Should fail validation when path is blank")
    void shouldFailValidationWhenPathIsBlank() {
      var serviceProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "http://user-service:8080",
          "",
          true,
          5000,
          30000,
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(serviceProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("path"));
    }

    @Test
    @DisplayName("Should fail validation when connectTimeout is not positive")
    void shouldFailValidationWhenConnectTimeoutIsNotPositive() {
      var serviceProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "http://user-service:8080",
          "/users/**",
          true,
          0,
          30000,
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(serviceProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("connectTimeout"));
    }

    @Test
    @DisplayName("Should fail validation when responseTimeout is not positive")
    void shouldFailValidationWhenResponseTimeoutIsNotPositive() {
      var serviceProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "http://user-service:8080",
          "/users/**",
          true,
          5000,
          -1,
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(serviceProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("responseTimeout"));
    }
  }

  @Nested
  @DisplayName("OpenApiProperties Tests")
  class OpenApiPropertiesTests {

    @Test
    @DisplayName("Should apply default values when fields are null")
    void shouldApplyDefaultValuesWhenFieldsAreNull() {
      var openApiProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
          true,
          null,
          null,
          null
      );

      assertThat(openApiProperties.displayName()).isEqualTo("Service API");
      assertThat(openApiProperties.description()).isEqualTo("API documentation");
      assertThat(openApiProperties.contextPath()).isEqualTo("");
    }

    @Test
    @DisplayName("Should apply default values when fields are blank")
    void shouldApplyDefaultValuesWhenFieldsAreBlank() {
      var openApiProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
          true,
          "",
          "  ",
          ""
      );

      assertThat(openApiProperties.displayName()).isEqualTo("Service API");
      assertThat(openApiProperties.description()).isEqualTo("API documentation");
      assertThat(openApiProperties.contextPath()).isEqualTo("");
    }

    @Test
    @DisplayName("Should preserve custom values when provided")
    void shouldPreserveCustomValuesWhenProvided() {
      var openApiProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
          true,
          "User Service",
          "User management API",
          "/api/users"
      );

      assertThat(openApiProperties.displayName()).isEqualTo("User Service");
      assertThat(openApiProperties.description()).isEqualTo("User management API");
      assertThat(openApiProperties.contextPath()).isEqualTo("/api/users");
    }
  }

  @Nested
  @DisplayName("LoadBalancingProperties Tests")
  class LoadBalancingPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with round-robin strategy")
    void shouldValidateSuccessfullyWithRoundRobinStrategy() {
      var loadBalancingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
          "round-robin",
          true,
          Duration.ofSeconds(30)
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties>> violations = validator.validate(loadBalancingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully with weighted strategy")
    void shouldValidateSuccessfullyWithWeightedStrategy() {
      var loadBalancingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
          "weighted",
          true,
          Duration.ofSeconds(30)
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties>> violations = validator.validate(loadBalancingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully with least-connections strategy")
    void shouldValidateSuccessfullyWithLeastConnectionsStrategy() {
      var loadBalancingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
          "least-connections",
          true,
          Duration.ofSeconds(30)
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties>> violations = validator.validate(loadBalancingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid strategy")
    void shouldFailValidationWithInvalidStrategy() {
      var loadBalancingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
          "invalid-strategy",
          true,
          Duration.ofSeconds(30)
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties>> violations = validator.validate(loadBalancingProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("strategy"));
    }

    @Test
    @DisplayName("Should fail validation when healthCheckInterval is null")
    void shouldFailValidationWhenHealthCheckIntervalIsNull() {
      var loadBalancingProperties = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
          "round-robin",
          true,
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties>> violations = validator.validate(loadBalancingProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("SecurityProperties Tests")
  class SecurityPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid security properties")
    void shouldValidateSuccessfullyWithValidSecurityProperties() {
      var securityProperties = createValidSecurityProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties>> violations = validator.validate(securityProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when jwt is null")
    void shouldFailValidationWhenJwtIsNull() {
      var securityProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties(
          null,
          createValidAuthenticationProperties(),
          List.of("/health", "/actuator/**")
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties>> violations = validator.validate(securityProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when authentication is null")
    void shouldFailValidationWhenAuthenticationIsNull() {
      var securityProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties(
          createValidJwtProperties(),
          null,
          List.of("/health", "/actuator/**")
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties>> violations = validator.validate(securityProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("JwtProperties Tests")
  class JwtPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with HS256 algorithm")
    void shouldValidateSuccessfullyWithHS256Algorithm() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15),
          Duration.ofDays(7),
          "iqscaffold-gateway",
          "iqscaffold-api",
          "HS256",
          "http://auth-service:8080/.well-known/jwks.json"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully with RS256 algorithm")
    void shouldValidateSuccessfullyWithRS256Algorithm() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15),
          Duration.ofDays(7),
          "iqscaffold-gateway",
          "iqscaffold-api",
          "RS256",
          "http://auth-service:8080/.well-known/jwks.json"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid algorithm")
    void shouldFailValidationWithInvalidAlgorithm() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15),
          Duration.ofDays(7),
          "iqscaffold-gateway",
          "iqscaffold-api",
          "ES256",
          "http://auth-service:8080/.well-known/jwks.json"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("algorithm"));
    }

    @Test
    @DisplayName("Should fail validation when issuer is blank")
    void shouldFailValidationWhenIssuerIsBlank() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15),
          Duration.ofDays(7),
          "",
          "iqscaffold-api",
          "RS256",
          "http://auth-service:8080/.well-known/jwks.json"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("issuer"));
    }

    @Test
    @DisplayName("Should fail validation when jwkSetUri is blank")
    void shouldFailValidationWhenJwkSetUriIsBlank() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15),
          Duration.ofDays(7),
          "iqscaffold-gateway",
          "iqscaffold-api",
          "RS256",
          ""
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("jwkSetUri"));
    }

    @Test
    @DisplayName("Should fail validation when accessTokenExpiry is null")
    void shouldFailValidationWhenAccessTokenExpiryIsNull() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          null,
          Duration.ofDays(7),
          "iqscaffold-gateway",
          "iqscaffold-api",
          "RS256",
          "http://auth-service:8080/.well-known/jwks.json"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when refreshTokenExpiry is null")
    void shouldFailValidationWhenRefreshTokenExpiryIsNull() {
      var jwtProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15),
          null,
          "iqscaffold-gateway",
          "iqscaffold-api",
          "RS256",
          "http://auth-service:8080/.well-known/jwks.json"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(jwtProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("AuthenticationProperties Tests")
  class AuthenticationPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid authentication properties")
    void shouldValidateSuccessfullyWithValidAuthenticationProperties() {
      var authProperties = createValidAuthenticationProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties>> violations = validator.validate(authProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when userServiceUrl is blank")
    void shouldFailValidationWhenUserServiceUrlIsBlank() {
      var authProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
          true,
          "",
          Duration.ofSeconds(5),
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties>> violations = validator.validate(authProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("userServiceUrl"));
    }

    @Test
    @DisplayName("Should fail validation when tokenValidationTimeout is null")
    void shouldFailValidationWhenTokenValidationTimeoutIsNull() {
      var authProperties = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
          true,
          "http://user-service:8080",
          null,
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties>> violations = validator.validate(authProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("RateLimitingProperties Tests")
  class RateLimitingPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid rate limiting properties")
    void shouldValidateSuccessfullyWithValidRateLimitingProperties() {
      var rateLimitingProperties = createValidRateLimitingProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties>> violations = validator.validate(rateLimitingProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when redis is null")
    void shouldFailValidationWhenRedisIsNull() {
      var rateLimitingProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties(
          true,
          null,
          createValidPoliciesProperties(),
          createValidTenantQuotasProperties()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties>> violations = validator.validate(rateLimitingProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("RateLimiting RedisProperties Tests")
  class RateLimitingRedisPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid redis properties")
    void shouldValidateSuccessfullyWithValidRedisProperties() {
      var redisProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
          "rate-limit:",
          Duration.ofMinutes(1)
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties>> violations = validator.validate(redisProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when keyPrefix is blank")
    void shouldFailValidationWhenKeyPrefixIsBlank() {
      var redisProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
          "",
          Duration.ofMinutes(1)
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties>> violations = validator.validate(redisProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("keyPrefix"));
    }

    @Test
    @DisplayName("Should fail validation when keyExpiry is null")
    void shouldFailValidationWhenKeyExpiryIsNull() {
      var redisProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
          "rate-limit:",
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties>> violations = validator.validate(redisProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("PoliciesProperties Tests")
  class PoliciesPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid policies properties")
    void shouldValidateSuccessfullyWithValidPoliciesProperties() {
      var policiesProperties = createValidPoliciesProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties>> violations = validator.validate(policiesProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when defaultRequestsPerMinute is below minimum")
    void shouldFailValidationWhenDefaultRequestsPerMinuteIsBelowMinimum() {
      var policiesProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
          0,
          200,
          Map.of()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties>> violations = validator.validate(policiesProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultRequestsPerMinute"));
    }

    @Test
    @DisplayName("Should fail validation when defaultRequestsPerMinute exceeds maximum")
    void shouldFailValidationWhenDefaultRequestsPerMinuteExceedsMaximum() {
      var policiesProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
          10001,
          200,
          Map.of()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties>> violations = validator.validate(policiesProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultRequestsPerMinute"));
    }

    @Test
    @DisplayName("Should fail validation when defaultBurstCapacity is below minimum")
    void shouldFailValidationWhenDefaultBurstCapacityIsBelowMinimum() {
      var policiesProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
          100,
          0,
          Map.of()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties>> violations = validator.validate(policiesProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultBurstCapacity"));
    }

    @Test
    @DisplayName("Should fail validation when defaultBurstCapacity exceeds maximum")
    void shouldFailValidationWhenDefaultBurstCapacityExceedsMaximum() {
      var policiesProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
          100,
          20001,
          Map.of()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties>> violations = validator.validate(policiesProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultBurstCapacity"));
    }
  }

  @Nested
  @DisplayName("EndpointPolicyProperties Tests")
  class EndpointPolicyPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid endpoint policy properties")
    void shouldValidateSuccessfullyWithValidEndpointPolicyProperties() {
      var endpointPolicyProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties(
          50,
          100,
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties>> violations =
          validator.validate(endpointPolicyProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when requestsPerMinute is below minimum")
    void shouldFailValidationWhenRequestsPerMinuteIsBelowMinimum() {
      var endpointPolicyProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties(
          0,
          100,
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties>> violations =
          validator.validate(endpointPolicyProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requestsPerMinute"));
    }

    @Test
    @DisplayName("Should fail validation when requestsPerMinute exceeds maximum")
    void shouldFailValidationWhenRequestsPerMinuteExceedsMaximum() {
      var endpointPolicyProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties(
          1001,
          100,
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties>> violations =
          validator.validate(endpointPolicyProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requestsPerMinute"));
    }

    @Test
    @DisplayName("Should fail validation when burstCapacity is below minimum")
    void shouldFailValidationWhenBurstCapacityIsBelowMinimum() {
      var endpointPolicyProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties(
          50,
          0,
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties>> violations =
          validator.validate(endpointPolicyProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("burstCapacity"));
    }

    @Test
    @DisplayName("Should fail validation when burstCapacity exceeds maximum")
    void shouldFailValidationWhenBurstCapacityExceedsMaximum() {
      var endpointPolicyProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties(
          50,
          2001,
          true
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties>> violations =
          validator.validate(endpointPolicyProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("burstCapacity"));
    }
  }

  @Nested
  @DisplayName("TenantQuotasProperties Tests")
  class TenantQuotasPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid tenant quotas properties")
    void shouldValidateSuccessfullyWithValidTenantQuotasProperties() {
      var tenantQuotasProperties = createValidTenantQuotasProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties>> violations = validator.validate(tenantQuotasProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when defaultTenantRequestsPerMinute is below minimum")
    void shouldFailValidationWhenDefaultTenantRequestsPerMinuteIsBelowMinimum() {
      var tenantQuotasProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
          true,
          0,
          Map.of()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties>> violations = validator.validate(tenantQuotasProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultTenantRequestsPerMinute"));
    }

    @Test
    @DisplayName("Should fail validation when defaultTenantRequestsPerMinute exceeds maximum")
    void shouldFailValidationWhenDefaultTenantRequestsPerMinuteExceedsMaximum() {
      var tenantQuotasProperties = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
          true,
          100001,
          Map.of()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties>> violations = validator.validate(tenantQuotasProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultTenantRequestsPerMinute"));
    }
  }

  @Nested
  @DisplayName("CircuitBreakerProperties Tests")
  class CircuitBreakerPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid circuit breaker properties")
    void shouldValidateSuccessfullyWithValidCircuitBreakerProperties() {
      var circuitBreakerProperties = createValidCircuitBreakerProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully with COUNT_BASED sliding window")
    void shouldValidateSuccessfullyWithCountBasedSlidingWindow() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          50,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          100,
          "COUNT_BASED"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate successfully with TIME_BASED sliding window")
    void shouldValidateSuccessfullyWithTimeBasedSlidingWindow() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          50,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          100,
          "TIME_BASED"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid sliding window type")
    void shouldFailValidationWithInvalidSlidingWindowType() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          50,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          100,
          "INVALID_TYPE"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("slidingWindowType"));
    }

    @Test
    @DisplayName("Should fail validation when failureRateThreshold is below minimum")
    void shouldFailValidationWhenFailureRateThresholdIsBelowMinimum() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          0,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          100,
          "COUNT_BASED"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("failureRateThreshold"));
    }

    @Test
    @DisplayName("Should fail validation when failureRateThreshold exceeds maximum")
    void shouldFailValidationWhenFailureRateThresholdExceedsMaximum() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          101,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          100,
          "COUNT_BASED"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("failureRateThreshold"));
    }

    @Test
    @DisplayName("Should fail validation when slidingWindowSize is below minimum")
    void shouldFailValidationWhenSlidingWindowSizeIsBelowMinimum() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          50,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          9,
          "COUNT_BASED"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("slidingWindowSize"));
    }

    @Test
    @DisplayName("Should fail validation when slidingWindowSize exceeds maximum")
    void shouldFailValidationWhenSlidingWindowSizeExceedsMaximum() {
      var circuitBreakerProperties = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
          true,
          50,
          50,
          Duration.ofSeconds(5),
          10,
          Duration.ofSeconds(60),
          1001,
          "COUNT_BASED"
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties>> violations = validator.validate(circuitBreakerProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("slidingWindowSize"));
    }
  }

  @Nested
  @DisplayName("CorsProperties Tests")
  class CorsPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid CORS properties")
    void shouldValidateSuccessfullyWithValidCorsProperties() {
      var corsProperties = createValidCorsProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CorsProperties>> violations = validator.validate(corsProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when maxAge is negative")
    void shouldFailValidationWhenMaxAgeIsNegative() {
      var corsProperties = new IqScaffoldProperties.GatewayProperties.CorsProperties(
          true,
          List.of("http://localhost:3000"),
          List.of("GET", "POST", "PUT", "DELETE"),
          List.of("*"),
          true,
          -1
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CorsProperties>> violations = validator.validate(corsProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("maxAge"));
    }

    @Test
    @DisplayName("Should fail validation when maxAge exceeds maximum")
    void shouldFailValidationWhenMaxAgeExceedsMaximum() {
      var corsProperties = new IqScaffoldProperties.GatewayProperties.CorsProperties(
          true,
          List.of("http://localhost:3000"),
          List.of("GET", "POST", "PUT", "DELETE"),
          List.of("*"),
          true,
          86401
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CorsProperties>> violations = validator.validate(corsProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("maxAge"));
    }
  }

  @Nested
  @DisplayName("TransformationProperties Tests")
  class TransformationPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid transformation properties")
    void shouldValidateSuccessfullyWithValidTransformationProperties() {
      var transformationProperties = createValidTransformationProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.TransformationProperties>> violations = validator.validate(transformationProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when request is null")
    void shouldFailValidationWhenRequestIsNull() {
      var transformationProperties = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
          null,
          createValidResponseTransformationProperties()
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.TransformationProperties>> violations = validator.validate(transformationProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when response is null")
    void shouldFailValidationWhenResponseIsNull() {
      var transformationProperties = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
          createValidRequestTransformationProperties(),
          null
      );

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.TransformationProperties>> violations = validator.validate(transformationProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("RequestTransformationProperties Tests")
  class RequestTransformationPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid request transformation properties")
    void shouldValidateSuccessfullyWithValidRequestTransformationProperties() {
      var requestTransformationProperties = createValidRequestTransformationProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties>>
          violations = validator.validate(requestTransformationProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should create request transformation properties with all features enabled")
    void shouldCreateRequestTransformationPropertiesWithAllFeaturesEnabled() {
      var requestTransformationProperties = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
          true,
          true,
          true,
          true,
          true, // enableFeatureContextPropagation
          List.of("X-Internal-Header"),
          Map.of("X-Custom-Header", "custom-value")
      );

      assertThat(requestTransformationProperties.enabled()).isTrue();
      assertThat(requestTransformationProperties.enableHeaderEnrichment()).isTrue();
      assertThat(requestTransformationProperties.enableUserContextPropagation()).isTrue();
      assertThat(requestTransformationProperties.enableTenantContextPropagation()).isTrue();
    }
  }

  @Nested
  @DisplayName("ResponseTransformationProperties Tests")
  class ResponseTransformationPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid response transformation properties")
    void shouldValidateSuccessfullyWithValidResponseTransformationProperties() {
      var responseTransformationProperties = createValidResponseTransformationProperties();

      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties>> violations =
          validator.validate(responseTransformationProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should create response transformation properties with all features enabled")
    void shouldCreateResponseTransformationPropertiesWithAllFeaturesEnabled() {
      var responseTransformationProperties = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
          true,
          true,
          true,
          true,
          List.of("X-Internal-Response-Header")
      );

      assertThat(responseTransformationProperties.enabled()).isTrue();
      assertThat(responseTransformationProperties.enableSecurityHeaders()).isTrue();
      assertThat(responseTransformationProperties.enableCorrelationHeaders()).isTrue();
      assertThat(responseTransformationProperties.removeInternalHeaders()).isTrue();
    }
  }

  // Helper methods to create valid test objects

  private IqScaffoldProperties.GatewayProperties createValidGatewayProperties() {
    return new IqScaffoldProperties.GatewayProperties(
        createValidRoutingProperties(),
        createValidSecurityProperties(),
        createValidRateLimitingProperties(),
        createValidCircuitBreakerProperties(),
        createValidCorsProperties(),
        createValidTransformationProperties(),
        createValidFeatureAccessProperties()
    );
  }

  private IqScaffoldProperties.GatewayProperties.RoutingProperties createValidRoutingProperties() {
    return new IqScaffoldProperties.GatewayProperties.RoutingProperties(
        createValidApiPrefixProperties(),
        Map.of("user-service", createValidServiceProperties()),
        true,
        createValidLoadBalancingProperties()
    );
  }

  private IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties createValidApiPrefixProperties() {
    return new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
        true,
        "/api",
        0
    );
  }

  private IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties createValidServiceProperties() {
    return new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
        "http://user-service:8080",
        "/users/**",
        true,
        5000,
        30000,
        null
    );
  }

  private IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties createValidLoadBalancingProperties() {
    return new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
        "round-robin",
        true,
        Duration.ofSeconds(30)
    );
  }

  private IqScaffoldProperties.GatewayProperties.SecurityProperties createValidSecurityProperties() {
    return new IqScaffoldProperties.GatewayProperties.SecurityProperties(
        createValidJwtProperties(),
        createValidAuthenticationProperties(),
        List.of("/health", "/actuator/**")
    );
  }

  private IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties createValidJwtProperties() {
    return new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "iqscaffold-gateway",
        "iqscaffold-api",
        "RS256",
        "http://auth-service:8080/.well-known/jwks.json"
    );
  }

  private IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties createValidAuthenticationProperties() {
    return new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
        true,
        "http://user-service:8080",
        Duration.ofSeconds(5),
        true
    );
  }

  private IqScaffoldProperties.GatewayProperties.RateLimitingProperties createValidRateLimitingProperties() {
    return new IqScaffoldProperties.GatewayProperties.RateLimitingProperties(
        true,
        new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
            "rate-limit:",
            Duration.ofMinutes(1)
        ),
        createValidPoliciesProperties(),
        createValidTenantQuotasProperties()
    );
  }

  private IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties createValidPoliciesProperties() {
    return new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
        100,
        200,
        Map.of()
    );
  }

  private IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties createValidTenantQuotasProperties() {
    return new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
        true,
        1000,
        Map.of()
    );
  }

  private IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties createValidCircuitBreakerProperties() {
    return new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
        true,
        50,
        50,
        Duration.ofSeconds(5),
        10,
        Duration.ofSeconds(60),
        100,
        "COUNT_BASED"
    );
  }

  private IqScaffoldProperties.GatewayProperties.CorsProperties createValidCorsProperties() {
    return new IqScaffoldProperties.GatewayProperties.CorsProperties(
        true,
        List.of("http://localhost:3000"),
        List.of("GET", "POST", "PUT", "DELETE"),
        List.of("*"),
        true,
        3600
    );
  }

  private IqScaffoldProperties.GatewayProperties.TransformationProperties createValidTransformationProperties() {
    return new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        createValidRequestTransformationProperties(),
        createValidResponseTransformationProperties()
    );
  }

  private IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties createValidRequestTransformationProperties() {
    return new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true,
        true,
        true,
        true,
        true, // enableFeatureContextPropagation
        List.of(),
        Map.of()
    );
  }

  private IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties createValidResponseTransformationProperties() {
    return new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true,
        true,
        true,
        true,
        List.of()
    );
  }

  private IqScaffoldProperties.GatewayProperties.FeatureAccessProperties createValidFeatureAccessProperties() {
    return new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties(
        true,
        List.of("/actuator/**", "/api/v1/public/**"),
        List.of(
            new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.FeatureMapping(
                "/api/v1/analytics/**",
                List.of("GET", "POST"),
                Set.of("advanced_analytics"),
                "Analytics endpoints"
            )
        ),
        new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.CacheProperties(
            Duration.ofMinutes(15),
            1000,
            true
        )
    );
  }
}
