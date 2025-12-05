package com.iqscaffold.userservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    var event = NotificationEvent.builder()
        .eventId("event-456")
        .notificationType("SMS")
        .recipientEmail("recipient@example.com")
        .recipientName("Jane Smith")
        .subject("Test Subject")
        .templateName("test-template")
        .templateData(templateData)
        .timestamp(timestamp)
        .tenantId("tenant-2")
        .userId("user-456")
        .build();

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
    var event = NotificationEvent.builder()
        .eventId("event-789")
        .notificationType("EMAIL")
        .recipientEmail("test@example.com")
        .recipientName("Test User")
        .subject("Test")
        .templateName("template")
        .templateData(null)
        .timestamp(Instant.now())
        .tenantId("tenant-4")
        .userId("user-999")
        .build();

    assertNull(event.getTemplateData());
  }

  @Test
  void shouldCompareNotificationEvents() {
    var timestamp = Instant.now();
    Map<String, Object> templateData = Map.of("key", "value");

    var event1 = NotificationEvent.builder()
        .eventId("event-111")
        .notificationType("EMAIL")
        .recipientEmail("compare@example.com")
        .recipientName("Compare User")
        .subject("Compare Subject")
        .templateName("compare-template")
        .templateData(templateData)
        .timestamp(timestamp)
        .tenantId("tenant-5")
        .userId("user-111")
        .build();

    var event2 = NotificationEvent.builder()
        .eventId("event-111")
        .notificationType("EMAIL")
        .recipientEmail("compare@example.com")
        .recipientName("Compare User")
        .subject("Compare Subject")
        .templateName("compare-template")
        .templateData(templateData)
        .timestamp(timestamp)
        .tenantId("tenant-5")
        .userId("user-111")
        .build();

    assertEquals(event1, event2);
    assertEquals(event1.hashCode(), event2.hashCode());
  }

  @Test
  void shouldHandleNullRecipientName() {
    var event = NotificationEvent.builder()
        .eventId("event-222")
        .notificationType("EMAIL")
        .recipientEmail("test@example.com")
        .recipientName(null)
        .subject("Test")
        .templateName("template")
        .templateData(Map.<String, Object>of())
        .timestamp(Instant.now())
        .tenantId("tenant-6")
        .userId("user-222")
        .build();

    assertNull(event.getRecipientName());
    assertNotNull(event.getRecipientEmail());
  }
}
