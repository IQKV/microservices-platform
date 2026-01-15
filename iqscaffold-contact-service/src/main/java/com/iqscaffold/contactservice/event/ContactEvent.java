package com.iqscaffold.contactservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event published when a contact is created, updated, or deleted.
 * <p>
 * This event is published to the CRM events exchange and can be consumed
 * by other services that need to react to contact lifecycle changes.
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

  /**
   * Creates a new contact event.
   *
   * @param eventType  Type of event (CONTACT_CREATED, CONTACT_UPDATED, CONTACT_DELETED)
   * @param contactId  ID of the contact
   * @param tenantId   Tenant ID
   * @param metadata   Additional event metadata
   */
  public ContactEvent(
      final String eventType,
      final Long contactId,
      final String tenantId,
      final Map<String, Object> metadata) {
    this.eventId = UUID.randomUUID().toString();
    this.eventType = eventType;
    this.contactId = contactId;
    this.tenantId = tenantId;
    this.timestamp = Instant.now();
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
