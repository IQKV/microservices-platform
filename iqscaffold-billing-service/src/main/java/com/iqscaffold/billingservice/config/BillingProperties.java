package com.iqscaffold.billingservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Type-safe configuration properties for the Billing Service.
 * 
 * <p>Uses Java records for immutability and automatic generation of constructors,
 * getters, equals, hashCode, and toString methods.
 */
@ConfigurationProperties(prefix = "iqscaffold.billing")
@Validated
public record BillingProperties(
    @Valid @NotNull Security security,
    @Valid @NotNull Integration integration,
    @Valid @NotNull Payment payment,
    @Valid @NotNull Subscription subscription,
    @Valid @NotNull Usage usage,
    @Valid @NotNull Invoice invoice,
    @Valid @NotNull Portal portal,
    @Valid @NotNull Features features
) {

    /**
     * Security configuration for JWT and rate limiting.
     */
    public record Security(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull RateLimiting rateLimiting
    ) {
        public record Jwt(
            @NotBlank String jwkSetUri,
            @NotBlank String issuer
        ) {}

        public record RateLimiting(
            boolean enabled,
            @Positive int requestsPerMinute,
            @Positive int burstCapacity
        ) {}
    }

    /**
     * Integration configuration for external services.
     */
    public record Integration(
        @Valid @NotNull UserService userService,
        @Valid @NotNull EmailService emailService
    ) {
        public record UserService(
            @NotBlank String url,
            @NotNull Duration timeout
        ) {}

        public record EmailService(
            @NotBlank String url,
            @NotNull Duration timeout
        ) {}
    }

    /**
     * Payment provider configuration.
     */
    public record Payment(
        @NotBlank @Pattern(regexp = "stripe|paypal|manual") String provider,
        @Valid @NotNull Stripe stripe,
        @Valid @NotNull PayPal paypal
    ) {
        public record Stripe(
            String apiKey,
            String webhookSecret
        ) {}

        public record PayPal(
            String clientId,
            String clientSecret
        ) {}
    }

    /**
     * Subscription configuration.
     */
    public record Subscription(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
        @Min(0) int trialDays,
        @Min(0) int gracePeriodDays,
        boolean allowMultipleSubscriptions
    ) {}

    /**
     * Usage metering configuration.
     */
    public record Usage(
        boolean meteringEnabled,
        @Positive int batchSize,
        @NotNull Duration flushInterval,
        @Positive int retentionDays
    ) {}

    /**
     * Invoice configuration.
     */
    public record Invoice(
        @NotBlank String numberFormat,
        @Positive int dueDays,
        boolean autoFinalize,
        boolean pdfGenerationEnabled
    ) {}

    /**
     * Customer portal configuration.
     */
    public record Portal(
        boolean enabled,
        boolean allowPlanChanges,
        boolean allowCancellation
    ) {}

    /**
     * Feature flags configuration.
     */
    public record Features(
        boolean proration,
        boolean dunning,
        boolean analytics,
        boolean webhooks
    ) {}
}
