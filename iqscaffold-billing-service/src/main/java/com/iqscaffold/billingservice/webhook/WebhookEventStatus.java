package com.iqscaffold.billingservice.webhook;

/**
 * Enum representing the processing status of a webhook event.
 */
public enum WebhookEventStatus {
  /**
   * Webhook event received and queued for processing.
   */
  PENDING,
  
  /**
   * Webhook event successfully processed.
   */
  PROCESSED,
  
  /**
   * Webhook event processing failed.
   * Will be retried with exponential backoff.
   */
  FAILED,
  
  /**
   * Webhook event processing failed after all retry attempts.
   * Requires manual intervention.
   */
  DEAD_LETTER
}
