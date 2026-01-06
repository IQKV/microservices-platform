package com.iqscaffold.billingservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for IQ Scaffold Billing Service. All custom configuration properties use the 'iqscaffold.' prefix for clear namespace separation.
 */
@ConfigurationProperties(prefix = "iqscaffold")
@Validated
public record IqScaffoldProperties(
    @Valid @NotNull Email email,
    @Valid @NotNull Billing billing,
    @NotBlank String tenantIdHeader
) {

  /**
   * Email configuration properties with iqscaffold.email prefix.
   */
  public record Email(
      @Valid @NotNull Smtp smtp,
      @Valid @NotNull Sender sender,
      @Valid @NotNull Templates templates
  ) {

    public record Smtp(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        String username,
        String password,
        boolean auth,
        boolean starttls,
        @NotNull Duration timeout
    ) {

    }

    public record Sender(
        @NotBlank String fromEmail,
        @NotBlank String fromName,
        @NotBlank String baseUrl
    ) {

    }

    public record Templates(
        @NotBlank String merchantOnboardingTemplate,
        @NotBlank String paymentSuccessfulTemplate,
        @NotBlank String paymentFailedTemplate,
        @NotBlank String paymentRefundedTemplate,
        @NotBlank String invoiceGeneratedTemplate
    ) {

    }
  }

  /**
   * Billing-specific configuration properties with iqscaffold.billing prefix.
   */
  public record Billing(
      @Valid @NotNull Security security,
      @Valid @NotNull Payment payment,
      @Valid @NotNull Integration integration,
      @Valid @NotNull Stripe stripe,
      @Valid @NotNull Notifications notifications
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
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "stripe|manual") String provider,
        boolean saasMode,
        @Valid @NotNull Stripe stripe
    ) {
      public record Stripe(
          @NotBlank String apiKey,
          @NotBlank String webhookSecret,
          @NotBlank String clientId // For Connect
      ) {}
    }

    public record Stripe(
        @NotBlank String publicKey,
        @NotBlank String secretKey,
        @NotBlank String webhookSecret,
        @NotBlank String connectClientId
    ) {

    }

    public record Notifications(
        boolean enableEmailNotifications,
        boolean enableWebhookNotifications,
        @NotNull Duration retryDelay,
        @Min(1) @Max(10) int maxRetries
    ) {

    }
  }
}