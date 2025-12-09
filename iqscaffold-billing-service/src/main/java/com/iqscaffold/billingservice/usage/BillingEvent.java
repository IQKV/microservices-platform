package com.iqscaffold.billingservice.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

/**
 * BillingEvent entity for audit trail and event tracking.
 *
 * <p>Records all significant billing-related events for audit, compliance,
 * and debugging purposes. Events are immutable once created and provide
 * a complete history of billing operations.
 *
 * <p>This entity is stored in tenant-scoped schemas for data isolation.
 *
 * <p>Event types include:
 * <ul>
 *   <li>Subscription lifecycle events (created, upgraded, canceled, etc.)</li>
 *   <li>Payment events (succeeded, failed, refunded)</li>
 *   <li>Invoice events (generated, paid, voided)</li>
 *   <li>Usage events (recorded, quota exceeded)</li>
 *   <li>Trial events (started, ending, ended, converted)</li>
 * </ul>
 *
 * <p><strong>Immutability:</strong> This entity is marked as immutable to satisfy
 * audit requirements (REQ-SEC-012). Once created, audit records cannot be modified
 * or deleted, ensuring data integrity for compliance and security investigations.
 *
 * @see com.iqscaffold.billingservice.shared.BillingConstants.BillingEvents
 */
@Entity
@Table(name = "billing_events")
@Immutable
public class BillingEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  @Column(name = "entity_type", nullable = false, length = 50)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(length = 500)
  private String description;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> changes;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  private String userAgent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Default constructor for JPA.
   */
  protected BillingEvent() {
    // Required by JPA
  }

  /**
   * Creates a new billing event.
   *
   * @param tenantId    the tenant identifier
   * @param userId      the user who triggered the event (may be null for system events)
   * @param eventType   the type of event (e.g., SUBSCRIPTION_CREATED)
   * @param entityType  the type of entity (e.g., SUBSCRIPTION, INVOICE)
   * @param entityId    the identifier of the entity
   * @param description human-readable description of the event
   * @param changes     map of changes made (for audit trail)
   * @param ipAddress   the IP address of the request
   * @param userAgent   the user agent of the request
   */
  public BillingEvent(
      final String tenantId,
      final Long userId,
      final String eventType,
      final String entityType,
      final Long entityId,
      final String description,
      final Map<String, Object> changes,
      final String ipAddress,
      final String userAgent) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.eventType = eventType;
    this.entityType = entityType;
    this.entityId = entityId;
    this.description = description;
    this.changes = changes;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.createdAt = LocalDateTime.now();
  }

  // Getters only - no setters for immutability

  public Long getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public Long getUserId() {
    return userId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getDescription() {
    return description;
  }

  public Map<String, Object> getChanges() {
    return changes;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
