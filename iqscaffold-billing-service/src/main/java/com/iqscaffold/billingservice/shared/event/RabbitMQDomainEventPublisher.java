package com.iqscaffold.billingservice.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of the domain event publisher.
 * Publishes domain events to RabbitMQ exchange for asynchronous processing.
 *
 * <p>Events are published AFTER successful aggregate persistence to ensure
 * consistency. Consumers can process events asynchronously without blocking
 * the main transaction.
 *
 * <p>Design Rationale: Using RabbitMQ for event publishing enables:
 * <ul>
 *   <li>Loose coupling between aggregates</li>
 *   <li>Eventual consistency</li>
 *   <li>Asynchronous processing of side effects (notifications, integrations)</li>
 *   <li>Scalable event-driven architecture</li>
 *   <li>Parallel processing of event handlers</li>
 *   <li>Prevention of cascading failures</li>
 * </ul>
 *
 * <p>Event Metadata:
 * Each published event includes comprehensive metadata for traceability and versioning:
 * <ul>
 *   <li>eventId: Unique identifier for idempotency</li>
 *   <li>eventType: Type of domain event</li>
 *   <li>eventVersion: Schema version for evolution</li>
 *   <li>aggregateId: ID of the aggregate that generated the event</li>
 *   <li>aggregateType: Type of aggregate (Subscription, Invoice, Payment, etc.)</li>
 *   <li>tenantId: Tenant context for multi-tenancy</li>
 *   <li>timestamp: When the event occurred</li>
 * </ul>
 *
 * <p>Event Versioning:
 * Events include a version number to support schema evolution. Consumers can handle
 * multiple versions of the same event type, enabling backward compatibility during
 * rolling deployments and gradual migrations.
 *
 * <p>Async Benefits:
 * <ul>
 *   <li>SubscriptionCreated → triggers welcome email, analytics update</li>
 *   <li>InvoiceGenerated → triggers PDF generation, email notification</li>
 *   <li>PaymentFailed → triggers retry scheduling, notification</li>
 *   <li>UsageRecorded → triggers quota check, analytics update</li>
 *   <li>QuotaExceeded → triggers notification, feature throttling</li>
 * </ul>
 *
 * <p>Note: In production, consider implementing the transactional outbox pattern
 * to ensure events are not lost on publish failure. This involves storing events
 * in a database table within the same transaction as the aggregate, then publishing
 * them asynchronously via a separate process.
 */
@Component
public class RabbitMQDomainEventPublisher implements DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQDomainEventPublisher.class);

  /**
   * Main exchange for billing domain events.
   * Matches the exchange configured in RabbitMQConfig.
   */
  private static final String EXCHANGE_NAME = "billing.events";

  /**
   * Current event schema version.
   * Increment this when making breaking changes to event structure.
   */
  private static final String EVENT_VERSION = "1.0";

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public RabbitMQDomainEventPublisher(final RabbitTemplate rabbitTemplate, final ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(final DomainEvent event) {
    try {
      // Convert event type to routing key format (e.g., "billing.subscription.created")
      var routingKey = event.eventType();

      // Serialize event to JSON
      var eventJson = objectMapper.writeValueAsString(event);

      // Publish to RabbitMQ with comprehensive metadata
      rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, eventJson, message -> {
        var properties = message.getMessageProperties();

        // Core event metadata
        properties.setHeader("eventId", event.eventId().toString());
        properties.setHeader("eventType", event.eventType());
        properties.setHeader("eventVersion", EVENT_VERSION);

        // Aggregate metadata
        properties.setHeader("aggregateId", event.aggregateId().toString());
        properties.setHeader("aggregateType", extractAggregateType(event.eventType()));

        // Tenant context for multi-tenancy
        properties.setHeader("tenantId", event.tenantId().toString());

        // Timestamp
        properties.setTimestamp(java.util.Date.from(event.occurredAt()));

        // Content type for proper deserialization
        properties.setContentType("application/json");

        return message;
      });

      log.info(
          "Published domain event: type={}, eventId={}, aggregateId={}, tenantId={}, version={}",
          event.eventType(),
          event.eventId(),
          event.aggregateId(),
          event.tenantId(),
          EVENT_VERSION
      );
    } catch (final Exception e) {
      log.error(
          "Failed to publish domain event: type={}, eventId={}, aggregateId={}, tenantId={}",
          event.eventType(),
          event.eventId(),
          event.aggregateId(),
          event.tenantId(),
          e
      );
      // In production, consider using transactional outbox pattern
      // to ensure events are not lost on publish failure
      throw new RuntimeException("Failed to publish domain event", e);
    }
  }

  @Override
  public void publishAll(final DomainEvent... events) {
    for (final var event : events) {
      publish(event);
    }
  }

  /**
   * Extracts the aggregate type from the event type.
   * Example: "billing.subscription.created" → "Subscription"
   *
   * @param eventType the event type
   * @return the aggregate type
   */
  private String extractAggregateType(final String eventType) {
    var parts = eventType.split("\\.");
    if (parts.length >= 2) {
      var aggregateType = parts[1];
      return Character.toUpperCase(aggregateType.charAt(0)) + aggregateType.substring(1);
    }
    return "Unknown";
  }
}
