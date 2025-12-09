package com.iqscaffold.billingservice.paymentmethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * PaymentMethod aggregate root entity.
 * Represents a stored payment method for a tenant.
 *
 * <p>Aggregate Invariants:
 * <ul>
 *   <li>Only one default payment method per tenant</li>
 *   <li>Expiry date must be in the future for active methods</li>
 *   <li>Provider payment method ID must be unique</li>
 *   <li>Full card numbers are never stored (PCI DSS compliance)</li>
 *   <li>Only last 4 digits and brand are stored for display</li>
 * </ul>
 *
 * <p>Boundary: All payment method state changes must go through this aggregate root.
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentMethodType type;

  @Column(name = "provider_payment_method_id", nullable = false, unique = true, length = 255)
  private String providerPaymentMethodId;

  @Column(length = 4)
  private String last4;

  @Column(length = 50)
  private String brand;

  @Column(name = "expiry_month")
  private Integer expiryMonth;

  @Column(name = "expiry_year")
  private Integer expiryYear;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault;

  @Column(nullable = false)
  private Boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "provider_customer_id", length = 255)
  private String providerCustomerId;

  /**
   * Default constructor for JPA.
   */
  protected PaymentMethod() {
    this.isDefault = false;
    this.active = true;
  }

  /**
   * Creates a new payment method.
   *
   * @param tenantId                the tenant ID
   * @param userId                  the user ID
   * @param type                    the payment method type
   * @param providerPaymentMethodId the payment method ID from the provider
   * @throws IllegalArgumentException if required fields are null or invalid
   */
  public PaymentMethod(
      final UUID tenantId,
      final UUID userId,
      final PaymentMethodType type,
      final String providerPaymentMethodId
  ) {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Payment method type cannot be null");
    }
    if (providerPaymentMethodId == null || providerPaymentMethodId.isBlank()) {
      throw new IllegalArgumentException("Provider payment method ID cannot be null or empty");
    }

    this.tenantId = tenantId;
    this.userId = userId;
    this.type = type;
    this.providerPaymentMethodId = providerPaymentMethodId;
    this.isDefault = false;
    this.active = true;
  }

  /**
   * Sets card details for display purposes.
   * Note: Full card numbers are NEVER stored (PCI DSS compliance).
   *
   * @param last4       the last 4 digits of the card
   * @param brand       the card brand (e.g., "Visa", "Mastercard")
   * @param expiryMonth the expiry month (1-12)
   * @param expiryYear  the expiry year (4 digits)
   * @throws IllegalArgumentException if expiry date is invalid or in the past
   */
  public void setCardDetails(
      final String last4,
      final String brand,
      final Integer expiryMonth,
      final Integer expiryYear
  ) {
    if (this.type != PaymentMethodType.CARD) {
      throw new IllegalStateException("Can only set card details for CARD payment methods");
    }

    validateExpiryDate(expiryMonth, expiryYear);

    this.last4 = last4;
    this.brand = brand;
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
  }

  /**
   * Marks this payment method as the default for the tenant.
   * Note: The caller is responsible for ensuring only one default exists per tenant.
   */
  public void markAsDefault() {
    if (!this.active) {
      throw new IllegalStateException("Cannot set inactive payment method as default");
    }
    this.isDefault = true;
  }

  /**
   * Removes the default status from this payment method.
   */
  public void removeDefaultStatus() {
    this.isDefault = false;
  }

  /**
   * Deactivates this payment method.
   *
   * @throws IllegalStateException if this is the default payment method
   */
  public void deactivate() {
    if (this.isDefault) {
      throw new IllegalStateException(
          "Cannot deactivate default payment method. Set another payment method as default first."
      );
    }
    this.active = false;
  }

  /**
   * Reactivates this payment method.
   */
  public void reactivate() {
    if (this.type == PaymentMethodType.CARD) {
      validateExpiryDate(this.expiryMonth, this.expiryYear);
    }
    this.active = true;
  }

  /**
   * Checks if the payment method is expired.
   *
   * @return true if the payment method has expired
   */
  public boolean isExpired() {
    if (this.type != PaymentMethodType.CARD || this.expiryMonth == null || this.expiryYear == null) {
      return false;
    }

    final YearMonth expiry = YearMonth.of(this.expiryYear, this.expiryMonth);
    final YearMonth now = YearMonth.now();
    return expiry.isBefore(now);
  }

  /**
   * Gets the display name for this payment method.
   *
   * @return a human-readable display name
   */
  public String getDisplayName() {
    return switch (this.type) {
      case CARD -> {
        if (brand != null && last4 != null) {
          yield String.format("%s ending in %s", brand, last4);
        }
        yield "Card ending in " + (last4 != null ? last4 : "****");
      }
      case BANK_ACCOUNT -> "Bank Account ending in " + (last4 != null ? last4 : "****");
      case PAYPAL -> "PayPal";
    };
  }

  /**
   * Gets the status message using switch expression.
   *
   * @return a human-readable status message
   */
  public String getStatusMessage() {
    if (!this.active) {
      return "Inactive";
    }
    if (isExpired()) {
      return "Expired";
    }
    if (this.isDefault) {
      return "Default payment method";
    }
    return "Active";
  }

  /**
   * Validates the expiry date.
   *
   * @param month the expiry month (1-12)
   * @param year  the expiry year (4 digits)
   * @throws IllegalArgumentException if expiry date is invalid or in the past
   */
  private void validateExpiryDate(final Integer month, final Integer year) {
    if (month == null || year == null) {
      throw new IllegalArgumentException("Expiry month and year cannot be null");
    }
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Expiry month must be between 1 and 12. Got: " + month);
    }
    if (year < 2000 || year > 2100) {
      throw new IllegalArgumentException("Expiry year must be between 2000 and 2100. Got: " + year);
    }

    final YearMonth expiry = YearMonth.of(year, month);
    final YearMonth now = YearMonth.now();
    if (expiry.isBefore(now)) {
      throw new IllegalArgumentException(
          String.format("Expiry date %s/%s is in the past", month, year)
      );
    }
  }

  @PrePersist
  protected void onCreate() {
    final var now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  // Getters
  public Long getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getUserId() {
    return userId;
  }

  public PaymentMethodType getType() {
    return type;
  }

  public String getProviderPaymentMethodId() {
    return providerPaymentMethodId;
  }

  public String getLast4() {
    return last4;
  }

  public String getBrand() {
    return brand;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public Boolean getActive() {
    return active;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public String getProviderCustomerId() {
    return providerCustomerId;
  }

  // Setters for GDPR compliance (anonymization)
  public void setLast4(String last4) {
    this.last4 = last4;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public void setProviderCustomerId(String providerCustomerId) {
    this.providerCustomerId = providerCustomerId;
  }

  public void setProviderPaymentMethodId(String providerPaymentMethodId) {
    this.providerPaymentMethodId = providerPaymentMethodId;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public boolean isDefault() {
    return isDefault != null && isDefault;
  }
}
