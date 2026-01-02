package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentProviderAdapter {
  /**
   * Create a Payment Intent.
   * @param amount amount in major units (e.g., 10.50)
   * @param currency currency code (e.g., USD)
   * @param applicationFeeAmount platform fee in major units
   * @param connectedAccountId Stripe Connect Account ID (optional)
   * @return the provider's payment intent ID
   */
  String createPaymentIntent(BigDecimal amount, String currency, BigDecimal applicationFeeAmount, Optional<String> connectedAccountId);

  /**
   * Refund a payment.
   * @param paymentIntentId the provider's payment intent ID
   * @param amount amount to refund (optional, full refund if empty)
   * @param connectedAccountId Stripe Connect Account ID (optional)
   */
  void refundPayment(String paymentIntentId, Optional<BigDecimal> amount, Optional<String> connectedAccountId);

  /**
   * Create a Stripe Connect Account.
   * @return the new account ID
   */
  String createConnectAccount();

  /**
   * Create an Account Link for onboarding.
   * @param accountId the Connect Account ID
   * @param refreshUrl URL to redirect if link expires
   * @param returnUrl URL to redirect after completion
   * @return the URL for the user to visit
   */
  String createAccountLink(String accountId, String refreshUrl, String returnUrl);
}
