package com.iqscaffold.billingservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for the Billing Service.
 */
@ConfigurationProperties(prefix = "iqscaffold.billing")
@Validated
public record BillingProperties(
    @Valid @NotNull Security security,
    @Valid @NotNull Payment payment,
    @Valid @NotNull Integration integration
) {

  public record Security(
      @Valid @NotNull Jwt jwt
  ) {
    public record Jwt(
        @NotBlank String jwkSetUri,
        @NotBlank String issuer
    ) {}
  }

  public record Integration(
      @Valid @NotNull EmailService emailService
  ) {
      public record EmailService(
          @NotBlank String url,
          @NotNull Long timeoutMs
      ) {}
  }

  public record Payment(
      @NotBlank @Pattern(regexp = "stripe|manual") String provider,
      @Valid @NotNull Stripe stripe
  ) {
    public record Stripe(
        @NotBlank String apiKey,
        @NotBlank String webhookSecret,
        @NotBlank String clientId // For Connect
    ) {}
  }
}
