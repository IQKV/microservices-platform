package com.iqscaffold.billingservice.subscription.event;

import com.iqscaffold.billingservice.config.RabbitMQConfig;
import com.iqscaffold.billingservice.infrastructure.messaging.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for subscription lifecycle events.
 * <p>
 * Publishes subscription events to RabbitMQ for consumption by other services
 * (e.g., notification service, analytics, audit logging).
 */
@Component
public class SubscriptionEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  // Routing keys for subscription events
  private static final String SUBSCRIPTION_CREATED_KEY = "billing.subscription.created";
  private static final String SUBSCRIPTION_UPDATED_KEY = "billing.subscription.updated";
  private static final String SUBSCRIPTION_CANCELED_KEY = "billing.subscription.canceled";
  private static final String SUBSCRIPTION_PAUSED_KEY = "billing.subscription.paused";
  private static final String SUBSCRIPTION_RESUMED_KEY = "billing.subscription.resumed";
  private static final String SUBSCRIPTION_TRIAL_ENDING_KEY = "billing.subscription.trial_ending";
  private static final String SUBSCRIPTION_PLAN_CHANGED_KEY = "billing.subscription.plan_changed";

  public SubscriptionEventPublisher(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publishes a subscription event to RabbitMQ.
   *
   * @param event The subscription event to publish
   */
  public void publish(SubscriptionEvent event) {
    try {
      String routingKey = getRoutingKey(event.getEventType());

      log.debug("Publishing subscription event: type={}, subscriptionId={}, tenantId={}",
          event.getEventType(), event.getSubscriptionId(), event.getTenantId());

      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          routingKey,
          event
      );

      log.info("Successfully published subscription event: type={}, subscriptionId={}, tenantId={}",
          event.getEventType(), event.getSubscriptionId(), event.getTenantId());

    } catch (final Exception e) {
      log.error("Failed to publish subscription event: type={}, subscriptionId={}, tenantId={}",
          event.getEventType(), event.getSubscriptionId(), event.getTenantId(), e);
      throw new MessagingException("Failed to publish subscription event", e);
    }
  }

  /**
   * Publishes a subscription created event.
   */
  public void publishSubscriptionCreated(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Publishes a subscription updated event.
   */
  public void publishSubscriptionUpdated(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Publishes a subscription canceled event.
   */
  public void publishSubscriptionCanceled(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Publishes a subscription paused event.
   */
  public void publishSubscriptionPaused(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Publishes a subscription resumed event.
   */
  public void publishSubscriptionResumed(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Publishes a subscription trial ending event.
   */
  public void publishSubscriptionTrialEnding(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Publishes a subscription plan changed event.
   */
  public void publishSubscriptionPlanChanged(SubscriptionEvent event) {
    publish(event);
  }

  /**
   * Maps event type to routing key.
   */
  private String getRoutingKey(SubscriptionEvent.SubscriptionEventType eventType) {
    return switch (eventType) {
      case SUBSCRIPTION_CREATED -> SUBSCRIPTION_CREATED_KEY;
      case SUBSCRIPTION_UPDATED -> SUBSCRIPTION_UPDATED_KEY;
      case SUBSCRIPTION_CANCELED -> SUBSCRIPTION_CANCELED_KEY;
      case SUBSCRIPTION_PAUSED -> SUBSCRIPTION_PAUSED_KEY;
      case SUBSCRIPTION_RESUMED -> SUBSCRIPTION_RESUMED_KEY;
      case SUBSCRIPTION_TRIAL_ENDING -> SUBSCRIPTION_TRIAL_ENDING_KEY;
      case SUBSCRIPTION_PLAN_CHANGED -> SUBSCRIPTION_PLAN_CHANGED_KEY;
    };
  }
}
