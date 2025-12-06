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
 * - Loose coupling between aggregates
 * - Eventual consistency
 * - Asynchronous processing of side effects (notifications, integrations)
 * - Scalable event-driven architecture
 */
@Component
public class RabbitMQDomainEventPublisher implements DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQDomainEventPublisher.class);

  private static final String EXCHANGE_NAME = "billing.domain.events";

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public RabbitMQDomainEventPublisher(final RabbitTemplate rabbitTemplate, final ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(final DomainEvent event) {
    try {
      var routingKey = event.eventType().toLowerCase().replace('_', '.');
      var eventJson = objectMapper.writeValueAsString(event);

      rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, eventJson, message -> {
        message.getMessageProperties().setHeader("eventType", event.eventType());
        message.getMessageProperties().setHeader("eventId", event.eventId().toString());
        message.getMessageProperties().setHeader("tenantId", event.tenantId().toString());
        message.getMessageProperties().setHeader("aggregateId", event.aggregateId().toString());
        message.getMessageProperties().setTimestamp(java.util.Date.from(event.occurredAt()));
        return message;
      });

      log.info(
        "Published domain event: type={}, eventId={}, aggregateId={}, tenantId={}",
        event.eventType(),
        event.eventId(),
        event.aggregateId(),
        event.tenantId()
      );
    } catch (Exception e) {
      log.error(
        "Failed to publish domain event: type={}, eventId={}, aggregateId={}",
        event.eventType(),
        event.eventId(),
        event.aggregateId(),
        e
      );
      // In production, consider using transactional outbox pattern
      // to ensure events are not lost on publish failure
      throw new RuntimeException("Failed to publish domain event", e);
    }
  }

  @Override
  public void publishAll(final DomainEvent... events) {
    for (var event : events) {
      publish(event);
    }
  }
}
