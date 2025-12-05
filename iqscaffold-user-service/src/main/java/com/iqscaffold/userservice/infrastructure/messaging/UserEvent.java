package com.iqscaffold.userservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base event for user-related events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

  private String eventId;
  private String eventType;
  private String userId;
  private String tenantId;
  private String email;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private Map<String, Object> metadata;

  public static UserEvent userCreated(String userId, String tenantId, String email) {
    return UserEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("USER_CREATED")
        .userId(userId)
        .tenantId(tenantId)
        .email(email)
        .timestamp(Instant.now())
        .build();
  }

  public static UserEvent userUpdated(String userId, String tenantId, String email) {
    return UserEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("USER_UPDATED")
        .userId(userId)
        .tenantId(tenantId)
        .email(email)
        .timestamp(Instant.now())
        .build();
  }

  public static UserEvent userDeleted(String userId, String tenantId, String email) {
    return UserEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("USER_DELETED")
        .userId(userId)
        .tenantId(tenantId)
        .email(email)
        .timestamp(Instant.now())
        .build();
  }

  public static UserEvent userVerified(String userId, String tenantId, String email) {
    return UserEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("USER_VERIFIED")
        .userId(userId)
        .tenantId(tenantId)
        .email(email)
        .timestamp(Instant.now())
        .build();
  }

  public static UserEvent passwordReset(String userId, String tenantId, String email) {
    return UserEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("PASSWORD_RESET")
        .userId(userId)
        .tenantId(tenantId)
        .email(email)
        .timestamp(Instant.now())
        .build();
  }
}
