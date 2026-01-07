package com.iqscaffold.billingservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NotificationEventTest {

  @Test
  void constructor_shouldCreateNotificationEventWithAllFields() {
    // Given
    String eventId = "event-123";
    String notificationType = "EMAIL";
    String recipientEmail = "test@example.com";
    String recipientName = "Test User";
    String subject = "Test Subject";
    String templateName = "test-template";
    Map<String, Object> templateData = new HashMap<>();
    templateData.put("key", "value");
    Instant timestamp = Instant.now();
    String tenantId = "tenant-123";
    String customerId = "customer-123";

    // When
    NotificationEvent event = new NotificationEvent(eventId, notificationType, recipientEmail,
        recipientName, subject, templateName, templateData, timestamp, tenantId, customerId);

    // Then
    assertEquals(eventId, event.getEventId());
    assertEquals(notificationType, event.getNotificationType());
    assertEquals(recipientEmail, event.getRecipientEmail());
    assertEquals(recipientName, event.getRecipientName());
    assertEquals(subject, event.getSubject());
    assertEquals(templateName, event.getTemplateName());
    assertEquals(templateData, event.getTemplateData());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(customerId, event.getCustomerId());
  }

  @Test
  void defaultConstructor_shouldCreateEmptyNotificationEvent() {
    // When
    NotificationEvent event = new NotificationEvent();

    // Then
    assertNotNull(event);
  }

  @Test
  void setters_shouldSetAllFields() {
    // Given
    NotificationEvent event = new NotificationEvent();
    String eventId = "event-456";
    String notificationType = "SMS";
    String recipientEmail = "user@example.com";
    String recipientName = "User Name";
    String subject = "Subject";
    String templateName = "template";
    Map<String, Object> templateData = new HashMap<>();
    Instant timestamp = Instant.now();
    String tenantId = "tenant-456";
    String customerId = "customer-456";

    // When
    event.setEventId(eventId);
    event.setNotificationType(notificationType);
    event.setRecipientEmail(recipientEmail);
    event.setRecipientName(recipientName);
    event.setSubject(subject);
    event.setTemplateName(templateName);
    event.setTemplateData(templateData);
    event.setTimestamp(timestamp);
    event.setTenantId(tenantId);
    event.setCustomerId(customerId);

    // Then
    assertEquals(eventId, event.getEventId());
    assertEquals(notificationType, event.getNotificationType());
    assertEquals(recipientEmail, event.getRecipientEmail());
    assertEquals(recipientName, event.getRecipientName());
    assertEquals(subject, event.getSubject());
    assertEquals(templateName, event.getTemplateName());
    assertEquals(templateData, event.getTemplateData());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(customerId, event.getCustomerId());
  }

  @Test
  void emailNotification_shouldCreateEmailNotificationEvent() {
    // Given
    String recipientEmail = "recipient@example.com";
    String recipientName = "Recipient Name";
    String subject = "Email Subject";
    String templateName = "email-template";
    Map<String, Object> templateData = new HashMap<>();
    templateData.put("name", "John");
    String tenantId = "tenant-789";
    String customerId = "customer-789";

    // When
    NotificationEvent event = NotificationEvent.emailNotification(
        recipientEmail, recipientName, subject, templateName, templateData, tenantId, customerId);

    // Then
    assertNotNull(event.getEventId());
    assertEquals("EMAIL", event.getNotificationType());
    assertEquals(recipientEmail, event.getRecipientEmail());
    assertEquals(recipientName, event.getRecipientName());
    assertEquals(subject, event.getSubject());
    assertEquals(templateName, event.getTemplateName());
    assertEquals(templateData, event.getTemplateData());
    assertNotNull(event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(customerId, event.getCustomerId());
  }
}
