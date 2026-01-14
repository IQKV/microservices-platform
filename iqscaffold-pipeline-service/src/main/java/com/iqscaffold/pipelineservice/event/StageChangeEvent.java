package com.iqscaffold.pipelineservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a lead moves between pipeline stages.
 */
public class StageChangeEvent {

  private String eventId;
  private String eventType;
  private Long leadId;
  private Long oldStageId;
  private Long newStageId;
  private String tenantId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  public StageChangeEvent() {
  }

  public StageChangeEvent(
      final Long leadId,
      final Long oldStageId,
      final Long newStageId,
      final String tenantId) {
    this.eventId = UUID.randomUUID().toString();
    this.eventType = "STAGE_CHANGED";
    this.leadId = leadId;
    this.oldStageId = oldStageId;
    this.newStageId = newStageId;
    this.tenantId = tenantId;
    this.timestamp = Instant.now();
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

  public Long getOldStageId() {
    return oldStageId;
  }

  public void setOldStageId(final Long oldStageId) {
    this.oldStageId = oldStageId;
  }

  public Long getNewStageId() {
    return newStageId;
  }

  public void setNewStageId(final Long newStageId) {
    this.newStageId = newStageId;
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

  @Override
  public String toString() {
    return "StageChangeEvent{" +
           "eventId='" + eventId + '\'' +
           ", eventType='" + eventType + '\'' +
           ", leadId=" + leadId +
           ", oldStageId=" + oldStageId +
           ", newStageId=" + newStageId +
           ", tenantId='" + tenantId + '\'' +
           ", timestamp=" + timestamp +
           '}';
  }
}
