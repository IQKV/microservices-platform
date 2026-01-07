package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for notification requests
 */
public class NotificationEvent {

  private String eventId;
  private String notificationType;
  private String recipientEmail;
  private String recipientName;
  private String subject;
  private String templateName;
  private Map<String, Object> templateData;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private String tenantId;
  private String customerId;

  public NotificationEvent() {
  }

  public NotificationEvent(final String eventId, final String notificationType, final String recipientEmail,
                           final String recipientName, final String subject, final String templateName,
                           final Map<String, Object> templateData, final Instant timestamp,
                           final String tenantId, final String customerId) {
    this.eventId = eventId;
    this.notificationType = notificationType;
    this.recipientEmail = recipientEmail;
    this.recipientName = recipientName;
    this.subject = subject;
    this.templateName = templateName;
    this.templateData = templateData;
    this.timestamp = timestamp;
    this.tenantId = tenantId;
    this.customerId = customerId;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getNotificationType() {
    return notificationType;
  }

  public void setNotificationType(String notificationType) {
    this.notificationType = notificationType;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public void setRecipientEmail(String recipientEmail) {
    this.recipientEmail = recipientEmail;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public void setRecipientName(String recipientName) {
    this.recipientName = recipientName;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public Map<String, Object> getTemplateData() {
    return templateData;
  }

  public void setTemplateData(Map<String, Object> templateData) {
    this.templateData = templateData;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public static NotificationEvent emailNotification(
      String recipientEmail,
      String recipientName,
      String subject,
      String templateName,
      Map<String, Object> templateData,
      String tenantId,
      String customerId) {
    return new NotificationEvent(
        java.util.UUID.randomUUID().toString(),
        "EMAIL",
        recipientEmail,
        recipientName,
        subject,
        templateName,
        templateData,
        Instant.now(),
        tenantId,
        customerId
    );
  }
}
