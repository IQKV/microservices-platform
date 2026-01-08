package com.iqscaffold.userservice.infrastructure.messaging;

import com.iqscaffold.userservice.usermanagement.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Publisher for user lifecycle events.
 * Integrates with business logic to publish events to RabbitMQ.
 */
@Component
public class UserEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

  private final MessagingService messagingService;

  public UserEventPublisher(final MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  /**
   * Publish user created event.
   * Called after successful user registration or creation.
   */
  public void publishUserCreated(User user) {
    try {
      log.debug("Publishing user created event for user: {}", user.getId());
      
      messagingService.publishUserCreated(
          user.getId().toString(),
          user.getTenantId(),
          user.getEmail()
      );
      
      log.info("Published user created event for user: {} in tenant: {}",
          user.getId(), user.getTenantId());
    } catch (final Exception e) {
      // Log error but don't fail the main operation
      log.error("Failed to publish user created event for user: {}", user.getId(), e);
    }
  }

  /**
   * Publish user updated event.
   * Called after successful user profile update.
   */
  public void publishUserUpdated(User user) {
    try {
      log.debug("Publishing user updated event for user: {}", user.getId());
      
      messagingService.publishUserUpdated(
          user.getId().toString(),
          user.getTenantId(),
          user.getEmail()
      );
      
      log.info("Published user updated event for user: {} in tenant: {}",
          user.getId(), user.getTenantId());
    } catch (final Exception e) {
      // Log error but don't fail the main operation
      log.error("Failed to publish user updated event for user: {}", user.getId(), e);
    }
  }

  /**
   * Publish user deleted event.
   * Called after successful user deletion.
   */
  public void publishUserDeleted(User user) {
    try {
      log.debug("Publishing user deleted event for user: {}", user.getId());
      
      messagingService.publishUserDeleted(
          user.getId().toString(),
          user.getTenantId(),
          user.getEmail()
      );
      
      log.info("Published user deleted event for user: {} in tenant: {}",
          user.getId(), user.getTenantId());
    } catch (final Exception e) {
      // Log error but don't fail the main operation
      log.error("Failed to publish user deleted event for user: {}", user.getId(), e);
    }
  }

  /**
   * Publish user verified event.
   * Called after successful email verification.
   */
  public void publishUserVerified(User user) {
    try {
      log.debug("Publishing user verified event for user: {}", user.getId());
      
      messagingService.publishUserVerified(
          user.getId().toString(),
          user.getTenantId(),
          user.getEmail()
      );
      
      log.info("Published user verified event for user: {} in tenant: {}",
          user.getId(), user.getTenantId());
    } catch (final Exception e) {
      // Log error but don't fail the main operation
      log.error("Failed to publish user verified event for user: {}", user.getId(), e);
    }
  }

  /**
   * Publish password reset event.
   * Called after successful password reset.
   */
  public void publishPasswordReset(User user) {
    try {
      log.debug("Publishing password reset event for user: {}", user.getId());
      
      messagingService.publishPasswordReset(
          user.getId().toString(),
          user.getTenantId(),
          user.getEmail()
      );
      
      log.info("Published password reset event for user: {} in tenant: {}",
          user.getId(), user.getTenantId());
    } catch (final Exception e) {
      // Log error but don't fail the main operation
      log.error("Failed to publish password reset event for user: {}", user.getId(), e);
    }
  }

  /**
   * Publish user created event with explicit parameters.
   * Useful when User entity is not available.
   */
  public void publishUserCreated(Long userId, String tenantId, String email) {
    try {
      log.debug("Publishing user created event for user: {}", userId);
      
      messagingService.publishUserCreated(
          userId.toString(),
          tenantId,
          email
      );
      
      log.info("Published user created event for user: {} in tenant: {}", userId, tenantId);
    } catch (final Exception e) {
      log.error("Failed to publish user created event for user: {}", userId, e);
    }
  }

  /**
   * Publish user updated event with explicit parameters.
   * Useful when User entity is not available.
   */
  public void publishUserUpdated(Long userId, String tenantId, String email) {
    try {
      log.debug("Publishing user updated event for user: {}", userId);
      
      messagingService.publishUserUpdated(
          userId.toString(),
          tenantId,
          email
      );
      
      log.info("Published user updated event for user: {} in tenant: {}", userId, tenantId);
    } catch (final Exception e) {
      log.error("Failed to publish user updated event for user: {}", userId, e);
    }
  }

  /**
   * Publish user deleted event with explicit parameters.
   * Useful when User entity is not available.
   */
  public void publishUserDeleted(Long userId, String tenantId, String email) {
    try {
      log.debug("Publishing user deleted event for user: {}", userId);
      
      messagingService.publishUserDeleted(
          userId.toString(),
          tenantId,
          email
      );
      
      log.info("Published user deleted event for user: {} in tenant: {}", userId, tenantId);
    } catch (final Exception e) {
      log.error("Failed to publish user deleted event for user: {}", userId, e);
    }
  }

  /**
   * Publish user verified event with explicit parameters.
   * Useful when User entity is not available.
   */
  public void publishUserVerified(Long userId, String tenantId, String email) {
    try {
      log.debug("Publishing user verified event for user: {}", userId);
      
      messagingService.publishUserVerified(
          userId.toString(),
          tenantId,
          email
      );
      
      log.info("Published user verified event for user: {} in tenant: {}", userId, tenantId);
    } catch (final Exception e) {
      log.error("Failed to publish user verified event for user: {}", userId, e);
    }
  }

  /**
   * Publish password reset event with explicit parameters.
   * Useful when User entity is not available.
   */
  public void publishPasswordReset(Long userId, String tenantId, String email) {
    try {
      log.debug("Publishing password reset event for user: {}", userId);
      
      messagingService.publishPasswordReset(
          userId.toString(),
          tenantId,
          email
      );
      
      log.info("Published password reset event for user: {} in tenant: {}", userId, tenantId);
    } catch (final Exception e) {
      log.error("Failed to publish password reset event for user: {}", userId, e);
    }
  }
}
