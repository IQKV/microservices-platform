package com.iqscaffold.billingservice.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a webhook event received from a payment provider.
 * 
 * <p>Webhook events are stored for:
 * <ul>
 *   <li><strong>Idempotency:</strong> Prevent duplicate processing of the same event</li>
 *   <li><strong>Audit Trail:</strong> Complete log of all webhook events for debugging</li>
 *   <li><strong>Replay:</strong> Enable webhook replay for debugging and recovery</li>
 *   <li><strong>Monitoring:</strong> Track webhook processing success/failure rates</li>
 * </ul>
 * 
 * <p>Events are retained for 30 days for audit and debugging purposes.
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  /**
   * The unique event ID from the payment provider.
   * Used as idempotency key to prevent duplicate processing.
   */
  @Column(name = "provider_event_id", nullable = false, unique = true, length = 255)
  private String providerEventId;
  
  /**
   * The payment provider name (stripe, paypal).
   */
  @Column(name = "provider", nullable = false, length = 50)
  private String provider;
  
  /**
   * The webhook event type (e.g., payment_intent.succeeded).
   */
  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;
  
  /**
   * The raw webhook payload (JSON).
   * Stored for audit and replay purposes.
   */
  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;
  
  /**
   * The webhook signature for verification.
   */
  @Column(name = "signature", nullable = false, length = 500)
  private String signature;
  
  /**
   * The processing status of the webhook event.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private WebhookEventStatus status;
  
  /**
   * Error message if processing failed.
   */
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;
  
  /**
   * Number of retry attempts.
   */
  @Column(name = "retry_count", nullable = false)
  @Builder.Default
  private int retryCount = 0;
  
  /**
   * Timestamp when the webhook was received.
   */
  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;
  
  /**
   * Timestamp when the webhook was processed.
   */
  @Column(name = "processed_at")
  private LocalDateTime processedAt;
  
  /**
   * Timestamp when the webhook was created in the database.
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
  
  /**
   * Timestamp when the webhook was last updated.
   */
  @Column(name = "updated_at")
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();
}
