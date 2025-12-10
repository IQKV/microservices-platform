package com.iqscaffold.userservice.infrastructure.messaging;

import com.iqscaffold.userservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Example listener for user events
 * This demonstrates how to consume messages from RabbitMQ
 */
@Component
public class UserEventListener {

  private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

  public UserEventListener() {
  }

  /**
   * Listen to user events queue
   * This is an example listener - implement actual business logic as needed
   */
  @RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE)
  public void handleUserEvent(UserEvent event) {
    try {
      log.info("Received user event: {} for user: {} in tenant: {}",
          event.getEventType(), event.getUserId(), event.getTenantId());

      // Process the event based on type
      switch (event.getEventType()) {
        case "USER_CREATED":
          handleUserCreated(event);
          break;
        case "USER_UPDATED":
          handleUserUpdated(event);
          break;
        case "USER_DELETED":
          handleUserDeleted(event);
          break;
        case "USER_VERIFIED":
          handleUserVerified(event);
          break;
        case "PASSWORD_RESET":
          handlePasswordReset(event);
          break;
        default:
          log.warn("Unknown event type: {}", event.getEventType());
      }

      log.debug("Successfully processed user event: {}", event.getEventId());
    } catch (final Exception e) {
      log.error("Error processing user event: {}", event.getEventId(), e);
      throw e; // Re-throw to trigger retry or DLQ routing
    }
  }

  private void handleUserCreated(UserEvent event) {
    log.info("Processing user created event for user: {}", event.getUserId());
    // Implement business logic for user creation
    // e.g., send welcome email, create user profile, etc.
  }

  private void handleUserUpdated(UserEvent event) {
    log.info("Processing user updated event for user: {}", event.getUserId());
    // Implement business logic for user update
    // e.g., sync with external systems, update cache, etc.
  }

  private void handleUserDeleted(UserEvent event) {
    log.info("Processing user deleted event for user: {}", event.getUserId());
    // Implement business logic for user deletion
    // e.g., cleanup resources, notify other services, etc.
  }

  private void handleUserVerified(UserEvent event) {
    log.info("Processing user verified event for user: {}", event.getUserId());
    // Implement business logic for user verification
    // e.g., enable account features, send confirmation email, etc.
  }

  private void handlePasswordReset(UserEvent event) {
    log.info("Processing password reset event for user: {}", event.getUserId());
    // Implement business logic for password reset
    // e.g., log security event, notify user, etc.
  }
}
