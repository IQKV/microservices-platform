package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for user-related activities from User Service.
 * This is a mirror of the UserEvent from the User Service.
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

  public UserEvent(final String eventId, final String eventType, final String userId,
                   final String tenantId, final String email, final Instant timestamp,
                   final Map<String, Object> metadata) {
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
}
