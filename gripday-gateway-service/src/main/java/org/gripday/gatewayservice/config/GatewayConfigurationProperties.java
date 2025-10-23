package org.gripday.gatewayservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gateway configuration properties with gripday.gateway prefix.
 * Handles routing, security, rate limiting, circuit breaker, CORS, and transformation configuration.
 */
@ConfigurationProperties(prefix = "gripday.gateway")
@Validated
public record GatewayConfigurationProperties(
    @Valid @NotNull RoutingProperties routing,
    @Valid @NotNull SecurityProperties security,
    @Valid @NotNull RateLimitingProperties rateLimiting,
    @Valid @NotNull CircuitBreakerProperties circuitBreaker,
    @Valid @NotNull CorsProperties cors,
    @Valid @NotNull TransformationProperties transformation
) {

    /**
     * Service routing configuration.
     */
    public record RoutingProperties(
        Map<String, ServiceProperties> services,
        boolean enableServiceDiscovery,
        @Valid @NotNull LoadBalancingProperties loadBalancing
    ) {
        
        /**
         * Individual service routing configuration.
         */
        public record ServiceProperties(
            @NotBlank(message = "Service URI must not be blank")
            String uri,
            
            @NotBlank(message = "Service path must not be blank")
            String path,
            
            boolean enabled,
            
            @Positive(message = "Connect timeout must be positive")
            int connectTimeout,
            
            @Positive(message = "Response timeout must be positive")
            int responseTimeout
        ) {}
        
        /**
         * Load balancing configuration.
         */
        public record LoadBalancingProperties(
            @Pattern(regexp = "round-robin|weighted|least-connections", 
                    message = "Load balancing strategy must be one of: round-robin, weighted, least-connections")
            String strategy,
            
            boolean enableHealthCheck,
            
            @NotNull(message = "Health check interval must not be null")
            Duration healthCheckInterval
        ) {}
    }

    /**
     * Gateway security configuration.
     */
    public record SecurityProperties(
        @Valid @NotNull JwtProperties jwt,
        @Valid @NotNull AuthenticationProperties authentication,
        List<String> publicPaths
    ) {
        
        /**
         * JWT configuration for gateway.
         */
        public record JwtProperties(
            @NotBlank(message = "JWT secret key must not be blank")
            String secretKey,
            
            @NotNull(message = "Access token expiry must not be null")
            Duration accessTokenExpiry,
            
            @NotNull(message = "Refresh token expiry must not be null")
            Duration refreshTokenExpiry,
            
            @NotBlank(message = "JWT issuer must not be blank")
            String issuer,
            
            @NotBlank(message = "JWT audience must not be blank")
            String audience,
            
            @Pattern(regexp = "HS256|RS256", message = "JWT algorithm must be either HS256 or RS256")
            String algorithm
        ) {}
        
        /**
         * Authentication service integration configuration.
         */
        public record AuthenticationProperties(
            boolean enabled,
            
            @NotBlank(message = "Auth service URL must not be blank")
            String authServiceUrl,
            
            @NotNull(message = "Token validation timeout must not be null")
            Duration tokenValidationTimeout,
            
            boolean enableUserContextPropagation
        ) {}
    }

    /**
     * Rate limiting configuration.
     */
    public record RateLimitingProperties(
        boolean enabled,
        @Valid @NotNull RedisProperties redis,
        @Valid @NotNull PoliciesProperties policies,
        @Valid @NotNull TenantQuotasProperties tenantQuotas
    ) {
        
        /**
         * Redis configuration for rate limiting.
         */
        public record RedisProperties(
            @NotBlank(message = "Redis key prefix must not be blank")
            String keyPrefix,
            
            @NotNull(message = "Key expiry must not be null")
            Duration keyExpiry
        ) {}
        
        /**
         * Rate limiting policies configuration.
         */
        public record PoliciesProperties(
            @Min(value = 1, message = "Default requests per minute must be at least 1")
            @Max(value = 10000, message = "Default requests per minute must not exceed 10000")
            int defaultRequestsPerMinute,
            
            @Min(value = 1, message = "Default burst capacity must be at least 1")
            @Max(value = 20000, message = "Default burst capacity must not exceed 20000")
            int defaultBurstCapacity,
            
            Map<String, EndpointPolicyProperties> endpoints
        ) {
            
            /**
             * Endpoint-specific rate limiting policy.
             */
            public record EndpointPolicyProperties(
                @Min(value = 1, message = "Requests per minute must be at least 1")
                @Max(value = 1000, message = "Requests per minute must not exceed 1000")
                int requestsPerMinute,
                
                @Min(value = 1, message = "Burst capacity must be at least 1")
                @Max(value = 2000, message = "Burst capacity must not exceed 2000")
                int burstCapacity,
                
                boolean enableTenantQuotas
            ) {}
        }
        
        /**
         * Tenant-specific quota configuration.
         */
        public record TenantQuotasProperties(
            boolean enabled,
            
            @Min(value = 1, message = "Default tenant requests per minute must be at least 1")
            @Max(value = 100000, message = "Default tenant requests per minute must not exceed 100000")
            int defaultTenantRequestsPerMinute,
            
            Map<String, Integer> tenantSpecificQuotas
        ) {}
    }

    /**
     * Circuit breaker configuration.
     */
    public record CircuitBreakerProperties(
        boolean enabled,
        
        @Min(value = 1, message = "Failure rate threshold must be at least 1")
        @Max(value = 100, message = "Failure rate threshold must not exceed 100")
        int failureRateThreshold,
        
        @Min(value = 1, message = "Slow call rate threshold must be at least 1")
        @Max(value = 100, message = "Slow call rate threshold must not exceed 100")
        int slowCallRateThreshold,
        
        @NotNull(message = "Slow call duration threshold must not be null")
        Duration slowCallDurationThreshold,
        
        @Min(value = 1, message = "Minimum number of calls must be at least 1")
        @Max(value = 100, message = "Minimum number of calls must not exceed 100")
        int minimumNumberOfCalls,
        
        @NotNull(message = "Wait duration in open state must not be null")
        Duration waitDurationInOpenState,
        
        @Min(value = 10, message = "Sliding window size must be at least 10")
        @Max(value = 1000, message = "Sliding window size must not exceed 1000")
        int slidingWindowSize,
        
        @Pattern(regexp = "COUNT_BASED|TIME_BASED", 
                message = "Sliding window type must be either COUNT_BASED or TIME_BASED")
        String slidingWindowType
    ) {}

    /**
     * CORS configuration.
     */
    public record CorsProperties(
        boolean enabled,
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials,
        
        @Min(value = 0, message = "Max age cannot be negative")
        @Max(value = 86400, message = "Max age must not exceed 86400 seconds (24 hours)")
        int maxAge
    ) {}

    /**
     * Request/response transformation configuration.
     */
    public record TransformationProperties(
        @Valid @NotNull RequestTransformationProperties request,
        @Valid @NotNull ResponseTransformationProperties response
    ) {
        
        /**
         * Request transformation configuration.
         */
        public record RequestTransformationProperties(
            boolean enabled,
            boolean enableHeaderEnrichment,
            boolean enableUserContextPropagation,
            boolean enableTenantContextPropagation,
            List<String> headersToRemove,
            Map<String, String> additionalHeaders
        ) {}
        
        /**
         * Response transformation configuration.
         */
        public record ResponseTransformationProperties(
            boolean enabled,
            boolean enableSecurityHeaders,
            boolean enableCorrelationHeaders,
            boolean removeInternalHeaders,
            List<String> additionalHeadersToRemove
        ) {}
    }
}