package com.iqscaffold.billingservice.shared.event;

/**
 * Interface for publishing domain events.
 * Domain events should be published AFTER successful aggregate persistence
 * to ensure consistency and enable eventual consistency between aggregates.
 *
 * <p>Implementation should use the transactional outbox pattern or similar
 * mechanism to ensure reliable event delivery.
 */
public interface DomainEventPublisher {

  /**
   * Publishes a domain event to the event bus (RabbitMQ).
   * Events are published asynchronously to decouple event producers from consumers.
   *
   * @param event the domain event to publish
   */
  void publish(DomainEvent event);

  /**
   * Publishes multiple domain events in a batch.
   * Useful for publishing multiple related events from a single aggregate operation.
   *
   * @param events the domain events to publish
   */
  void publishAll(DomainEvent... events);
}
