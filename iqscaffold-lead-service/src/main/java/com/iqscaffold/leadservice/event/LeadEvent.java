package com.iqscaffold.leadservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for lead lifecycle events published by Lead Service.
 * <p>
 * This event is published when leads are created, updated, deleted, or converted.
 * Other services (e.g., Pipeline Service) can subscribe to these events to
 * react to lead changes.
 */
public class LeadEvent {

  private String eventId;
  private String eventType;
  private Long leadId;
  private String tenantId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private Map<String, Object> metadata;

  public LeadEvent() {
  }

  /**
   * Creates a new LeadEvent with auto-generated event ID and current timestamp.
   *
   * @param eventType The type of event (e.g., "LEAD_CREATED", "LEAD_DELETED")
   * @param leadId    The ID of the lead
   * @param tenantId  The tenant ID
   * @param metadata  Additional event metadata
   */
  public LeadEvent(
      final String eventType,
      final Long leadId,
      final String tenantId,
      final Map<String, Object> metadata) {
    this.eventId = UUID.randomUUID().toString();
    this.eventType = eventType;
    this.leadId = leadId;
    this.tenantId = tenantId;
    this.timestamp = Instant.now();
    this.metadata = metadata;
  }

  /**
   * Creates a new LeadEvent with all fields specified.
   *
   * @param eventId   The event ID
   * @param eventType The type of event
   * @param leadId    The ID of the lead
   * @param tenantId  The tenant ID
   * @param timestamp The event timestamp
   * @param metadata  Additional event metadata
   */
  public LeadEvent(
      final String eventId,
      final String eventType,
      final Long leadId,
      final String tenantId,
      final Instant timestamp,
      final Map<String, Object> metadata) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.leadId = leadId;
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

  public Long getLeadId() {
    return leadId;
  }

  public void setLeadId(final Long leadId) {
    this.leadId = leadId;
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
    return "LeadEvent{" +
           "eventId='" + eventId + '\'' +
           ", eventType='" + eventType + '\'' +
           ", leadId=" + leadId +
           ", tenantId='" + tenantId + '\'' +
           ", timestamp=" + timestamp +
           '}';
  }
}
