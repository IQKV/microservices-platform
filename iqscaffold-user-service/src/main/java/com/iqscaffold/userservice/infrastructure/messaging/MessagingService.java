package com.iqscaffold.userservice.infrastructure.messaging;

import com.iqscaffold.userservice.config.RabbitMQConfig;
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
   * Publish a user event
   */
  public void publishUserEvent(UserEvent event, String routingKey) {
    try {
      log.debug("Publishing user event: {} with routing key: {}", event.getEventType(), routingKey);
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EVENTS_EXCHANGE,
          routingKey,
          event
      );
      log.info("Successfully published user event: {} for user: {}",
          event.getEventType(), event.getUserId());
    } catch (Exception e) {
      log.error("Failed to publish user event: {} for user: {}",
          event.getEventType(), event.getUserId(), e);
      throw new MessagingException("Failed to publish user event", e);
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
    } catch (Exception e) {
      log.error("Failed to publish notification event to: {}",
          event.getRecipientEmail(), e);
      throw new MessagingException("Failed to publish notification event", e);
    }
  }

  /**
   * Publish user created event
   */
  public void publishUserCreated(String userId, String tenantId, String email) {
    UserEvent event = UserEvent.userCreated(userId, tenantId, email);
    publishUserEvent(event, RabbitMQConfig.USER_CREATED_KEY);
  }

  /**
   * Publish user updated event
   */
  public void publishUserUpdated(String userId, String tenantId, String email) {
    UserEvent event = UserEvent.userUpdated(userId, tenantId, email);
    publishUserEvent(event, RabbitMQConfig.USER_UPDATED_KEY);
  }

  /**
   * Publish user deleted event
   */
  public void publishUserDeleted(String userId, String tenantId, String email) {
    UserEvent event = UserEvent.userDeleted(userId, tenantId, email);
    publishUserEvent(event, RabbitMQConfig.USER_DELETED_KEY);
  }

  /**
   * Publish user verified event
   */
  public void publishUserVerified(String userId, String tenantId, String email) {
    UserEvent event = UserEvent.userVerified(userId, tenantId, email);
    publishUserEvent(event, RabbitMQConfig.USER_VERIFIED_KEY);
  }

  /**
   * Publish password reset event
   */
  public void publishPasswordReset(String userId, String tenantId, String email) {
    UserEvent event = UserEvent.passwordReset(userId, tenantId, email);
    publishUserEvent(event, RabbitMQConfig.PASSWORD_RESET_KEY);
  }
}
