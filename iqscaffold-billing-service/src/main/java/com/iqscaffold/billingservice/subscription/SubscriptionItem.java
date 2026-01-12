package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Line item within a subscription.
 * <p>
 * Supports multiple plans/add-ons within a single subscription.
 * Stored in public schema.
 */
@Entity
@Table(name = "subscription_item", schema = "public")
public class SubscriptionItem {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_subscription_id", nullable = false)
  private TenantSubscription tenantSubscription;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id")
  private SubscriptionPlan plan;

  @Column(nullable = false)
  private Integer quantity = 1;

  @Column(name = "unit_amount", precision = 19, scale = 2)
  private BigDecimal unitAmount;

  @Column(name = "stripe_subscription_item_id")
  private String stripeSubscriptionItemId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SubscriptionItem() {
  }

  // Getters and setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public TenantSubscription getTenantSubscription() {
    return tenantSubscription;
  }

  public void setTenantSubscription(TenantSubscription tenantSubscription) {
    this.tenantSubscription = tenantSubscription;
  }

  public SubscriptionPlan getPlan() {
    return plan;
  }

  public void setPlan(SubscriptionPlan plan) {
    this.plan = plan;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitAmount() {
    return unitAmount;
  }

  public void setUnitAmount(BigDecimal unitAmount) {
    this.unitAmount = unitAmount;
  }

  public String getStripeSubscriptionItemId() {
    return stripeSubscriptionItemId;
  }

  public void setStripeSubscriptionItemId(String stripeSubscriptionItemId) {
    this.stripeSubscriptionItemId = stripeSubscriptionItemId;
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

  public BigDecimal getTotalAmount() {
    if (unitAmount == null) {
      return BigDecimal.ZERO;
    }
    return unitAmount.multiply(BigDecimal.valueOf(quantity));
  }
}
