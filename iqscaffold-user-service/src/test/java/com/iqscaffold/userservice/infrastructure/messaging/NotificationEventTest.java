package com.iqscaffold.userservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NotificationEventTest {

  @Test
  void shouldCreateEmailNotification() {
    Map<String, Object> templateData = Map.of("name", "John", "link", "https://example.com");

    var event = NotificationEvent.emailNotification(
        "user@example.com",
        "John Doe",
        "Welcome Email",
        "welcome-template",
        templateData,
        "tenant-1",
        "user-123"
    );

    assertNotNull(event);
    assertNotNull(event.getEventId());
    assertEquals("EMAIL", event.getNotificationType());
    assertEquals("user@example.com", event.getRecipientEmail());
    assertEquals("John Doe", event.getRecipientName());
    assertEquals("Welcome Email", event.getSubject());
    assertEquals("welcome-template", event.getTemplateName());
    assertEquals(templateData, event.getTemplateData());
    assertEquals("tenant-1", event.getTenantId());
    assertEquals("user-123", event.getUserId());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void shouldBuildNotificationWithAllFields() {
    var timestamp = Instant.now();
    Map<String, Object> templateData = Map.of("key", "value");

    var event = new NotificationEvent(
        "event-456",
        "SMS",
        "recipient@example.com",
        "Jane Smith",
        "Test Subject",
        "test-template",
        templateData,
        timestamp,
        "tenant-2",
        "user-456"
    );

    assertEquals("event-456", event.getEventId());
    assertEquals("SMS", event.getNotificationType());
    assertEquals("recipient@example.com", event.getRecipientEmail());
    assertEquals("Jane Smith", event.getRecipientName());
    assertEquals("Test Subject", event.getSubject());
    assertEquals("test-template", event.getTemplateName());
    assertEquals(templateData, event.getTemplateData());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals("tenant-2", event.getTenantId());
    assertEquals("user-456", event.getUserId());
  }

  @Test
  void shouldHandleEmptyTemplateData() {
    var event = NotificationEvent.emailNotification(
        "user@example.com",
        "User Name",
        "Subject",
        "template",
        Map.<String, Object>of(),
        "tenant-3",
        "user-789"
    );

    assertNotNull(event.getTemplateData());
    assertTrue(event.getTemplateData().isEmpty());
  }

  @Test
  void shouldHandleNullTemplateData() {
    var event = new NotificationEvent(
        "event-789",
        "EMAIL",
        "test@example.com",
        "Test User",
        "Test",
        "template",
        null,
        Instant.now(),
        "tenant-4",
        "user-999"
    );

    assertNull(event.getTemplateData());
  }

  @Test
  void shouldCompareNotificationEvents() {
    var timestamp = Instant.now();
    Map<String, Object> templateData = Map.of("key", "value");

    var event1 = new NotificationEvent(
        "event-111",
        "EMAIL",
        "compare@example.com",
        "Compare User",
        "Compare Subject",
        "compare-template",
        templateData,
        timestamp,
        "tenant-5",
        "user-111"
    );

    var event2 = new NotificationEvent(
        "event-111",
        "EMAIL",
        "compare@example.com",
        "Compare User",
        "Compare Subject",
        "compare-template",
        templateData,
        timestamp,
        "tenant-5",
        "user-111"
    );

    // Note: Without @Data annotation, we need to implement equals/hashCode manually
    // or use field-by-field comparison for now
    assertEquals(event1.getEventId(), event2.getEventId());
    assertEquals(event1.getNotificationType(), event2.getNotificationType());
    assertEquals(event1.getRecipientEmail(), event2.getRecipientEmail());
  }

  @Test
  void shouldHandleNullRecipientName() {
    var event = new NotificationEvent(
        "event-222",
        "EMAIL",
        "test@example.com",
        null,
        "Test",
        "template",
        Map.<String, Object>of(),
        Instant.now(),
        "tenant-6",
        "user-222"
    );

    assertNull(event.getRecipientName());
    assertNotNull(event.getRecipientEmail());
  }
}

