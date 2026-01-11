package com.iqscaffold.billingservice.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;

/**
 * Data Transfer Objects for payment gateway configuration.
 */
public class GatewayConfigDtos {

  /**
   * Base DTO for gateway-specific configuration data.
   * Implementations contain provider-specific fields (API keys, secrets, etc.).
   */
  public sealed interface GatewayConfigData permits
      StripeGatewayConfigData,
      PayPalGatewayConfigData,
      SquareGatewayConfigData,
      BraintreeGatewayConfigData {
    PaymentGatewayProvider getProvider();
  }

  /**
   * Stripe gateway configuration data.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record StripeGatewayConfigData(
      @NotBlank String apiKey,
      @NotBlank String webhookSecret,
      String clientId,
      String publicKey
  ) implements GatewayConfigData {
    @Override
    public PaymentGatewayProvider getProvider() {
      return PaymentGatewayProvider.STRIPE;
    }
  }

  /**
   * PayPal gateway configuration data.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record PayPalGatewayConfigData(
      @NotBlank String clientId,
      @NotBlank String clientSecret,
      String webhookId,
      @NotBlank @Pattern(regexp = "sandbox|live") String mode
  ) implements GatewayConfigData {
    @Override
    public PaymentGatewayProvider getProvider() {
      return PaymentGatewayProvider.PAYPAL;
    }
  }

  /**
   * Square gateway configuration data.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SquareGatewayConfigData(
      @NotBlank String accessToken,
      @NotBlank String locationId,
      String webhookSignatureKey,
      String applicationId
  ) implements GatewayConfigData {
    @Override
    public PaymentGatewayProvider getProvider() {
      return PaymentGatewayProvider.SQUARE;
    }
  }

  /**
   * Braintree gateway configuration data.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BraintreeGatewayConfigData(
      @NotBlank String merchantId,
      @NotBlank String publicKey,
      @NotBlank String privateKey,
      @NotBlank @Pattern(regexp = "sandbox|production") String environment
  ) implements GatewayConfigData {
    @Override
    public PaymentGatewayProvider getProvider() {
      return PaymentGatewayProvider.BRAINTREE;
    }
  }

  /**
   * Request to create or update gateway configuration.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateGatewayConfigRequest(
      @NotNull PaymentGatewayProvider gatewayProvider,
      @NotNull GatewayConfigData configData,
      @NotBlank @Pattern(regexp = "test|live") String mode,
      boolean isActive,
      boolean isPrimary,
      String displayName,
      String description
  ) {
  }

  /**
   * Request to update existing gateway configuration.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateGatewayConfigRequest(
      GatewayConfigData configData,
      @Pattern(regexp = "test|live") String mode,
      Boolean isActive,
      Boolean isPrimary,
      String displayName,
      String description
  ) {
  }

  /**
   * Response containing gateway configuration details.
   * Note: Sensitive data (like API keys) is masked in responses.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record GatewayConfigResponse(
      UUID id,
      String tenantId,
      PaymentGatewayProvider gatewayProvider,
      boolean isActive,
      boolean isPrimary,
      String mode,
      String displayName,
      String description,
      MaskedConfigData maskedConfigData,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  /**
   * Masked configuration data for secure responses.
   * Contains only non-sensitive metadata about the configuration.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record MaskedConfigData(
      PaymentGatewayProvider provider,
      boolean isConfigured,
      String lastFourChars
  ) {
  }

  /**
   * Summary response for listing gateway configurations.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record GatewayConfigSummary(
      UUID id,
      PaymentGatewayProvider gatewayProvider,
      boolean isActive,
      boolean isPrimary,
      String mode,
      String displayName,
      Instant updatedAt
  ) {
  }

  /**
   * Response when activating/deactivating a gateway.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record GatewayStatusResponse(
      UUID id,
      PaymentGatewayProvider gatewayProvider,
      boolean isActive,
      boolean isPrimary,
      String message
  ) {
  }
}
