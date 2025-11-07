package org.gripday.gatewayservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Gateway Service with gripday prefix. Provides centralized configuration for routing, security, rate limiting, and observability.
 */
@ConfigurationProperties(prefix = "gripday.gateway")
@Validated
public record GatewayProperties(
    @Valid @NotNull Routing routing,
    @Valid @NotNull Security security,
    @Valid @NotNull RateLimiting rateLimiting,
    @Valid @NotNull CircuitBreaker circuitBreaker,
    @Valid @NotNull Cors cors,
    @Valid @NotNull Observability observability
) {

  /**
   * Routing configuration for service discovery and load balancing.
   */
  public record Routing(
      @Valid @NotNull Services services,
      @NotNull Boolean enableServiceDiscovery,
      @NotNull LoadBalancing loadBalancing
  ) {

    public record Services(
        @Valid @NotNull AuthService authService
    ) {

    }

    public record AuthService(
        @NotBlank String uri,
        @NotBlank String path,
        @NotNull Boolean enabled,
        @Positive int connectTimeout,
        @Positive int responseTimeout
    ) {

    }

    public record LoadBalancing(
        @NotBlank String strategy,
        @NotNull Boolean enableHealthCheck,
        @NotNull Duration healthCheckInterval
    ) {

    }
  }

  /**
   * Security configuration for JWT authentication and authorization.
   */
  public record Security(
      @Valid @NotNull Jwt jwt,
      @Valid @NotNull Authentication authentication,
      @NotNull List<String> publicPaths
  ) {

    public record Jwt(
        @NotBlank String secretKey,
        @NotNull Duration accessTokenExpiry,
        @NotNull Duration refreshTokenExpiry,
        @NotBlank String issuer,
        @NotBlank String audience
    ) {

    }

    public record Authentication(
        @NotNull Boolean enabled,
        @NotBlank String authServiceUrl,
        @NotNull Duration tokenValidationTimeout,
        @NotNull Boolean enableUserContextPropagation
    ) {

    }
  }

  /**
   * Rate limiting configuration with tenant-aware policies.
   */
  public record RateLimiting(
      @NotNull Boolean enabled,
      @Valid @NotNull Redis redis,
      @Valid @NotNull Policies policies,
      @Valid @NotNull TenantQuotas tenantQuotas
  ) {

    public record Redis(
        @NotBlank String keyPrefix,
        @NotNull Duration keyExpiry
    ) {

    }

    public record Policies(
        @Positive int defaultRequestsPerMinute,
        @Positive int defaultBurstCapacity,
        @NotNull Map<String, EndpointPolicy> endpoints
    ) {

    }

    public record EndpointPolicy(
        @Positive int requestsPerMinute,
        @Positive int burstCapacity,
        @NotNull Boolean enableTenantQuotas
    ) {

    }

    public record TenantQuotas(
        @NotNull Boolean enabled,
        @Positive int defaultTenantRequestsPerMinute,
        @NotNull Map<String, Integer> tenantSpecificQuotas
    ) {

    }
  }

  /**
   * Circuit breaker configuration for fault tolerance.
   */
  public record CircuitBreaker(
      @NotNull Boolean enabled,
      @Positive int failureRateThreshold,
      @Positive int slowCallRateThreshold,
      @NotNull Duration slowCallDurationThreshold,
      @Positive int minimumNumberOfCalls,
      @NotNull Duration waitDurationInOpenState,
      @Positive int slidingWindowSize,
      @NotBlank String slidingWindowType
  ) {

  }

  /**
   * CORS configuration for cross-origin requests.
   */
  public record Cors(
      @NotNull Boolean enabled,
      @NotNull List<String> allowedOrigins,
      @NotNull List<String> allowedMethods,
      @NotNull List<String> allowedHeaders,
      @NotNull Boolean allowCredentials,
      @Positive long maxAge
  ) {

  }

  /**
   * Observability configuration for monitoring and tracing.
   */
  public record Observability(
      @Valid @NotNull Tracing tracing,
      @Valid @NotNull Metrics metrics,
      @Valid @NotNull Logging logging
  ) {

    public record Tracing(
        @NotNull Boolean enabled,
        @NotNull Double samplingRate,
        @NotBlank String serviceName
    ) {

    }

    public record Metrics(
        @NotNull Boolean enabled,
        @NotBlank String prefix,
        @NotNull List<String> enabledMetrics
    ) {

    }

    public record Logging(
        @NotBlank String level,
        @NotNull Boolean enableRequestLogging,
        @NotNull Boolean enableResponseLogging,
        @NotNull Boolean enableCorrelationId
    ) {

    }
  }
}