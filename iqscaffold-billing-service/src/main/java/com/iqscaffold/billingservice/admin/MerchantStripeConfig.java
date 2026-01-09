package com.iqscaffold.billingservice.admin;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "merchant_stripe_config", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.admin.MerchantStripeConfig")
public class MerchantStripeConfig {
  @Id
  private UUID id;

  @Column(name = "stripe_account_id")
  private String stripeAccountId;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "organization_id", nullable = false)
  private Long organizationId;

  @Column(name = "charges_enabled")
  private boolean chargesEnabled;

  @Column(name = "payouts_enabled")
  private boolean payoutsEnabled;

  @Column(name = "application_fee_percent", precision = 5, scale = 2)
  private java.math.BigDecimal applicationFeePercent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public MerchantStripeConfig() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getStripeAccountId() {
    return stripeAccountId;
  }

  public void setStripeAccountId(String stripeAccountId) {
    this.stripeAccountId = stripeAccountId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public boolean isChargesEnabled() {
    return chargesEnabled;
  }

  public void setChargesEnabled(boolean chargesEnabled) {
    this.chargesEnabled = chargesEnabled;
  }

  public boolean isPayoutsEnabled() {
    return payoutsEnabled;
  }

  public void setPayoutsEnabled(boolean payoutsEnabled) {
    this.payoutsEnabled = payoutsEnabled;
  }

  public java.math.BigDecimal getApplicationFeePercent() {
    return applicationFeePercent;
  }

  public void setApplicationFeePercent(java.math.BigDecimal applicationFeePercent) {
    this.applicationFeePercent = applicationFeePercent;
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
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
