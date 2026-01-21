package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a tenant's subscription to a platform plan.
 * <p>
 * Stored in tenant-specific schema for maximum isolation.
 * Each tenant can have one active subscription at a time.
 * Tenant context is determined by the schema, not a column.
 *
 * <h3>Entity Graphs</h3>
 * <ul>
 *   <li><strong>subscription-with-plan</strong> - Eagerly loads subscription plan for billing operations</li>
 *   <li><strong>subscription-with-plan-and-features</strong> - Loads plan with features for feature access checks</li>
 * </ul>
 */
@Entity
@Table(name = "tenant_subscription")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.subscription.TenantSubscription")
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "subscription-with-plan",
        attributeNodes = {
            @NamedAttributeNode("plan")
        }
    ),
    @NamedEntityGraph(
        name = "subscription-with-plan-and-features",
        attributeNodes = {
            @NamedAttributeNode(value = "plan", subgraph = "plan-with-features")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "plan-with-features",
                attributeNodes = {
                    @NamedAttributeNode("planFeatures")
                }
            )
        }
    )
})
public class TenantSubscription {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private SubscriptionPlan plan;

  @Column(name = "organization_id")
  private Long organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private SubscriptionStatus status;

  @Column(name = "stripe_subscription_id")
  private String stripeSubscriptionId;

  @Column(name = "stripe_customer_id")
  private String stripeCustomerId;

  @Column(name = "current_period_start")
  private Instant currentPeriodStart;

  @Column(name = "current_period_end")
  private Instant currentPeriodEnd;

  @Column(name = "trial_end")
  private Instant trialEnd;

  @Column(name = "cancel_at")
  private Instant cancelAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @Column(name = "cancellation_reason", columnDefinition = "TEXT")
  private String cancellationReason;

  /**
   * Additional metadata (JSON format).
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public TenantSubscription() {
  }

  // Getters and setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public SubscriptionPlan getPlan() {
    return plan;
  }

  public void setPlan(SubscriptionPlan plan) {
    this.plan = plan;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus status) {
    this.status = status;
  }

  public String getStripeSubscriptionId() {
    return stripeSubscriptionId;
  }

  public void setStripeSubscriptionId(String stripeSubscriptionId) {
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  public String getStripeCustomerId() {
    return stripeCustomerId;
  }

  public void setStripeCustomerId(String stripeCustomerId) {
    this.stripeCustomerId = stripeCustomerId;
  }

  public Instant getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public void setCurrentPeriodStart(Instant currentPeriodStart) {
    this.currentPeriodStart = currentPeriodStart;
  }

  public Instant getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
    this.currentPeriodEnd = currentPeriodEnd;
  }

  public Instant getTrialEnd() {
    return trialEnd;
  }

  public void setTrialEnd(Instant trialEnd) {
    this.trialEnd = trialEnd;
  }

  public Instant getCancelAt() {
    return cancelAt;
  }

  public void setCancelAt(Instant cancelAt) {
    this.cancelAt = cancelAt;
  }

  public Instant getCanceledAt() {
    return canceledAt;
  }

  public void setCanceledAt(Instant canceledAt) {
    this.canceledAt = canceledAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  // Business methods

  public boolean isActive() {
    return status == SubscriptionStatus.ACTIVE;
  }

  public boolean isCanceled() {
    return status == SubscriptionStatus.CANCELED;
  }

  public boolean isInTrial() {
    return status == SubscriptionStatus.TRIALING;
  }

  public boolean isPastDue() {
    return status == SubscriptionStatus.PAST_DUE;
  }

  public boolean isPaused() {
    return status == SubscriptionStatus.PAUSED;
  }

  public boolean canRenew() {
    return isActive() || isInTrial();
  }

  public boolean isExpired() {
    return currentPeriodEnd != null && currentPeriodEnd.isBefore(Instant.now());
  }
}
