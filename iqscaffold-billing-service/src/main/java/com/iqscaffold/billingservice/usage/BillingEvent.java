package com.iqscaffold.billingservice.usage;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
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
 * @see com.iqscaffold.billingservice.shared.BillingConstants.BillingEvents
 */
@Entity
@Table(name = "billing_events")
public class BillingEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 100)
  private String eventType;

  @Column(length = 100)
  private String aggregateType;

  @Column(length = 100)
  private String aggregateId;

  @Column(nullable = false)
  private UUID userId;

  @Column(length = 500)
  private String description;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> eventData;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt;

  /**
   * Default constructor for JPA.
   */
  protected BillingEvent() {
    // Required by JPA
  }

  /**
   * Creates a new billing event.
   *
   * @param tenantId the tenant identifier
   * @param eventType the type of event (e.g., SUBSCRIPTION_CREATED)
   * @param aggregateType the type of aggregate (e.g., Subscription, Invoice)
   * @param aggregateId the identifier of the aggregate
   * @param userId the user who triggered the event
   * @param description human-readable description of the event
   */
  public BillingEvent(
      final UUID tenantId,
      final String eventType,
      final String aggregateType,
      final String aggregateId,
      final UUID userId,
      final String description) {
    this.tenantId = tenantId;
    this.eventType = eventType;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.userId = userId;
    this.description = description;
    this.occurredAt = LocalDateTime.now();
  }

  /**
   * Creates a new billing event with event data.
   *
   * @param tenantId the tenant identifier
   * @param eventType the type of event
   * @param aggregateType the type of aggregate
   * @param aggregateId the identifier of the aggregate
   * @param userId the user who triggered the event
   * @param description human-readable description
   * @param eventData additional event-specific data
   */
  public BillingEvent(
      final UUID tenantId,
      final String eventType,
      final String aggregateType,
      final String aggregateId,
      final UUID userId,
      final String description,
      final Map<String, Object> eventData) {
    this(tenantId, eventType, aggregateType, aggregateId, userId, description);
    this.eventData = eventData;
  }

  // Getters and setters

  public Long getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(final UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(final String eventType) {
    this.eventType = eventType;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(final String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(final String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(final UUID userId) {
    this.userId = userId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public Map<String, Object> getEventData() {
    return eventData;
  }

  public void setEventData(final Map<String, Object> eventData) {
    this.eventData = eventData;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(final Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }
}
