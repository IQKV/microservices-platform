package com.iqscaffold.gatewayservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for IQ Scaffold Gateway Service. All custom configuration properties use the 'iqscaffold.' prefix for clear namespace separation.
 */
@ConfigurationProperties(prefix = "iqscaffold")
@Validated
public record IqScaffoldProperties(
    @Valid @NotNull CacheProperties cache,
    @Valid @NotNull GatewayProperties gateway,
    @Valid @NotNull ObservabilityProperties observability
) {

  /**
   * Cache configuration properties with iqscaffold.cache prefix.
   */
  public record CacheProperties(
      @Valid @NotNull RedisProperties redis
  ) {

    public record RedisProperties(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        String password,
        @Min(0) @Max(15) int database,
        @NotNull Duration timeout,
        @Valid @NotNull PoolProperties pool,
        @NotBlank String keyPrefix,
        @NotNull Duration defaultTtl,
        boolean enableStatistics
    ) {

      public record PoolProperties(
          @Min(1) @Max(100) int maxActive,
          @Min(0) @Max(50) int maxIdle,
          @Min(0) @Max(25) int minIdle,
          @NotNull Duration maxWait
      ) {

      }
    }
  }

  /**
   * Gateway configuration properties with iqscaffold.gateway prefix.
   */
  public record GatewayProperties(
      @Valid @NotNull RoutingProperties routing,
      @Valid @NotNull SecurityProperties security,
      @Valid @NotNull RateLimitingProperties rateLimiting,
      @Valid @NotNull CircuitBreakerProperties circuitBreaker,
      @Valid @NotNull CorsProperties cors,
      @Valid @NotNull TransformationProperties transformation
  ) {

    public record RoutingProperties(
        @Valid @NotNull ApiPrefixProperties apiPrefix,
        Map<String, ServiceProperties> services,
        boolean enableServiceDiscovery,
        @Valid @NotNull LoadBalancingProperties loadBalancing
    ) {

      /**
       * API prefix configuration for environment-specific routing.
       * Allows configuring /api prefix in development/staging and clean URLs in production.
       * <p>
       * Example configurations:
       * - Development/Staging: enabled=true, prefix="/api", stripCount=0 (keep /api in URLs)
       * - Production: enabled=true, prefix="", stripCount=0 (no prefix, deployed on api.iqscaffold.com)
       * - Strip mode: enabled=true, prefix="/api", stripCount=1 (accept /api/v1/admin/users, forward as /v1/admin/users)
       */
      public record ApiPrefixProperties(
          boolean enabled,
          @NotBlank String prefix,
          @Min(0) @Max(5) int stripCount
      ) {

      }

      public record ServiceProperties(
          @NotBlank String uri,
          @NotBlank String path,
          boolean enabled,
          @Positive int connectTimeout,
          @Positive int responseTimeout,
          OpenApiProperties openapi
      ) {

        public record OpenApiProperties(
            boolean enabled,
            String displayName,
            String description,
            String contextPath
        ) {

          public OpenApiProperties {
            // Default values if not provided
            if (displayName == null || displayName.isBlank()) {
              displayName = "Service API";
            }
            if (description == null || description.isBlank()) {
              description = "API documentation";
            }
            if (contextPath == null || contextPath.isBlank()) {
              contextPath = "";
            }
          }
        }
      }

      public record LoadBalancingProperties(
          @Pattern(regexp = "round-robin|weighted|least-connections") String strategy,
          boolean enableHealthCheck,
          @NotNull Duration healthCheckInterval
      ) {

      }
    }

    public record SecurityProperties(
        @Valid @NotNull JwtProperties jwt,
        @Valid @NotNull AuthenticationProperties authentication,
        List<String> publicPaths
    ) {

      public record JwtProperties(
          @NotNull Duration accessTokenExpiry,
          @NotNull Duration refreshTokenExpiry,
          @NotBlank String issuer,
          String audience, // Optional
          @Pattern(regexp = "HS256|RS256") String algorithm,
          @NotBlank String jwkSetUri // Required for RSA validation
      ) {

      }

      public record AuthenticationProperties(
          boolean enabled,
          @NotBlank String userServiceUrl,
          @NotNull Duration tokenValidationTimeout,
          boolean enableUserContextPropagation
      ) {

      }
    }

    public record RateLimitingProperties(
        boolean enabled,
        @Valid @NotNull RedisProperties redis,
        @Valid @NotNull PoliciesProperties policies,
        @Valid @NotNull TenantQuotasProperties tenantQuotas
    ) {

      public record RedisProperties(
          @NotBlank String keyPrefix,
          @NotNull Duration keyExpiry
      ) {

      }

      public record PoliciesProperties(
          @Min(1) @Max(10000) int defaultRequestsPerMinute,
          @Min(1) @Max(20000) int defaultBurstCapacity,
          Map<String, EndpointPolicyProperties> endpoints
      ) {

        public record EndpointPolicyProperties(
            @Min(1) @Max(1000) int requestsPerMinute,
            @Min(1) @Max(2000) int burstCapacity,
            boolean enableTenantQuotas
        ) {

        }
      }

      public record TenantQuotasProperties(
          boolean enabled,
          @Min(1) @Max(100000) int defaultTenantRequestsPerMinute,
          Map<String, Integer> tenantSpecificQuotas
      ) {

      }
    }

    public record CircuitBreakerProperties(
        boolean enabled,
        @Min(1) @Max(100) int failureRateThreshold,
        @Min(1) @Max(100) int slowCallRateThreshold,
        @NotNull Duration slowCallDurationThreshold,
        @Min(1) @Max(100) int minimumNumberOfCalls,
        @NotNull Duration waitDurationInOpenState,
        @Min(10) @Max(1000) int slidingWindowSize,
        @Pattern(regexp = "COUNT_BASED|TIME_BASED") String slidingWindowType
    ) {

    }

    public record CorsProperties(
        boolean enabled,
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials,
        @Min(0) @Max(86400) int maxAge
    ) {

    }

    public record TransformationProperties(
        @Valid @NotNull RequestTransformationProperties request,
        @Valid @NotNull ResponseTransformationProperties response
    ) {

      public record RequestTransformationProperties(
          boolean enabled,
          boolean enableHeaderEnrichment,
          boolean enableUserContextPropagation,
          boolean enableTenantContextPropagation,
          List<String> headersToRemove,
          Map<String, String> additionalHeaders
      ) {

      }

      public record ResponseTransformationProperties(
          boolean enabled,
          boolean enableSecurityHeaders,
          boolean enableCorrelationHeaders,
          boolean removeInternalHeaders,
          List<String> additionalHeadersToRemove
      ) {

      }
    }
  }

  /**
   * Observability configuration properties with iqscaffold.observability prefix.
   */
  public record ObservabilityProperties(
      @Valid @NotNull TracingProperties tracing,
      @Valid @NotNull MetricsProperties metrics,
      @Valid @NotNull LoggingProperties logging
  ) {

    public record TracingProperties(
        boolean enabled,
        @NotBlank String serviceName,
        @DecimalMin("0.0") @DecimalMax("1.0") double samplingRate,
        @NotBlank String endpoint,
        @NotNull Duration timeout,
        @NotNull Duration exportTimeout,
        @Positive int batchSize
    ) {

    }

    public record MetricsProperties(
        boolean enabled,
        @NotBlank String path,
        @NotBlank String prefix,
        boolean includeHostTag,
        boolean includeApplicationTag,
        boolean includeEnvironmentTag,
        Map<String, String> customTags,
        List<String> enabledMetrics
    ) {

    }

    public record LoggingProperties(
        @Pattern(regexp = "DEBUG|INFO|WARN|ERROR") String level,
        @Pattern(regexp = "console|json") String format,
        boolean enableRequestLogging,
        boolean enableResponseLogging,
        boolean enableCorrelationId,
        boolean includeTraceId,
        boolean includeSpanId,
        boolean includeUserId,
        boolean includeTenantId,
        @NotBlank String correlationIdHeader,
        @NotBlank String requestIdHeader,
        @NotBlank String tenantIdHeader
    ) {

    }
  }
}
