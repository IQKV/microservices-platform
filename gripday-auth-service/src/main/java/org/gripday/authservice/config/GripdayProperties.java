package org.gripday.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for Gripday Auth Service.
 * All custom configuration properties use the 'gripday.' prefix for clear namespace separation.
 */
@ConfigurationProperties(prefix = "gripday")
@Validated
public record GripdayProperties(
    @Valid @NotNull Database database,
    @Valid @NotNull Auth auth,
    @Valid @NotNull Cache cache,
    @Valid @NotNull Email email,
    @Valid @NotNull Observability observability
) {

    /**
     * Database configuration properties with gripday.database prefix.
     */
    public record Database(
        @NotBlank String host,
        @Positive int port,
        @NotBlank String name,
        @NotBlank String username,
        @NotBlank String password,
        @Valid @NotNull Pool pool
    ) {
        public record Pool(
            @Positive int initialSize,
            @Positive int maxActive,
            @Positive int maxIdle,
            @Positive int minIdle,
            @Positive long maxWait
        ) {}
    }

    /**
     * Authentication configuration properties with gripday.auth prefix.
     */
    public record Auth(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Security security
    ) {
        public record Jwt(
            @NotBlank String secret,
            @Positive long accessTokenExpirationMinutes,
            @Positive long refreshTokenExpirationDays,
            @NotBlank String issuer,
            @NotBlank String audience
        ) {}

        public record Security(
            @Positive int passwordEncoderStrength,
            @Positive int maxLoginAttempts,
            @Positive long lockoutDurationMinutes,
            @Valid @NotNull RateLimit rateLimit
        ) {
            public record RateLimit(
                @Positive int requestsPerMinute,
                @Positive long windowSizeMinutes
            ) {}
        }
    }

    /**
     * Cache configuration properties with gripday.cache prefix.
     */
    public record Cache(
        @Valid @NotNull Redis redis
    ) {
        public record Redis(
            @NotBlank String host,
            @Positive int port,
            @NotBlank String password,
            @Positive int database,
            @Valid @NotNull Pool pool
        ) {
            public record Pool(
                @Positive int maxActive,
                @Positive int maxIdle,
                @Positive int minIdle,
                @Positive long maxWait
            ) {}
        }
    }

    /**
     * Email configuration properties with gripday.email prefix.
     */
    public record Email(
        @Valid @NotNull Smtp smtp,
        @Valid @NotNull Verification verification,
        @Valid @NotNull Templates templates
    ) {
        public record Smtp(
            @NotBlank String host,
            @Positive int port,
            String username,
            String password,
            boolean auth,
            boolean starttls
        ) {}

        public record Verification(
            @NotBlank String fromEmail,
            @NotBlank String fromName,
            @NotBlank String baseUrl,
            @Positive int rateLimit
        ) {}

        public record Templates(
            @NotBlank String verificationSubject,
            @NotBlank String verificationTemplate
        ) {}
    }

    /**
     * Observability configuration properties with gripday.observability prefix.
     */
    public record Observability(
        @Valid @NotNull Tracing tracing,
        @Valid @NotNull Metrics metrics,
        @Valid @NotNull Logging logging
    ) {
        public record Tracing(
            boolean enabled,
            double sampleRate,
            @NotBlank String serviceName
        ) {}

        public record Metrics(
            boolean enabled,
            @NotBlank String path
        ) {}

        public record Logging(
            @NotBlank String level,
            @NotBlank String format,
            boolean includeCorrelationId
        ) {}
    }
}