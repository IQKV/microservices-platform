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

  // ==================== Subscription Management Methods ====================

  /**
   * Create a Product in the payment provider.
   *
   * @param name        Product name
   * @param description Product description
   * @param metadata    Additional metadata
   * @return Product ID
   */
  String createProduct(String name, String description, java.util.Map<String, String> metadata);

  /**
   * Create a Price for a Product.
   *
   * @param productId     Product ID
   * @param amount        Price amount in major units
   * @param currency      Currency code
   * @param interval      Billing interval (month, year, etc.)
   * @param intervalCount Number of intervals between billings
   * @param metadata      Additional metadata
   * @return Price ID
   */
  String createPrice(String productId, BigDecimal amount, String currency, 
                     String interval, Integer intervalCount, java.util.Map<String, String> metadata);

  /**
   * Create a Subscription for a customer.
   *
   * @param customerId        Customer ID
   * @param priceId           Price ID to subscribe to
   * @param trialPeriodDays   Number of trial days (optional)
   * @param metadata          Additional metadata
   * @param idempotencyKey    Key to ensure operation is not repeated
   * @return Subscription ID
   */
  String createSubscription(String customerId, String priceId, Integer trialPeriodDays,
                            java.util.Map<String, String> metadata, String idempotencyKey);

  /**
   * Update a subscription (e.g., change plan, update quantity).
   *
   * @param subscriptionId Subscription ID
   * @param newPriceId     New price ID (if changing plan)
   * @param metadata       Updated metadata
   * @return Updated subscription ID
   */
  String updateSubscription(String subscriptionId, String newPriceId, java.util.Map<String, String> metadata);

  /**
   * Cancel a subscription.
   *
   * @param subscriptionId  Subscription ID
   * @param cancelAtPeriodEnd If true, cancel at end of current period; if false, cancel immediately
   */
  void cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd);

  /**
   * Pause a subscription (pause collection).
   *
   * @param subscriptionId Subscription ID
   */
  void pauseSubscription(String subscriptionId);

  /**
   * Resume a paused subscription.
   *
   * @param subscriptionId Subscription ID
   */
  void resumeSubscription(String subscriptionId);

  /**
   * Retrieve subscription details from the provider.
   *
   * @param subscriptionId Subscription ID
   * @return Subscription details (provider-specific object)
   */
  Object getSubscription(String subscriptionId);
}
