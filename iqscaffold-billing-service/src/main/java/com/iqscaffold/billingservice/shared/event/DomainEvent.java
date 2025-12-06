package com.iqscaffold.billingservice.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the billing service.
 * Domain events represent significant business occurrences that have already happened.
 * They are immutable and include timestamp and aggregate identity for traceability.
 *
 * <p>All domain events should be implemented as Java records for immutability and conciseness.
 *
 * <p>Design Rationale: Domain events enable loose coupling between aggregates and support
 * eventual consistency. They trigger side effects (notifications, integrations) without
 * coupling aggregates directly.
 */
public interface DomainEvent {

  /**
   * Returns the unique identifier of this event.
   *
   * @return event ID
   */
  UUID eventId();

  /**
   * Returns the timestamp when this event occurred.
   *
   * @return event timestamp
   */
  Instant occurredAt();

  /**
   * Returns the type of this event (e.g., "SUBSCRIPTION_CREATED").
   *
   * @return event type
   */
  String eventType();

  /**
   * Returns the ID of the aggregate that generated this event.
   *
   * @return aggregate ID
   */
  Long aggregateId();

  /**
   * Returns the tenant ID associated with this event.
   *
   * @return tenant ID
   */
  UUID tenantId();
}
