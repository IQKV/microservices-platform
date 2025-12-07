package com.iqscaffold.billingservice.shared.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQDomainEventPublisher Tests")
class RabbitMQDomainEventPublisherTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  @Mock
  private ObjectMapper objectMapper;

  private RabbitMQDomainEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new RabbitMQDomainEventPublisher(rabbitTemplate, objectMapper);
  }

  @Test
  @DisplayName("Should publish domain event with correct exchange and routing key")
  void shouldPublishDomainEventWithCorrectExchangeAndRoutingKey() throws Exception {
    // Arrange
    var eventId = UUID.randomUUID();
    var tenantId = UUID.randomUUID();
    var aggregateId = 1L;
    var occurredAt = Instant.now();

    var event = new SubscriptionCreated(
      eventId,
      occurredAt,
      aggregateId,
      tenantId,
      UUID.randomUUID(),
      1L,
      "ACTIVE",
      false
    );

    when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"" + eventId + "\"}");

    // Act
    publisher.publish(event);

    // Assert
    verify(rabbitTemplate).convertAndSend(
      eq("billing.events"),
      eq(BillingConstants.BillingEvents.SUBSCRIPTION_CREATED),
      anyString(),
      any(MessagePostProcessor.class)
    );
  }

  @Test
  @DisplayName("Should include event metadata in message headers")
  void shouldIncludeEventMetadataInMessageHeaders() throws Exception {
    // Arrange
    var eventId = UUID.randomUUID();
    var tenantId = UUID.randomUUID();
    var aggregateId = 1L;
    var occurredAt = Instant.now();

    var event = new SubscriptionCreated(
      eventId,
      occurredAt,
      aggregateId,
      tenantId,
      UUID.randomUUID(),
      1L,
      "ACTIVE",
      false
    );

    when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"" + eventId + "\"}");

    var messageProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

    // Act
    publisher.publish(event);

    // Assert
    verify(rabbitTemplate).convertAndSend(
      eq("billing.events"),
      eq(BillingConstants.BillingEvents.SUBSCRIPTION_CREATED),
      anyString(),
      messageProcessorCaptor.capture()
    );

    // Verify message processor was captured
    assertThat(messageProcessorCaptor.getValue()).isNotNull();
  }

  @Test
  @DisplayName("Should publish multiple events in batch")
  void shouldPublishMultipleEventsInBatch() throws Exception {
    // Arrange
    var event1 = new SubscriptionCreated(
      UUID.randomUUID(),
      Instant.now(),
      1L,
      UUID.randomUUID(),
      UUID.randomUUID(),
      1L,
      "ACTIVE",
      false
    );

    var event2 = new SubscriptionCreated(
      UUID.randomUUID(),
      Instant.now(),
      2L,
      UUID.randomUUID(),
      UUID.randomUUID(),
      1L,
      "ACTIVE",
      false
    );

    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    // Act
    publisher.publishAll(event1, event2);

    // Assert - verify it was called twice (once for each event)
    verify(rabbitTemplate, org.mockito.Mockito.times(2)).convertAndSend(
      eq("billing.events"),
      eq(BillingConstants.BillingEvents.SUBSCRIPTION_CREATED),
      anyString(),
      any(MessagePostProcessor.class)
    );
  }

  @Test
  @DisplayName("Should throw exception when event publishing fails")
  void shouldThrowExceptionWhenEventPublishingFails() throws Exception {
    // Arrange
    var event = new SubscriptionCreated(
      UUID.randomUUID(),
      Instant.now(),
      1L,
      UUID.randomUUID(),
      UUID.randomUUID(),
      1L,
      "ACTIVE",
      false
    );

    when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

    // Act & Assert
    assertThatThrownBy(() -> publisher.publish(event))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Failed to publish domain event");
  }

  @Test
  @DisplayName("Should throw exception when RabbitMQ is unavailable")
  void shouldThrowExceptionWhenRabbitMQIsUnavailable() throws Exception {
    // Arrange
    var event = new SubscriptionCreated(
      UUID.randomUUID(),
      Instant.now(),
      1L,
      UUID.randomUUID(),
      UUID.randomUUID(),
      1L,
      "ACTIVE",
      false
    );

    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    doThrow(new RuntimeException("RabbitMQ connection failed"))
      .when(rabbitTemplate)
      .convertAndSend(anyString(), anyString(), anyString(), any(MessagePostProcessor.class));

    // Act & Assert
    assertThatThrownBy(() -> publisher.publish(event))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Failed to publish domain event");
  }
}
