package com.iqscaffold.billingservice.webhook;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;

/**
 * Provider-agnostic webhook event abstraction.
 * <p>
 * This record wraps gateway-specific webhook events into a unified structure
 * that can be processed consistently across all payment providers.
 * <p>
 * Each provider adapter is responsible for parsing their native event format
 * (e.g., Stripe Event, PayPal Webhook, Square Event) into this common structure.
 *
 * @param eventId           Unique identifier for the event
 * @param eventType         Normalized event type (e.g., "payment.succeeded", "payment.failed")
 * @param provider          Which payment gateway sent this event
 * @param tenantId          Optional tenant ID resolved from event metadata
 * @param resourceId        The ID of the primary resource (e.g., payment intent ID, payout ID)
 * @param resourceType      Type of resource (e.g., "payment_intent", "payout", "account")
 * @param metadata          Additional event-specific metadata
 * @param rawPayload        Original event object for provider-specific handling
 */
public record WebhookEvent(
    String eventId,
    String eventType,
    PaymentGatewayProvider provider,
    Optional<String> tenantId,
    String resourceId,
    String resourceType,
    Map<String, Object> metadata,
    Object rawPayload
) {

  /**
   * Normalized event types across all payment providers.
   */
  public static final class EventType {
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String PAYMENT_PARTIALLY_REFUNDED = "payment.partially_refunded";
    public static final String PAYOUT_PAID = "payout.paid";
    public static final String PAYOUT_FAILED = "payout.failed";
    public static final String ACCOUNT_UPDATED = "account.updated";
    
    // Subscription events
    public static final String SUBSCRIPTION_CREATED = "subscription.created";
    public static final String SUBSCRIPTION_UPDATED = "subscription.updated";
    public static final String SUBSCRIPTION_CANCELED = "subscription.canceled";
    public static final String SUBSCRIPTION_TRIAL_ENDING = "subscription.trial_ending";
    
    // Invoice events
    public static final String INVOICE_CREATED = "invoice.created";
    public static final String INVOICE_FINALIZED = "invoice.finalized";
    public static final String INVOICE_PAID = "invoice.paid";
    public static final String INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
    public static final String INVOICE_VOIDED = "invoice.voided";

    private EventType() {
      // Utility class
    }
  }

  /**
   * Resource types that can appear in webhook events.
   */
  public static final class ResourceType {
    public static final String PAYMENT_INTENT = "payment_intent";
    public static final String CHARGE = "charge";
    public static final String REFUND = "refund";
    public static final String PAYOUT = "payout";
    public static final String ACCOUNT = "account";
    public static final String SUBSCRIPTION = "subscription";
    public static final String INVOICE = "invoice";

    private ResourceType() {
      // Utility class
    }
  }

  /**
   * Check if this event is related to a payment.
   */
  public boolean isPaymentEvent() {
    return eventType.startsWith("payment.");
  }

  /**
   * Check if this event is related to a payout.
   */
  public boolean isPayoutEvent() {
    return eventType.startsWith("payout.");
  }

  /**
   * Check if this event is related to an account.
   */
  public boolean isAccountEvent() {
    return eventType.startsWith("account.");
  }

  /**
   * Check if this event is related to a subscription.
   */
  public boolean isSubscriptionEvent() {
    return eventType.startsWith("subscription.");
  }

  /**
   * Check if this event is related to an invoice.
   */
  public boolean isInvoiceEvent() {
    return eventType.startsWith("invoice.");
  }

  /**
   * Get metadata value as String.
   */
  public Optional<String> getMetadataString(String key) {
    Object value = metadata.get(key);
    return Optional.ofNullable(value != null ? value.toString() : null);
  }

  /**
   * Get metadata value as Boolean.
   */
  public Optional<Boolean> getMetadataBoolean(String key) {
    Object value = metadata.get(key);
    if (value instanceof Boolean b) {
      return Optional.of(b);
    }
    return Optional.empty();
  }
}
