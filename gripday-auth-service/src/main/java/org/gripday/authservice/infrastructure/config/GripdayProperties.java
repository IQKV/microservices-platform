package org.gripday.authservice.infrastructure.config;

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
import java.util.Map;

/**
 * Configuration properties for Gripday Auth Service.
 * All custom configuration properties use the 'gripday.' prefix for clear namespace separation.
 */
@ConfigurationProperties(prefix = "gripday")
@Validated
public record GripdayProperties(
    @Valid @NotNull DatabaseProperties database,
    @Valid @NotNull CacheProperties cache,
    @Valid @NotNull AuthProperties auth,
    @Valid @NotNull ObservabilityProperties observability
) {

    /**
     * Database configuration properties with gripday.database prefix.
     */
    public record DatabaseProperties(
        @NotBlank String url,
        @NotBlank String username,
        @NotBlank String password,
        @Valid @NotNull PoolProperties pool,
        @Valid @NotNull MigrationProperties migration
    ) {
        
        public record PoolProperties(
            @Min(1) @Max(100) int maximumSize,
            @Min(0) @Max(50) int minimumIdle,
            @NotNull Duration connectionTimeout,
            @NotNull Duration idleTimeout,
            @NotNull Duration maxLifetime
        ) {}
        
        public record MigrationProperties(
            boolean enabled,
            @NotBlank String contexts,
            boolean validateOnMigrate
        ) {}
    }

    /**
     * Cache configuration properties with gripday.cache prefix.
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
            ) {}
        }
    }

    /**
     * Authentication configuration properties with gripday.auth prefix.
     */
    public record AuthProperties(
        @Valid @NotNull JwtProperties jwt,
        @Valid @NotNull SecurityProperties security,
        @Valid @NotNull OAuth2Properties oauth2
    ) {
        
        public record JwtProperties(
            @NotBlank String secretKey,
            @NotNull Duration accessTokenExpiry,
            @NotNull Duration refreshTokenExpiry,
            @NotBlank String issuer,
            @NotBlank String audience,
            @Pattern(regexp = "HS256|RS256") String algorithm
        ) {}
        
        public record SecurityProperties(
            @Valid @NotNull PasswordProperties password,
            @Valid @NotNull RateLimitingProperties rateLimiting,
            @Valid @NotNull SessionProperties session
        ) {
            
            public record PasswordProperties(
                @Min(4) @Max(20) int encoderStrength,
                boolean requireSpecialChars,
                @Min(6) @Max(128) int minLength
            ) {}
            
            public record RateLimitingProperties(
                @Min(1) @Max(100) int loginAttempts,
                @NotNull Duration lockoutDuration
            ) {}
            
            public record SessionProperties(
                @NotNull Duration timeout,
                @Min(1) @Max(10) int concurrentSessions
            ) {}
        }
        
        public record OAuth2Properties(
            boolean enabled,
            Map<String, OAuth2ProviderProperties> providers
        ) {
            
            public record OAuth2ProviderProperties(
                String clientId,
                String clientSecret,
                boolean enabled
            ) {}
        }
    }

    /**
     * Observability configuration properties with gripday.observability prefix.
     */
    public record ObservabilityProperties(
        @Valid @NotNull TracingProperties tracing,
        @Valid @NotNull MetricsProperties metrics,
        @Valid @NotNull LoggingProperties logging
    ) {
        
        public record TracingProperties(
            boolean enabled,
            @NotBlank String serviceName,
            @Min(0.0) @Max(1.0) double samplingRate,
            @NotBlank String endpoint,
            @NotNull Duration timeout,
            @NotNull Duration exportTimeout,
            @Positive int batchSize
        ) {}
        
        public record MetricsProperties(
            boolean enabled,
            @NotBlank String path,
            @NotBlank String prefix,
            boolean includeHostTag,
            boolean includeApplicationTag,
            boolean includeEnvironmentTag,
            Map<String, String> customTags
        ) {}
        
        public record LoggingProperties(
            @Pattern(regexp = "DEBUG|INFO|WARN|ERROR") String level,
            @Pattern(regexp = "console|json") String format,
            boolean includeCorrelationId,
            boolean includeTraceId,
            boolean includeSpanId,
            boolean includeUserId,
            boolean includeTenantId,
            @NotBlank String correlationIdHeader,
            @NotBlank String requestIdHeader,
            @NotBlank String tenantIdHeader,
            boolean enableSqlLogging,
            boolean enableSecurityEvents,
            boolean enablePerformanceLogging
        ) {}
    }
}