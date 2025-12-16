package com.iqscaffold.userservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Base event for user-related events
 */
public class UserEvent {

  private String eventId;
  private String eventType;
  private String userId;
  private String tenantId;
  private String email;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private Map<String, Object> metadata;

  public UserEvent() {
  }

  public UserEvent(String eventId, String eventType, String userId, String tenantId, 
                   String email, Instant timestamp, Map<String, Object> metadata) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.userId = userId;
    this.tenantId = tenantId;
    this.email = email;
    this.timestamp = timestamp;
    this.metadata = metadata;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public static UserEvent userCreated(String userId, String tenantId, String email) {
    return new UserEvent(
        java.util.UUID.randomUUID().toString(),
        "USER_CREATED",
        userId,
        tenantId,
        email,
        Instant.now(),
        null
    );
  }

  public static UserEvent userUpdated(String userId, String tenantId, String email) {
    return new UserEvent(
        java.util.UUID.randomUUID().toString(),
        "USER_UPDATED",
        userId,
        tenantId,
        email,
        Instant.now(),
        null
    );
  }

  public static UserEvent userDeleted(String userId, String tenantId, String email) {
    return new UserEvent(
        java.util.UUID.randomUUID().toString(),
        "USER_DELETED",
        userId,
        tenantId,
        email,
        Instant.now(),
        null
    );
  }

  public static UserEvent userVerified(String userId, String tenantId, String email) {
    return new UserEvent(
        java.util.UUID.randomUUID().toString(),
        "USER_VERIFIED",
        userId,
        tenantId,
        email,
        Instant.now(),
        null
    );
  }

  public static UserEvent passwordReset(String userId, String tenantId, String email) {
    return new UserEvent(
        java.util.UUID.randomUUID().toString(),
        "PASSWORD_RESET",
        userId,
        tenantId,
        email,
        Instant.now(),
        null
    );
  }
}
