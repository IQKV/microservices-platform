package com.iqscaffold.pipelineservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Map;

/**
 * Event for lead lifecycle events consumed by Pipeline Service.
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
