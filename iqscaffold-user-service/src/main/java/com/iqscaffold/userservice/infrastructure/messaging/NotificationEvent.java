package com.iqscaffold.userservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for notification requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
