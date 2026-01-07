package com.iqscaffold.billingservice.webhook;

/**
 * Interface for handling incoming webhooks from payment providers to synchronize
 * local state with gateway state.
 * <p>
 * This interface defines the contract for webhook processing including:
 * <ul>
 *   <li>Cryptographic signature validation</li>
 *   <li>Event parsing and routing</li>
 *   <li>Multi-tenant context resolution</li>
 *   <li>Idempotent event processing</li>
 * </ul>
 *
 * <h4>Security Features:</h4>
 * <ul>
 *   <li><strong>Signature Verification</strong> - Validates webhook authenticity</li>
 *   <li><strong>Tenant Resolution</strong> - Resolves tenant context from event metadata</li>
 *   <li><strong>Event Validation</strong> - Ensures event integrity and structure</li>
 *   <li><strong>Idempotency</strong> - Safe to process duplicate events</li>
 * </ul>
 *
 * <h4>Supported Events:</h4>
 * <ul>
 *   <li>payment_intent.succeeded, payment_intent.payment_failed - Updates payment status</li>
 *   <li>charge.refunded - Syncs refund status</li>
 *   <li>payout.paid - Records Payout entities</li>
 *   <li>account.updated - Updates merchant configuration capabilities</li>
 * </ul>
 *
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public interface WebhookService {

  /**
   * Main entry point for processing webhook payloads.
   * <p>
   * Processing Steps:
   * <ol>
   *   <li>Constructs the payment provider Event object, verifying the cryptographic signature</li>
   *   <li>Resolves tenant context from event metadata</li>
   *   <li>Dispatches the event to the appropriate handler based on event type</li>
   * </ol>
   * <p>
   * Note: This method is designed to be idempotent-safe. Repeated calls for the
   * same event should generally result in the same outcome (e.g., updating status to
   * 'SUCCEEDED' is safe to do twice).
   *
   * @param payload   The raw JSON payload from the request body
   * @param sigHeader The signature header value for verification (e.g., Stripe-Signature)
   * @throws IllegalArgumentException If signature verification fails or webhook parsing fails
   */
  void processWebhook(String payload, String sigHeader);
}
