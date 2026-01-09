package com.iqscaffold.billingservice.infrastructure.messaging;

import com.iqscaffold.billingservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of EventPublisher.
 * Publishes domain events to RabbitMQ exchange for consumption by other services.
 */
@Component
public class RabbitMQEventPublisher implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public RabbitMQEventPublisher(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publishMerchantOnboarded(MerchantOnboardedEvent event) {
    try {
      log.debug("Publishing merchant onboarded event for organization: {}", event.organizationId());
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          RabbitMQConfig.MERCHANT_ONBOARDED_KEY,
          event
      );
      log.info("Successfully published merchant onboarded event for organization: {}, stripe account: {}",
          event.organizationId(), event.stripeAccountId());
    } catch (final Exception e) {
      log.error("Failed to publish merchant onboarded event for organization: {}",
          event.organizationId(), e);
      throw new MessagingException("Failed to publish merchant onboarded event", e);
    }
  }

  @Override
  public void publishMerchantCapabilitiesUpdated(MerchantCapabilitiesUpdatedEvent event) {
    try {
      log.debug("Publishing merchant capabilities updated event for organization: {}", event.organizationId());
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          RabbitMQConfig.MERCHANT_CAPABILITIES_UPDATED_KEY,
          event
      );
      log.info("Successfully published merchant capabilities updated event for organization: {}, charges: {}, payouts: {}",
          event.organizationId(), event.chargesEnabled(), event.payoutsEnabled());
    } catch (final Exception e) {
      log.error("Failed to publish merchant capabilities updated event for organization: {}",
          event.organizationId(), e);
      throw new MessagingException("Failed to publish merchant capabilities updated event", e);
    }
  }
}
