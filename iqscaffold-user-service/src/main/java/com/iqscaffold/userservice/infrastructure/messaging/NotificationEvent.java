package com.iqscaffold.userservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

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
  private String userId;

  public NotificationEvent() {
  }

  public NotificationEvent(String eventId, String notificationType, String recipientEmail,
                           String recipientName, String subject, String templateName,
                           Map<String, Object> templateData, Instant timestamp,
                           String tenantId, String userId) {
    this.eventId = eventId;
    this.notificationType = notificationType;
    this.recipientEmail = recipientEmail;
    this.recipientName = recipientName;
    this.subject = subject;
    this.templateName = templateName;
    this.templateData = templateData;
    this.timestamp = timestamp;
    this.tenantId = tenantId;
    this.userId = userId;
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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NotificationEvent that = (NotificationEvent) o;
    return Objects.equals(eventId, that.eventId)
        && Objects.equals(notificationType, that.notificationType)
        && Objects.equals(recipientEmail, that.recipientEmail)
        && Objects.equals(recipientName, that.recipientName)
        && Objects.equals(subject, that.subject)
        && Objects.equals(templateName, that.templateName)
        && Objects.equals(templateData, that.templateData)
        && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(tenantId, that.tenantId)
        && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventId, notificationType, recipientEmail, recipientName,
        subject, templateName, templateData, timestamp, tenantId, userId);
  }

  @Override
  public String toString() {
    return "NotificationEvent{"
        + "eventId='" + eventId + '\''
        + ", notificationType='" + notificationType + '\''
        + ", recipientEmail='" + recipientEmail + '\''
        + ", recipientName='" + recipientName + '\''
        + ", subject='" + subject + '\''
        + ", templateName='" + templateName + '\''
        + ", templateData=" + templateData
        + ", timestamp=" + timestamp
        + ", tenantId='" + tenantId + '\''
        + ", userId='" + userId + '\''
        + '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String eventId;
    private String notificationType;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String templateName;
    private Map<String, Object> templateData;
    private Instant timestamp;
    private String tenantId;
    private String userId;

    public Builder eventId(String eventId) {
      this.eventId = eventId;
      return this;
    }

    public Builder notificationType(String notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    public Builder recipientEmail(String recipientEmail) {
      this.recipientEmail = recipientEmail;
      return this;
    }

    public Builder recipientName(String recipientName) {
      this.recipientName = recipientName;
      return this;
    }

    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder templateName(String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder templateData(Map<String, Object> templateData) {
      this.templateData = templateData;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public NotificationEvent build() {
      return new NotificationEvent(eventId, notificationType, recipientEmail, recipientName,
          subject, templateName, templateData, timestamp, tenantId, userId);
    }
  }

  public static NotificationEvent emailNotification(
      String recipientEmail,
      String recipientName,
      String subject,
      String templateName,
      Map<String, Object> templateData,
      String tenantId,
      String userId) {
    return NotificationEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .notificationType("EMAIL")
        .recipientEmail(recipientEmail)
        .recipientName(recipientName)
        .subject(subject)
        .templateName(templateName)
        .templateData(templateData)
        .timestamp(Instant.now())
        .tenantId(tenantId)
        .userId(userId)
        .build();
  }
}
