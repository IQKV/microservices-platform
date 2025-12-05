package com.iqscaffold.userservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UserEventTest {

  @Test
  void shouldCreateUserCreatedEvent() {
    var event = UserEvent.userCreated("user-123", "tenant-1", "user@example.com");

    assertNotNull(event);
    assertNotNull(event.getEventId());
    assertEquals("USER_CREATED", event.getEventType());
    assertEquals("user-123", event.getUserId());
    assertEquals("tenant-1", event.getTenantId());
    assertEquals("user@example.com", event.getEmail());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void shouldCreateUserUpdatedEvent() {
    var event = UserEvent.userUpdated("user-456", "tenant-2", "updated@example.com");

    assertNotNull(event);
    assertEquals("USER_UPDATED", event.getEventType());
    assertEquals("user-456", event.getUserId());
    assertEquals("tenant-2", event.getTenantId());
    assertEquals("updated@example.com", event.getEmail());
  }

  @Test
  void shouldCreateUserDeletedEvent() {
    var event = UserEvent.userDeleted("user-789", "tenant-3", "deleted@example.com");

    assertNotNull(event);
    assertEquals("USER_DELETED", event.getEventType());
    assertEquals("user-789", event.getUserId());
  }

  @Test
  void shouldCreateUserVerifiedEvent() {
    var event = UserEvent.userVerified("user-111", "tenant-4", "verified@example.com");

    assertNotNull(event);
    assertEquals("USER_VERIFIED", event.getEventType());
    assertEquals("user-111", event.getUserId());
  }

  @Test
  void shouldCreatePasswordResetEvent() {
    var event = UserEvent.passwordReset("user-222", "tenant-5", "reset@example.com");

    assertNotNull(event);
    assertEquals("PASSWORD_RESET", event.getEventType());
    assertEquals("user-222", event.getUserId());
  }

  @Test
  void shouldBuildEventWithMetadata() {
    Map<String, Object> metadata = Map.of("key1", "value1", "key2", "value2");
    var timestamp = Instant.now();

    var event = UserEvent.builder()
        .eventId("event-123")
        .eventType("CUSTOM_EVENT")
        .userId("user-333")
        .tenantId("tenant-6")
        .email("custom@example.com")
        .timestamp(timestamp)
        .metadata(metadata)
        .build();

    assertEquals("event-123", event.getEventId());
    assertEquals("CUSTOM_EVENT", event.getEventType());
    assertEquals("user-333", event.getUserId());
    assertEquals("tenant-6", event.getTenantId());
    assertEquals("custom@example.com", event.getEmail());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(metadata, event.getMetadata());
  }

  @Test
  void shouldHandleNullMetadata() {
    var event = UserEvent.builder()
        .eventId("event-456")
        .eventType("TEST_EVENT")
        .userId("user-444")
        .tenantId("tenant-7")
        .email("test@example.com")
        .timestamp(Instant.now())
        .metadata(null)
        .build();

    assertNull(event.getMetadata());
  }

  @Test
  void shouldCompareEvents() {
    var timestamp = Instant.now();
    var event1 = UserEvent.builder()
        .eventId("event-789")
        .eventType("TEST")
        .userId("user-555")
        .tenantId("tenant-8")
        .email("compare@example.com")
        .timestamp(timestamp)
        .build();

    var event2 = UserEvent.builder()
        .eventId("event-789")
        .eventType("TEST")
        .userId("user-555")
        .tenantId("tenant-8")
        .email("compare@example.com")
        .timestamp(timestamp)
        .build();

    assertEquals(event1, event2);
    assertEquals(event1.hashCode(), event2.hashCode());
  }
}
