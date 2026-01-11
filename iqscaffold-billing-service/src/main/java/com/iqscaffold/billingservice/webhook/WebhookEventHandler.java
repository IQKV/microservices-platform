package com.iqscaffold.billingservice.webhook;

/**
 * Interface for handling normalized webhook events from payment providers.
 * <p>
 * Implementations process specific types of webhook events (payments, payouts, accounts)
 * in a provider-agnostic manner by working with the normalized {@link WebhookEvent} structure.
 * <p>
 * This design enables:
 * <ul>
 *   <li>Single Responsibility - Each handler focuses on one domain area</li>
 *   <li>Provider Independence - Handlers work with normalized events, not provider-specific formats</li>
 *   <li>Easy Testing - Handlers can be tested with mock WebhookEvent objects</li>
 *   <li>Extensibility - New event types can be added without modifying existing handlers</li>
 * </ul>
 */
public interface WebhookEventHandler {

  /**
   * Process a normalized webhook event.
   * <p>
   * Implementations should:
   * <ol>
   *   <li>Validate the event is relevant to this handler</li>
   *   <li>Update local state based on the event</li>
   *   <li>Publish domain events if needed</li>
   *   <li>Be idempotent - safe to process the same event multiple times</li>
   * </ol>
   *
   * @param event The normalized webhook event to process
   * @throws IllegalArgumentException If the event is invalid or unsupported by this handler
   */
  void handleEvent(WebhookEvent event);

  /**
   * Check if this handler supports the given event type.
   *
   * @param event The webhook event to check
   * @return true if this handler can process the event, false otherwise
   */
  boolean supports(WebhookEvent event);
}
