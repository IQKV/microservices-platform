package com.iqscaffold.billingservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UserEventTest {

  @Test
  void shouldCreateUserEventWithNoArgsConstructor() {
    var event = new UserEvent();

    assertNotNull(event);
  }

  @Test
  void shouldCreateUserEventWithAllArgs() {
    var eventId = "event-123";
    var eventType = "USER_CREATED";
    var userId = "user-456";
    var tenantId = "tenant-789";
    var email = "test@example.com";
    var timestamp = Instant.now();
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("source", "test");

    var event = new UserEvent(eventId, eventType, userId, tenantId, email, timestamp, metadata);

    assertEquals(eventId, event.getEventId());
    assertEquals(eventType, event.getEventType());
    assertEquals(userId, event.getUserId());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(email, event.getEmail());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(metadata, event.getMetadata());
  }

  @Test
  void shouldSetAndGetEventId() {
    var event = new UserEvent();
    var eventId = "event-123";

    event.setEventId(eventId);

    assertEquals(eventId, event.getEventId());
  }

  @Test
  void shouldSetAndGetEventType() {
    var event = new UserEvent();
    var eventType = "USER_CREATED";

    event.setEventType(eventType);

    assertEquals(eventType, event.getEventType());
  }

  @Test
  void shouldSetAndGetUserId() {
    var event = new UserEvent();
    var userId = "user-456";

    event.setUserId(userId);

    assertEquals(userId, event.getUserId());
  }

  @Test
  void shouldSetAndGetTenantId() {
    var event = new UserEvent();
    var tenantId = "tenant-789";

    event.setTenantId(tenantId);

    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void shouldSetAndGetEmail() {
    var event = new UserEvent();
    var email = "test@example.com";

    event.setEmail(email);

    assertEquals(email, event.getEmail());
  }

  @Test
  void shouldSetAndGetTimestamp() {
    var event = new UserEvent();
    var timestamp = Instant.now();

    event.setTimestamp(timestamp);

    assertEquals(timestamp, event.getTimestamp());
  }

  @Test
  void shouldSetAndGetMetadata() {
    var event = new UserEvent();
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("key", "value");

    event.setMetadata(metadata);

    assertEquals(metadata, event.getMetadata());
    assertEquals("value", event.getMetadata().get("key"));
  }

  @Test
  void shouldHandleNullMetadata() {
    var event = new UserEvent("id", "type", "userId", "tenantId", "email@test.com", Instant.now(), null);

    assertNotNull(event);
    assertEquals(null, event.getMetadata());
  }
}
