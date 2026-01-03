package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentProviderAdapter {
  public record ProviderPaymentIntent(String id, String clientSecret) {
  }

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
}
