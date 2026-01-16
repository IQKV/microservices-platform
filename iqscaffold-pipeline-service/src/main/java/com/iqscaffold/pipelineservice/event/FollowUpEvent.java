package com.iqscaffold.pipelineservice.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for follow-up lifecycle events published by Pipeline Service.
 * <p>
 * This event is published when follow-ups are scheduled or completed.
 * Other services can subscribe to these events for notifications,
 * analytics, or integration with external systems.
 */
public class FollowUpEvent {

  private String eventId;
  private String eventType;
  private Long followUpId;
  private Long leadId;
  private String tenantId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private Map<String, Object> metadata;

  public FollowUpEvent() {
  }

  /**
   * Creates a new FollowUpEvent with auto-generated event ID and current timestamp.
   *
   * @param eventType  The type of event (e.g., "FOLLOWUP_SCHEDULED", "FOLLOWUP_COMPLETED")
   * @param followUpId The ID of the follow-up
   * @param leadId     The ID of the associated lead
   * @param tenantId   The tenant ID
   * @param metadata   Additional event metadata
   */
  public FollowUpEvent(
      final String eventType,
      final Long followUpId,
      final Long leadId,
      final String tenantId,
      final Map<String, Object> metadata) {
    this.eventId = UUID.randomUUID().toString();
    this.eventType = eventType;
    this.followUpId = followUpId;
    this.leadId = leadId;
    this.tenantId = tenantId;
    this.timestamp = Instant.now();
    this.metadata = metadata;
  }

  /**
   * Creates a new FollowUpEvent with all fields specified.
   *
   * @param eventId    The event ID
   * @param eventType  The type of event
   * @param followUpId The ID of the follow-up
   * @param leadId     The ID of the associated lead
   * @param tenantId   The tenant ID
   * @param timestamp  The event timestamp
   * @param metadata   Additional event metadata
   */
  public FollowUpEvent(
      final String eventId,
      final String eventType,
      final Long followUpId,
      final Long leadId,
      final String tenantId,
      final Instant timestamp,
      final Map<String, Object> metadata) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.followUpId = followUpId;
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

  public Long getFollowUpId() {
    return followUpId;
  }

  public void setFollowUpId(final Long followUpId) {
    this.followUpId = followUpId;
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
    return "FollowUpEvent{" +
           "eventId='" + eventId + '\'' +
           ", eventType='" + eventType + '\'' +
           ", followUpId=" + followUpId +
           ", leadId=" + leadId +
           ", tenantId='" + tenantId + '\'' +
           ", timestamp=" + timestamp +
           '}';
  }
}
