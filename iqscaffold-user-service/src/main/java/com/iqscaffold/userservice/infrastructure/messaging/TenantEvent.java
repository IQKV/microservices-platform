package com.iqscaffold.userservice.infrastructure.messaging;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for tenant lifecycle activities.
 * Published when tenants are created, updated, deleted, or have status changes.
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

  /**
   * Create a tenant created event.
   */
  public static TenantEvent tenantCreated(String tenantId, String organizationName, Map<String, Object> additionalMetadata) {
    var metadata = new HashMap<String, Object>();
    if (additionalMetadata != null) {
      metadata.putAll(additionalMetadata);
    }
    
    return new TenantEvent(
        UUID.randomUUID().toString(),
        "TENANT_CREATED",
        tenantId,
        organizationName,
        Instant.now(),
        metadata
    );
  }

  /**
   * Create a tenant updated event.
   */
  public static TenantEvent tenantUpdated(String tenantId, String organizationName) {
    return new TenantEvent(
        UUID.randomUUID().toString(),
        "TENANT_UPDATED",
        tenantId,
        organizationName,
        Instant.now(),
        new HashMap<>()
    );
  }

  /**
   * Create a tenant deleted event.
   */
  public static TenantEvent tenantDeleted(String tenantId, String organizationName) {
    return new TenantEvent(
        UUID.randomUUID().toString(),
        "TENANT_DELETED",
        tenantId,
        organizationName,
        Instant.now(),
        new HashMap<>()
    );
  }

  /**
   * Create a tenant suspended event.
   */
  public static TenantEvent tenantSuspended(String tenantId, String organizationName, String reason, String suspendedBy) {
    var metadata = new HashMap<String, Object>();
    metadata.put("reason", reason);
    metadata.put("suspendedBy", suspendedBy);
    
    return new TenantEvent(
        UUID.randomUUID().toString(),
        "TENANT_SUSPENDED",
        tenantId,
        organizationName,
        Instant.now(),
        metadata
    );
  }

  /**
   * Create a tenant archived event.
   */
  public static TenantEvent tenantArchived(String tenantId, String organizationName, String reason, String archivedBy) {
    var metadata = new HashMap<String, Object>();
    metadata.put("reason", reason);
    metadata.put("archivedBy", archivedBy);
    
    return new TenantEvent(
        UUID.randomUUID().toString(),
        "TENANT_ARCHIVED",
        tenantId,
        organizationName,
        Instant.now(),
        metadata
    );
  }

  /**
   * Create a tenant restored event.
   */
  public static TenantEvent tenantRestored(String tenantId, String organizationName, String restoredBy) {
    var metadata = new HashMap<String, Object>();
    metadata.put("restoredBy", restoredBy);
    
    return new TenantEvent(
        UUID.randomUUID().toString(),
        "TENANT_RESTORED",
        tenantId,
        organizationName,
        Instant.now(),
        metadata
    );
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
