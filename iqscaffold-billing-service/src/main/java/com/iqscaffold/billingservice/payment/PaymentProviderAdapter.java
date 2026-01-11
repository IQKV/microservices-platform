package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.util.Optional;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import com.iqscaffold.billingservice.webhook.WebhookEvent;

/**
 * Adapter interface for payment gateway providers.
 * Implementations provide gateway-specific logic for payment processing,
 * customer management, merchant onboarding, and webhook handling.
 */
public interface PaymentProviderAdapter {
  public record ProviderPaymentIntent(String id, String clientSecret) {
  }

  /**
   * Get the payment gateway provider type this adapter supports.
   */
  PaymentGatewayProvider getProviderType();

  /**
   * Create a Payment Intent.
   *
   * @param amount               amount in major units
   * @param currency             currency code
   * @param description          description of payment
   * @param customerEmail        email of the customer (for lookup/creation)
   * @param customerName         name of the customer
   * @param metadata             arbitrary metadata
   * @param applicationFeeAmount platform fee
   * @param connectedAccountId   Stripe Connect Account ID
   * @param idempotencyKey       Key to ensure operation is not repeated
   * @return structured payment intent carrier
   */
  ProviderPaymentIntent createPaymentIntent(
      BigDecimal amount,
      String currency,
      String description,
      String customerEmail,
      String customerName,
      java.util.Map<String, String> metadata,
      BigDecimal applicationFeeAmount,
      Optional<String> connectedAccountId,
      String idempotencyKey);

  /**
   * Refund a payment.
   *
   * @param paymentIntentId    the ID of the payment intent
   * @param amount             optional amount to refund (if empty, full refund)
   * @param currency           currency code (required if amount is present)
   * @param connectedAccountId Stripe Connect Account ID (optional)
   */
  void refundPayment(String paymentIntentId, Optional<BigDecimal> amount, String currency,
                     Optional<String> connectedAccountId);

  /**
   * Create a Stripe Connect Account.
   *
   * @return the new account ID
   */
  String createConnectAccount();

  /**
   * Create an Account Link for onboarding.
   *
   * @param accountId  the Connect Account ID
   * @param refreshUrl URL to redirect if link expires
   * @param returnUrl  URL to redirect after completion
   * @return the URL for the user to visit
   */
  String createAccountLink(String accountId, String refreshUrl, String returnUrl);

  /**
   * Verify webhook signature and parse the webhook payload into a normalized event.
   * <p>
   * This method performs cryptographic signature verification to ensure the webhook
   * is authentic and originated from the payment provider. It then parses the
   * provider-specific event format into a unified {@link WebhookEvent} structure.
   *
   * @param payload   The raw JSON payload from the request body
   * @param sigHeader The signature header value (e.g., Stripe-Signature, PayPal-Transmission-Sig)
   * @return Normalized webhook event
   * @throws IllegalArgumentException If signature verification fails or payload is invalid
   */
  WebhookEvent verifyAndParseWebhook(String payload, String sigHeader);
}
