package com.iqscaffold.billingservice.webhook;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for WebhookEvent entity.
 * 
 * <p>Provides data access methods for webhook event storage and retrieval,
 * supporting idempotency checks and audit queries.
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
  
  /**
   * Checks if a webhook event with the given provider event ID exists.
   * Used for idempotency checks to prevent duplicate processing.
   * 
   * @param providerEventId the provider event ID
   * @return true if event exists, false otherwise
   */
  boolean existsByProviderEventId(String providerEventId);
  
  /**
   * Finds webhook events by provider and status.
   * 
   * @param provider the provider name
   * @param status the event status
   * @return list of webhook events
   */
  List<WebhookEvent> findByProviderAndStatus(String provider, WebhookEventStatus status);
  
  /**
   * Finds failed webhook events that need retry.
   * 
   * @param maxRetryCount maximum retry count
   * @return list of webhook events eligible for retry
   */
  @Query("""
      SELECT w FROM WebhookEvent w
      WHERE w.status = 'FAILED'
      AND w.retryCount < :maxRetryCount
      ORDER BY w.receivedAt ASC
      """)
  List<WebhookEvent> findFailedEventsForRetry(@Param("maxRetryCount") int maxRetryCount);
  
  /**
   * Finds webhook events older than the specified date for cleanup.
   * 
   * @param cutoffDate the cutoff date
   * @return list of old webhook events
   */
  List<WebhookEvent> findByReceivedAtBefore(LocalDateTime cutoffDate);
  
  /**
   * Finds webhook events by event type and status.
   * 
   * @param eventType the event type
   * @param status the event status
   * @return list of webhook events
   */
  List<WebhookEvent> findByEventTypeAndStatus(String eventType, WebhookEventStatus status);
}
