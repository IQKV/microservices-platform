package com.iqscaffold.leadservice.event;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for contact lifecycle events consumed by Lead Service.
 * <p>
 * This event is published by the Contact Service when contacts are
 * created, updated, or deleted. The Lead Service consumes these events
 * to track lead conversions and maintain data consistency.
 */
public class ContactEvent {

  private String eventId;
  private String eventType;
  private Long contactId;
  private String tenantId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private Map<String, Object> metadata;

  public ContactEvent() {
  }

  public ContactEvent(
      final String eventId,
      final String eventType,
      final Long contactId,
      final String tenantId,
      final Instant timestamp,
      final Map<String, Object> metadata) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.contactId = contactId;
    this.tenantId = tenantId;
    this.timestamp = timestamp;
    this.metadata = metadata;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(final String eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(final String eventType) {
    this.eventType = eventType;
  }

  public Long getContactId() {
    return contactId;
  }

  public void setContactId(final Long contactId) {
    this.contactId = contactId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(final String tenantId) {
    this.tenantId = tenantId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(final Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @Override
  public String toString() {
    return "ContactEvent{" +
           "eventId='" + eventId + '\'' +
           ", eventType='" + eventType + '\'' +
           ", contactId=" + contactId +
           ", tenantId='" + tenantId + '\'' +
           ", timestamp=" + timestamp +
           '}';
  }
}
