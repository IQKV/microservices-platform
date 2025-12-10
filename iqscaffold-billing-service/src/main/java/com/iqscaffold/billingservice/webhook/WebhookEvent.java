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
  private LocalDateTime createdAt = LocalDateTime.now();

  /**
   * Timestamp when the webhook was last updated.
   */
  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  public WebhookEvent() {
  }

  public WebhookEvent(final Long id, final String providerEventId, final String provider, final String eventType,
                      final String payload, final String signature, final WebhookEventStatus status,
                      final String errorMessage, final int retryCount, final LocalDateTime receivedAt,
                      final LocalDateTime processedAt, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
    this.id = id;
    this.providerEventId = providerEventId;
    this.provider = provider;
    this.eventType = eventType;
    this.payload = payload;
    this.signature = signature;
    this.status = status;
    this.errorMessage = errorMessage;
    this.retryCount = retryCount;
    this.receivedAt = receivedAt;
    this.processedAt = processedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getProviderEventId() {
    return providerEventId;
  }

  public void setProviderEventId(String providerEventId) {
    this.providerEventId = providerEventId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public WebhookEventStatus getStatus() {
    return status;
  }

  public void setStatus(WebhookEventStatus status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public LocalDateTime getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(LocalDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  public LocalDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(LocalDateTime processedAt) {
    this.processedAt = processedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public static WebhookEventBuilder builder() {
    return new WebhookEventBuilder();
  }

  public static class WebhookEventBuilder {
    private Long id;
    private String providerEventId;
    private String provider;
    private String eventType;
    private String payload;
    private String signature;
    private WebhookEventStatus status;
    private String errorMessage;
    private int retryCount = 0;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    WebhookEventBuilder() {
    }

    public WebhookEventBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public WebhookEventBuilder providerEventId(String providerEventId) {
      this.providerEventId = providerEventId;
      return this;
    }

    public WebhookEventBuilder provider(String provider) {
      this.provider = provider;
      return this;
    }

    public WebhookEventBuilder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public WebhookEventBuilder payload(String payload) {
      this.payload = payload;
      return this;
    }

    public WebhookEventBuilder signature(String signature) {
      this.signature = signature;
      return this;
    }

    public WebhookEventBuilder status(WebhookEventStatus status) {
      this.status = status;
      return this;
    }

    public WebhookEventBuilder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public WebhookEventBuilder retryCount(int retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    public WebhookEventBuilder receivedAt(LocalDateTime receivedAt) {
      this.receivedAt = receivedAt;
      return this;
    }

    public WebhookEventBuilder processedAt(LocalDateTime processedAt) {
      this.processedAt = processedAt;
      return this;
    }

    public WebhookEventBuilder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public WebhookEventBuilder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public WebhookEvent build() {
      return new WebhookEvent(id, providerEventId, provider, eventType, payload, signature,
          status, errorMessage, retryCount, receivedAt, processedAt, createdAt, updatedAt);
    }
  }
}
