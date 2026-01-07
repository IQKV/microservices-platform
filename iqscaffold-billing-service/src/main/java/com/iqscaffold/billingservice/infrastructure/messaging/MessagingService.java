package com.iqscaffold.billingservice.infrastructure.messaging;

import com.iqscaffold.billingservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing messages to RabbitMQ
 */
@Service
public class MessagingService {

  private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

  private final RabbitTemplate rabbitTemplate;

  public MessagingService(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publish a billing event
   */
  public void publishBillingEvent(BillingEvent event, String routingKey) {
    try {
      log.debug("Publishing billing event: {} with routing key: {}", event.getEventType(), routingKey);
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          routingKey,
          event
      );
      log.info("Successfully published billing event: {} for payment: {}",
          event.getEventType(), event.getPaymentId());
    } catch (final Exception e) {
      log.error("Failed to publish billing event: {} for payment: {}",
          event.getEventType(), event.getPaymentId(), e);
      throw new MessagingException("Failed to publish billing event", e);
    }
  }

  /**
   * Publish a notification event
   */
  public void publishNotificationEvent(NotificationEvent event) {
    try {
      log.debug("Publishing notification event: {} to: {}",
          event.getNotificationType(), event.getRecipientEmail());
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          RabbitMQConfig.NOTIFICATION_EMAIL_KEY,
          event
      );
      log.info("Successfully published notification event to: {}", event.getRecipientEmail());
    } catch (final Exception e) {
      log.error("Failed to publish notification event to: {}",
          event.getRecipientEmail(), e);
      throw new MessagingException("Failed to publish notification event", e);
    }
  }

  /**
   * Publish payment successful event
   */
  public void publishPaymentSuccessful(String paymentId, String tenantId, String customerEmail) {
    BillingEvent event = BillingEvent.paymentSuccessful(paymentId, tenantId, customerEmail);
    publishBillingEvent(event, RabbitMQConfig.PAYMENT_SUCCESSFUL_KEY);
  }

  /**
   * Publish payment failed event
   */
  public void publishPaymentFailed(String paymentId, String tenantId, String customerEmail) {
    BillingEvent event = BillingEvent.paymentFailed(paymentId, tenantId, customerEmail);
    publishBillingEvent(event, RabbitMQConfig.PAYMENT_FAILED_KEY);
  }

  /**
   * Publish payment refunded event
   */
  public void publishPaymentRefunded(String paymentId, String tenantId, String customerEmail) {
    BillingEvent event = BillingEvent.paymentRefunded(paymentId, tenantId, customerEmail);
    publishBillingEvent(event, RabbitMQConfig.PAYMENT_REFUNDED_KEY);
  }

  /**
   * Publish merchant onboarding event
   */
  public void publishMerchantOnboarding(String merchantId, String tenantId, String merchantEmail) {
    BillingEvent event = BillingEvent.merchantOnboarding(merchantId, tenantId, merchantEmail);
    publishBillingEvent(event, RabbitMQConfig.MERCHANT_ONBOARDING_KEY);
  }

  /**
   * Publish invoice generated event
   */
  public void publishInvoiceGenerated(String invoiceId, String tenantId, String customerEmail) {
    BillingEvent event = BillingEvent.invoiceGenerated(invoiceId, tenantId, customerEmail);
    publishBillingEvent(event, RabbitMQConfig.INVOICE_GENERATED_KEY);
  }
}
