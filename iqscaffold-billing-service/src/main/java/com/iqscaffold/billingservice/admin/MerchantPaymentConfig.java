package com.iqscaffold.billingservice.admin;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entity representing merchant payment gateway configuration.
 * This abstraction allows support for multiple payment providers (Stripe, PayPal, etc.)
 * and links them to organizations in the user service.
 */
@Entity
@Table(name = "merchant_payment_config", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.admin.MerchantPaymentConfig")
public class MerchantPaymentConfig {
  @Id
  private UUID id;

  /**
   * The merchant/connected account ID in the payment gateway system.
   * For Stripe: Connect account ID (acct_xxx)
   * For PayPal: Merchant ID
   */
  @Column(name = "gateway_account_id")
  private String gatewayAccountId;

  /**
   * The payment gateway provider type.
   */
  @Column(name = "gateway_provider", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private PaymentGatewayProvider gatewayProvider;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "organization_id", nullable = false)
  private Long organizationId;

  /**
   * Whether the merchant can accept charges/payments.
   */
  @Column(name = "charges_enabled")
  private boolean chargesEnabled;

  /**
   * Whether the merchant can receive payouts.
   */
  @Column(name = "payouts_enabled")
  private boolean payoutsEnabled;

  /**
   * Platform application fee percentage (e.g., 2.5 for 2.5%).
   */
  @Column(name = "application_fee_percent", precision = 5, scale = 2)
  private java.math.BigDecimal applicationFeePercent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public MerchantPaymentConfig() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getGatewayAccountId() {
    return gatewayAccountId;
  }

  public void setGatewayAccountId(String gatewayAccountId) {
    this.gatewayAccountId = gatewayAccountId;
  }

  public PaymentGatewayProvider getGatewayProvider() {
    return gatewayProvider;
  }

  public void setGatewayProvider(PaymentGatewayProvider gatewayProvider) {
    this.gatewayProvider = gatewayProvider;
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
