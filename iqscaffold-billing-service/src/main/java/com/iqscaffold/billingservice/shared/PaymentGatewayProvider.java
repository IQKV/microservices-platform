package com.iqscaffold.billingservice.shared;

/**
 * Enum representing supported payment gateway providers.
 * This abstraction allows the system to support multiple payment processors
 * without coupling the domain model to a specific provider.
 */
public enum PaymentGatewayProvider {
  /**
   * Stripe payment gateway.
   * Supports payment processing, Connect accounts, and webhooks.
   */
  STRIPE,

  /**
   * PayPal payment gateway.
   * Future implementation for PayPal integration.
   */
  PAYPAL,

  /**
   * Square payment gateway.
   * Future implementation for Square integration.
   */
  SQUARE,

  /**
   * Braintree payment gateway.
   * Future implementation for Braintree integration.
   */
  BRAINTREE
}
