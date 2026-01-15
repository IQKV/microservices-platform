package com.iqscaffold.contactservice.webhook;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
  List<Webhook> findByActiveTrue();

  /**
   * Finds active webhooks that subscribe to a specific event.
   *
   * @param event The event type
   * @return List of webhooks subscribed to the event
   */
  @Query("SELECT w FROM Webhook w WHERE w.active = true AND w.events LIKE %:event%")
  List<Webhook> findActiveWebhooksByEvent(String event);
}
