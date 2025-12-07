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
 *
 * <p>Event Metadata:
 * Each event includes comprehensive metadata for traceability, idempotency, and versioning:
 * <ul>
 *   <li>eventId: Unique identifier for idempotency and deduplication</li>
 *   <li>occurredAt: Timestamp when the event occurred</li>
 *   <li>eventType: Type of domain event (e.g., "billing.subscription.created")</li>
 *   <li>aggregateId: ID of the aggregate that generated this event</li>
 *   <li>tenantId: Tenant context for multi-tenancy isolation</li>
 * </ul>
 *
 * <p>Event Versioning:
 * Events support schema evolution through versioning. The version is managed by the
 * publisher and included in message headers. Consumers can handle multiple versions
 * of the same event type for backward compatibility.
 */
public interface DomainEvent {

  /**
   * Returns the unique identifier of this event.
   * Used for idempotency and deduplication in event consumers.
   *
   * @return event ID
   */
  UUID eventId();

  /**
   * Returns the timestamp when this event occurred.
   * Used for event ordering and audit trails.
   *
   * @return event timestamp
   */
  Instant occurredAt();

  /**
   * Returns the type of this event (e.g., "billing.subscription.created").
   * Used for routing and consumer filtering.
   *
   * @return event type
   */
  String eventType();

  /**
   * Returns the ID of the aggregate that generated this event.
   * Used for event sourcing and aggregate reconstruction.
   *
   * @return aggregate ID
   */
  Long aggregateId();

  /**
   * Returns the tenant ID associated with this event.
   * Used for multi-tenant isolation and filtering.
   *
   * @return tenant ID
   */
  UUID tenantId();
}
