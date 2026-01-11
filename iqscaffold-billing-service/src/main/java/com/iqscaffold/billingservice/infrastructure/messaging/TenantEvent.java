package com.iqscaffold.billingservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Map;

/**
 * Event for tenant-related activities from User Service.
 * This is a mirror of the TenantEvent from the User Service.
 */
public class TenantEvent {

  private String eventId;
  private String eventType;
  private String tenantId;
  private String organizationName;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private Map<String, Object> metadata;

  public TenantEvent() {
  }

  public TenantEvent(
      final String eventId,
      final String eventType,
      final String tenantId,
      final String organizationName,
      final Instant timestamp,
      final Map<String, Object> metadata) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.tenantId = tenantId;
    this.organizationName = organizationName;
    this.timestamp = timestamp;
    this.metadata = metadata;
  }

  // Getters and setters

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

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
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
