package com.iqscaffold.userservice.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iqscaffold.userservice.usermanagement.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserEventPublisherTest {

  @Mock
  private MessagingService messagingService;

  @Mock
  private User user;

  private UserEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new UserEventPublisher(messagingService);
  }

  @Test
  void shouldPublishUserCreatedEventWithUserEntity() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");

    publisher.publishUserCreated(user);

    verify(messagingService).publishUserCreated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserCreatedEventWithParameters() {
    publisher.publishUserCreated(123L, "tenant-1", "test@example.com");

    verify(messagingService).publishUserCreated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserUpdatedEventWithUserEntity() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");

    publisher.publishUserUpdated(user);

    verify(messagingService).publishUserUpdated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserUpdatedEventWithParameters() {
    publisher.publishUserUpdated(123L, "tenant-1", "test@example.com");

    verify(messagingService).publishUserUpdated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserDeletedEventWithUserEntity() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");

    publisher.publishUserDeleted(user);

    verify(messagingService).publishUserDeleted("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserDeletedEventWithParameters() {
    publisher.publishUserDeleted(123L, "tenant-1", "test@example.com");

    verify(messagingService).publishUserDeleted("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserVerifiedEventWithUserEntity() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");

    publisher.publishUserVerified(user);

    verify(messagingService).publishUserVerified("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishUserVerifiedEventWithParameters() {
    publisher.publishUserVerified(123L, "tenant-1", "test@example.com");

    verify(messagingService).publishUserVerified("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishPasswordResetEventWithUserEntity() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");

    publisher.publishPasswordReset(user);

    verify(messagingService).publishPasswordReset("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldPublishPasswordResetEventWithParameters() {
    publisher.publishPasswordReset(123L, "tenant-1", "test@example.com");

    verify(messagingService).publishPasswordReset("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldHandleExceptionWhenPublishingUserCreatedWithUserEntity() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");
    doThrow(new RuntimeException("Test exception"))
        .when(messagingService).publishUserCreated(anyString(), anyString(), anyString());

    // Should not throw exception - error is logged
    publisher.publishUserCreated(user);

    verify(messagingService).publishUserCreated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldHandleExceptionWhenPublishingUserCreatedWithParameters() {
    doThrow(new RuntimeException("Test exception"))
        .when(messagingService).publishUserCreated(anyString(), anyString(), anyString());

    // Should not throw exception - error is logged
    publisher.publishUserCreated(123L, "tenant-1", "test@example.com");

    verify(messagingService).publishUserCreated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldHandleExceptionWhenPublishingUserUpdated() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");
    doThrow(new RuntimeException("Test exception"))
        .when(messagingService).publishUserUpdated(anyString(), anyString(), anyString());

    // Should not throw exception - error is logged
    publisher.publishUserUpdated(user);

    verify(messagingService).publishUserUpdated("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldHandleExceptionWhenPublishingUserDeleted() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");
    doThrow(new RuntimeException("Test exception"))
        .when(messagingService).publishUserDeleted(anyString(), anyString(), anyString());

    // Should not throw exception - error is logged
    publisher.publishUserDeleted(user);

    verify(messagingService).publishUserDeleted("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldHandleExceptionWhenPublishingUserVerified() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");
    doThrow(new RuntimeException("Test exception"))
        .when(messagingService).publishUserVerified(anyString(), anyString(), anyString());

    // Should not throw exception - error is logged
    publisher.publishUserVerified(user);

    verify(messagingService).publishUserVerified("123", "tenant-1", "test@example.com");
  }

  @Test
  void shouldHandleExceptionWhenPublishingPasswordReset() {
    when(user.getId()).thenReturn(123L);
    when(user.getTenantId()).thenReturn("tenant-1");
    when(user.getEmail()).thenReturn("test@example.com");
    doThrow(new RuntimeException("Test exception"))
        .when(messagingService).publishPasswordReset(anyString(), anyString(), anyString());

    // Should not throw exception - error is logged
    publisher.publishPasswordReset(user);

    verify(messagingService).publishPasswordReset("123", "tenant-1", "test@example.com");
  }
}

