package com.iqscaffold.billingservice.usage;

import com.iqscaffold.billingservice.subscription.Subscription;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Type;

/**
 * UsageRecord entity for tracking resource consumption.
 * 
 * <p>Tracks usage metrics for billing and quota enforcement purposes.
 * Each record represents a measurement of resource consumption for a specific
 * metric type within a billing period.
 * 
 * <p>This entity is stored in tenant-scoped schemas for data isolation.
 * 
 * @see MetricType
 * @see Subscription
 */
@Entity
@Table(name = "usage_records")
public class UsageRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "subscription_id", nullable = false)
  private Subscription subscription;

  @Column(nullable = false)
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MetricType metricType;

  @Column(nullable = false)
  private Long quantity;

  @Column(length = 50)
  private String unit;

  @Column(nullable = false)
  private LocalDateTime recordedAt;

  @Column(nullable = false)
  private LocalDateTime billingPeriodStart;

  @Column(nullable = false)
  private LocalDateTime billingPeriodEnd;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Default constructor for JPA.
   */
  protected UsageRecord() {
    // Required by JPA
  }

  /**
   * Creates a new usage record.
   *
   * @param subscription the subscription this usage belongs to
   * @param tenantId the tenant identifier
   * @param metricType the type of metric being tracked
   * @param quantity the amount of usage
   * @param unit the unit of measurement (optional)
   * @param recordedAt when the usage was recorded
   * @param billingPeriodStart start of the billing period
   * @param billingPeriodEnd end of the billing period
   */
  public UsageRecord(
      final Subscription subscription,
      final UUID tenantId,
      final MetricType metricType,
      final Long quantity,
      final String unit,
      final LocalDateTime recordedAt,
      final LocalDateTime billingPeriodStart,
      final LocalDateTime billingPeriodEnd) {
    this.subscription = subscription;
    this.tenantId = tenantId;
    this.metricType = metricType;
    this.quantity = quantity;
    this.unit = unit;
    this.recordedAt = recordedAt;
    this.billingPeriodStart = billingPeriodStart;
    this.billingPeriodEnd = billingPeriodEnd;
    this.createdAt = LocalDateTime.now();
  }

  // Getters and setters

  public Long getId() {
    return id;
  }

  public Subscription getSubscription() {
    return subscription;
  }

  public void setSubscription(final Subscription subscription) {
    this.subscription = subscription;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(final UUID tenantId) {
    this.tenantId = tenantId;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(final MetricType metricType) {
    this.metricType = metricType;
  }

  public Long getQuantity() {
    return quantity;
  }

  public void setQuantity(final Long quantity) {
    this.quantity = quantity;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(final String unit) {
    this.unit = unit;
  }

  public LocalDateTime getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(final LocalDateTime recordedAt) {
    this.recordedAt = recordedAt;
  }

  public LocalDateTime getBillingPeriodStart() {
    return billingPeriodStart;
  }

  public void setBillingPeriodStart(final LocalDateTime billingPeriodStart) {
    this.billingPeriodStart = billingPeriodStart;
  }

  public LocalDateTime getBillingPeriodEnd() {
    return billingPeriodEnd;
  }

  public void setBillingPeriodEnd(final LocalDateTime billingPeriodEnd) {
    this.billingPeriodEnd = billingPeriodEnd;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(final Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
