package com.iqscaffold.contactservice.webhook;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for webhook management.
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

  /**
   * Finds all active webhooks.
   *
   * @return List of active webhooks
   */
  @Cacheable(value = "activeWebhooks")
  List<Webhook> findByActiveTrue();

  /**
   * Finds active webhooks that subscribe to a specific event.
   * Results are cached to improve performance for frequent webhook triggering.
   *
   * @param event The event type
   * @return List of webhooks subscribed to the event
   */
  @Cacheable(value = "webhooksByEvent", key = "#event")
  @Query("SELECT w FROM Webhook w WHERE w.active = true AND w.events LIKE CONCAT('%', :event, '%')")
  List<Webhook> findActiveWebhooksByEvent(@Param("event") String event);

  /**
   * Finds active webhooks that subscribe to multiple events.
   * Use this for batch webhook processing.
   *
   * @param events List of event types
   * @return List of webhooks subscribed to any of the events
   */
  default List<Webhook> findActiveWebhooksByEvents(List<String> events) {
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    return findByActiveTrue().stream()
        .filter(w -> events.stream().anyMatch(e -> w.getEvents().contains(e)))
        .toList();
  }
}
