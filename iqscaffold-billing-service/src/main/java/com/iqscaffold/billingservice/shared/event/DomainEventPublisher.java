package com.iqscaffold.billingservice.shared.event;

/**
 * Interface for publishing domain events.
 * Domain events should be published AFTER successful aggregate persistence
 * to ensure consistency and enable eventual consistency between aggregates.
 *
 * <p>Transactional Outbox Pattern (Recommended):
 * For production systems, implement the transactional outbox pattern to ensure
 * reliable event delivery:
 * <ol>
 *   <li>Store events in a database table within the same transaction as the aggregate</li>
 *   <li>A separate process polls the outbox table and publishes events to RabbitMQ</li>
 *   <li>Mark events as published after successful delivery</li>
 *   <li>Retry failed events with exponential backoff</li>
 * </ol>
 *
 * <p>This ensures that events are never lost, even if RabbitMQ is temporarily unavailable
 * or the application crashes after persisting the aggregate but before publishing the event.
 *
 * <p>Async Benefits:
 * Publishing events asynchronously provides several benefits:
 * <ul>
 *   <li>Decouples event producers from consumers</li>
 *   <li>Enables parallel processing of event handlers</li>
 *   <li>Prevents cascading failures (e.g., email service down doesn't block payments)</li>
 *   <li>Supports eventual consistency between aggregates</li>
 *   <li>Allows independent scaling of event consumers</li>
 * </ul>
 */
public interface DomainEventPublisher {

  /**
   * Publishes a domain event to the event bus (RabbitMQ).
   * Events are published asynchronously to decouple event producers from consumers.
   *
   * <p>The event is published AFTER successful aggregate persistence to ensure
   * consistency. Consumers can process events asynchronously without blocking
   * the main transaction.
   *
   * @param event the domain event to publish
   * @throws RuntimeException if event publishing fails
   */
  void publish(DomainEvent event);

  /**
   * Publishes multiple domain events in a batch.
   * Useful for publishing multiple related events from a single aggregate operation.
   *
   * <p>Events are published in order, and all events are published even if one fails.
   * Consider using this method when multiple events are generated from a single
   * aggregate operation (e.g., subscription upgrade generates both SubscriptionUpgraded
   * and InvoiceGenerated events).
   *
   * @param events the domain events to publish
   * @throws RuntimeException if any event publishing fails
   */
  void publishAll(DomainEvent... events);
}
