package com.iqscaffold.userservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.iqscaffold.userservice.config.RabbitMQConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  @Captor
  private ArgumentCaptor<UserEvent> userEventCaptor;

  @Captor
  private ArgumentCaptor<NotificationEvent> notificationEventCaptor;

  private MessagingService messagingService;

  @BeforeEach
  void setUp() {
    messagingService = new MessagingService(rabbitTemplate);
  }

  @Test
  void shouldPublishUserCreatedEvent() {
    messagingService.publishUserCreated("user-123", "tenant-1", "user@example.com");

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.USER_CREATED_KEY),
        userEventCaptor.capture()
    );

    var event = userEventCaptor.getValue();
    assertEquals("USER_CREATED", event.getEventType());
    assertEquals("user-123", event.getUserId());
    assertEquals("tenant-1", event.getTenantId());
    assertEquals("user@example.com", event.getEmail());
  }

  @Test
  void shouldPublishUserUpdatedEvent() {
    messagingService.publishUserUpdated("user-456", "tenant-2", "updated@example.com");

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.USER_UPDATED_KEY),
        userEventCaptor.capture()
    );

    var event = userEventCaptor.getValue();
    assertEquals("USER_UPDATED", event.getEventType());
    assertEquals("user-456", event.getUserId());
  }

  @Test
  void shouldPublishUserDeletedEvent() {
    messagingService.publishUserDeleted("user-789", "tenant-3", "deleted@example.com");

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.USER_DELETED_KEY),
        userEventCaptor.capture()
    );

    var event = userEventCaptor.getValue();
    assertEquals("USER_DELETED", event.getEventType());
    assertEquals("user-789", event.getUserId());
  }

  @Test
  void shouldPublishUserVerifiedEvent() {
    messagingService.publishUserVerified("user-111", "tenant-4", "verified@example.com");

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.USER_VERIFIED_KEY),
        userEventCaptor.capture()
    );

    var event = userEventCaptor.getValue();
    assertEquals("USER_VERIFIED", event.getEventType());
    assertEquals("user-111", event.getUserId());
  }

  @Test
  void shouldPublishPasswordResetEvent() {
    messagingService.publishPasswordReset("user-222", "tenant-5", "reset@example.com");

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.PASSWORD_RESET_KEY),
        userEventCaptor.capture()
    );

    var event = userEventCaptor.getValue();
    assertEquals("PASSWORD_RESET", event.getEventType());
    assertEquals("user-222", event.getUserId());
  }

  @Test
  void shouldPublishNotificationEvent() {
    Map<String, Object> templateData = Map.of("name", "John", "link", "https://example.com");
    var notification = NotificationEvent.emailNotification(
        "user@example.com",
        "John Doe",
        "Welcome",
        "welcome-template",
        templateData,
        "tenant-1",
        "user-123"
    );

    messagingService.publishNotificationEvent(notification);

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.NOTIFICATION_EMAIL_KEY),
        notificationEventCaptor.capture()
    );

    var event = notificationEventCaptor.getValue();
    assertEquals("EMAIL", event.getNotificationType());
    assertEquals("user@example.com", event.getRecipientEmail());
    assertEquals("John Doe", event.getRecipientName());
  }

  @Test
  void shouldPublishUserEventWithCustomRoutingKey() {
    var event = UserEvent.userCreated("user-333", "tenant-6", "custom@example.com");

    messagingService.publishUserEvent(event, "user.custom");

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq("user.custom"),
        eq(event)
    );
  }

  @Test
  void shouldThrowMessagingExceptionOnPublishFailure() {
    doThrow(new RuntimeException("Connection failed"))
        .when(rabbitTemplate)
        .convertAndSend(any(String.class), any(String.class), any(UserEvent.class));

    assertThrows(MessagingException.class, () ->
        messagingService.publishUserCreated("user-444", "tenant-7", "fail@example.com")
    );
  }

  @Test
  void shouldThrowMessagingExceptionOnNotificationPublishFailure() {
    var notification = NotificationEvent.emailNotification(
        "fail@example.com",
        "Fail User",
        "Subject",
        "template",
        Map.of(),
        "tenant-8",
        "user-555"
    );

    doThrow(new RuntimeException("Connection failed"))
        .when(rabbitTemplate)
        .convertAndSend(any(String.class), any(String.class), any(NotificationEvent.class));

    assertThrows(MessagingException.class, () ->
        messagingService.publishNotificationEvent(notification)
    );
  }

  @Test
  void shouldVerifyNoInteractionsWhenNotCalled() {
    verifyNoInteractions(rabbitTemplate);
  }
}

