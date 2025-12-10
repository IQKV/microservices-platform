package com.iqscaffold.userservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

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

  public UserEvent(final String eventId, final String eventType, final String userId, final String tenantId,
                   final String email, final Instant timestamp, final Map<String, Object> metadata) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserEvent userEvent = (UserEvent) o;
    return Objects.equals(eventId, userEvent.eventId)
           && Objects.equals(eventType, userEvent.eventType)
           && Objects.equals(userId, userEvent.userId)
           && Objects.equals(tenantId, userEvent.tenantId)
           && Objects.equals(email, userEvent.email)
           && Objects.equals(timestamp, userEvent.timestamp)
           && Objects.equals(metadata, userEvent.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventId, eventType, userId, tenantId, email, timestamp, metadata);
  }

  @Override
  public String toString() {
    return "UserEvent{"
           + "eventId='" + eventId + '\''
           + ", eventType='" + eventType + '\''
           + ", userId='" + userId + '\''
           + ", tenantId='" + tenantId + '\''
           + ", email='" + email + '\''
           + ", timestamp=" + timestamp
           + ", metadata=" + metadata
           + '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String eventId;
    private String eventType;
    private String userId;
    private String tenantId;
    private String email;
    private Instant timestamp;
    private Map<String, Object> metadata;

    public Builder eventId(String eventId) {
      this.eventId = eventId;
      return this;
    }

    public Builder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public UserEvent build() {
      return new UserEvent(eventId, eventType, userId, tenantId, email, timestamp, metadata);
    }
  }

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
