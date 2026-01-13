package com.iqscaffold.billingservice.subscription.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.config.RabbitMQConfig;
import com.iqscaffold.billingservice.infrastructure.messaging.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class SubscriptionEventPublisherTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  private SubscriptionEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new SubscriptionEventPublisher(rabbitTemplate);
  }

  @Test
  void publish_shouldSendEventToRabbitMQ() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.created"),
        eq(event)
    );
  }

  @Test
  void publish_shouldHandleRabbitMQException() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED);
    doThrow(new RuntimeException("RabbitMQ error")).when(rabbitTemplate)
        .convertAndSend(any(String.class), any(String.class), any(SubscriptionEvent.class));

    // When & Then
    assertThrows(MessagingException.class, () -> publisher.publish(event));
  }

  @Test
  void publishSubscriptionCreated_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED);

    // When
    publisher.publishSubscriptionCreated(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.created"),
        eq(event)
    );
  }

  @Test
  void publishSubscriptionUpdated_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_UPDATED);

    // When
    publisher.publishSubscriptionUpdated(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.updated"),
        eq(event)
    );
  }

  @Test
  void publishSubscriptionCanceled_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CANCELED);

    // When
    publisher.publishSubscriptionCanceled(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.canceled"),
        eq(event)
    );
  }

  @Test
  void publishSubscriptionPaused_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PAUSED);

    // When
    publisher.publishSubscriptionPaused(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.paused"),
        eq(event)
    );
  }

  @Test
  void publishSubscriptionResumed_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_RESUMED);

    // When
    publisher.publishSubscriptionResumed(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.resumed"),
        eq(event)
    );
  }

  @Test
  void publishSubscriptionTrialEnding_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_TRIAL_ENDING);

    // When
    publisher.publishSubscriptionTrialEnding(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.trial_ending"),
        eq(event)
    );
  }

  @Test
  void publishSubscriptionPlanChanged_shouldCallPublish() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PLAN_CHANGED);

    // When
    publisher.publishSubscriptionPlanChanged(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.plan_changed"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForCreated() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.created"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForUpdated() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_UPDATED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.updated"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForCanceled() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CANCELED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.canceled"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForPaused() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PAUSED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.paused"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForResumed() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_RESUMED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.resumed"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForTrialEnding() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_TRIAL_ENDING);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.trial_ending"),
        eq(event)
    );
  }

  @Test
  void getRoutingKey_shouldReturnCorrectKeyForPlanChanged() {
    // Given
    SubscriptionEvent event = createTestEvent(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PLAN_CHANGED);

    // When
    publisher.publish(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("billing.subscription.plan_changed"),
        eq(event)
    );
  }

  private SubscriptionEvent createTestEvent(SubscriptionEvent.SubscriptionEventType eventType) {
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        eventType,
        UUID.randomUUID(),
        "tenant-123",
        UUID.randomUUID(),
        "Test Plan",
        "ACTIVE",
        "sub_123",
        null,
        Instant.now()
    );
  }
}