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
 * Entity representing tenant-specific payment gateway configuration.
 * <p>
 * This entity stores encrypted gateway-specific settings (API keys, secrets, etc.)
 * per tenant, enabling multi-tenant payment gateway management. Each tenant can
 * configure multiple payment gateways and designate one as primary.
 * </p>
 * <p>
 * The {@code configData} field stores encrypted JSON containing gateway-specific
 * configuration (e.g., Stripe API key, PayPal client credentials).
 * </p>
 */
@Entity
@Table(name = "payment_gateway_config", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.admin.PaymentGatewayConfig")
public class PaymentGatewayConfig {

  @Id
  private UUID id;

  /**
   * Tenant identifier - links configuration to specific tenant.
   */
  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  /**
   * The payment gateway provider type.
   */
  @Column(name = "gateway_provider", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private PaymentGatewayProvider gatewayProvider;

  /**
   * Whether this gateway is currently active for the tenant.
   * Inactive gateways cannot be used for processing payments.
   */
  @Column(name = "is_active", nullable = false)
  private boolean isActive = false;

  /**
   * Whether this is the primary/default gateway for the tenant.
   * Only one gateway per tenant should be marked as primary.
   */
  @Column(name = "is_primary", nullable = false)
  private boolean isPrimary = false;

  /**
   * Gateway mode: 'test' or 'live'.
   * Determines whether to use sandbox/test or production credentials.
   */
  @Column(name = "mode", nullable = false, length = 20)
  private String mode = "test";

  /**
   * Encrypted gateway-specific configuration data stored as JSON.
   * Contains sensitive information like API keys, secrets, tokens.
   * Must be encrypted at rest using {@link com.iqscaffold.billingservice.security.GatewayConfigEncryptionService}.
   */
  @Column(name = "config_data", nullable = false, columnDefinition = "TEXT")
  private String configData;

  /**
   * Optional display name for this gateway configuration.
   */
  @Column(name = "display_name", length = 255)
  private String displayName;

  /**
   * Optional description or notes about this configuration.
   */
  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public PaymentGatewayConfig() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public PaymentGatewayProvider getGatewayProvider() {
    return gatewayProvider;
  }

  public void setGatewayProvider(PaymentGatewayProvider gatewayProvider) {
    this.gatewayProvider = gatewayProvider;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public boolean isPrimary() {
    return isPrimary;
  }

  public void setPrimary(boolean primary) {
    isPrimary = primary;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getConfigData() {
    return configData;
  }

  public void setConfigData(String configData) {
    this.configData = configData;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
