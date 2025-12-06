package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Type;

/**
 * Payment aggregate root entity.
 * Represents a payment transaction for an invoice.
 *
 * <p>Aggregate Invariants:
 * <ul>
 *   <li>Payment amount must be positive</li>
 *   <li>Refund amount cannot exceed original payment amount</li>
 *   <li>Payment must reference a valid invoice</li>
 *   <li>Succeeded payments cannot be modified</li>
 *   <li>Refunded payments cannot be refunded again</li>
 * </ul>
 *
 * <p>Boundary: All payment state changes must go through this aggregate root.
 */
@Entity
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentStatus status;

  @ManyToOne
  @JoinColumn(name = "payment_method_id")
  private PaymentMethod paymentMethod;

  @Column(name = "provider_payment_id", length = 255)
  private String providerPaymentId;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @Column(name = "refunded_amount", precision = 10, scale = 2)
  private BigDecimal refundedAmount;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Default constructor for JPA.
   */
  protected Payment() {
    this.metadata = new HashMap<>();
    this.refundedAmount = BigDecimal.ZERO;
  }

  /**
   * Creates a new payment.
   *
   * @param invoice the invoice being paid
   * @param tenantId the tenant ID
   * @param amount the payment amount
   * @param currency the currency code
   * @param paymentMethod the payment method used
   * @throws IllegalArgumentException if amount is not positive or invoice is null
   */
  public Payment(
    final Invoice invoice,
    final UUID tenantId,
    final BigDecimal amount,
    final String currency,
    final PaymentMethod paymentMethod
  ) {
    validateAmount(amount);
    if (invoice == null) {
      throw new IllegalArgumentException("Invoice cannot be null");
    }
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency cannot be null or empty");
    }

    this.invoice = invoice;
    this.tenantId = tenantId;
    this.amount = amount;
    this.currency = currency;
    this.paymentMethod = paymentMethod;
    this.status = PaymentStatus.PENDING;
    this.metadata = new HashMap<>();
    this.refundedAmount = BigDecimal.ZERO;
  }

  /**
   * Marks the payment as succeeded.
   *
   * @param providerPaymentId the payment ID from the payment provider
   * @throws IllegalStateException if payment is not in PENDING status
   */
  public void markAsSucceeded(final String providerPaymentId) {
    if (this.status != PaymentStatus.PENDING) {
      throw new IllegalStateException(
        "Can only mark PENDING payments as succeeded. Current status: " + this.status
      );
    }
    this.status = PaymentStatus.SUCCEEDED;
    this.providerPaymentId = providerPaymentId;
    this.failureReason = null;
  }

  /**
   * Marks the payment as failed.
   *
   * @param failureReason the reason for the failure
   * @throws IllegalStateException if payment is not in PENDING status
   */
  public void markAsFailed(final String failureReason) {
    if (this.status != PaymentStatus.PENDING) {
      throw new IllegalStateException(
        "Can only mark PENDING payments as failed. Current status: " + this.status
      );
    }
    this.status = PaymentStatus.FAILED;
    this.failureReason = failureReason;
  }

  /**
   * Processes a refund for this payment.
   *
   * @param refundAmount the amount to refund
   * @throws IllegalStateException if payment is not succeeded or already fully refunded
   * @throws IllegalArgumentException if refund amount exceeds available amount
   */
  public void processRefund(final BigDecimal refundAmount) {
    if (this.status != PaymentStatus.SUCCEEDED && this.status != PaymentStatus.REFUNDED) {
      throw new IllegalStateException(
        "Can only refund SUCCEEDED or partially REFUNDED payments. Current status: " + this.status
      );
    }

    validateAmount(refundAmount);

    final BigDecimal totalRefunded = this.refundedAmount.add(refundAmount);
    if (totalRefunded.compareTo(this.amount) > 0) {
      throw new IllegalArgumentException(
        String.format(
          "Refund amount (%s) would exceed original payment amount (%s). Already refunded: %s",
          refundAmount,
          this.amount,
          this.refundedAmount
        )
      );
    }

    this.refundedAmount = totalRefunded;
    this.status = PaymentStatus.REFUNDED;
  }

  /**
   * Gets the remaining refundable amount.
   *
   * @return the amount that can still be refunded
   */
  public BigDecimal getRefundableAmount() {
    if (this.status != PaymentStatus.SUCCEEDED && this.status != PaymentStatus.REFUNDED) {
      return BigDecimal.ZERO;
    }
    return this.amount.subtract(this.refundedAmount);
  }

  /**
   * Checks if the payment is fully refunded.
   *
   * @return true if the entire payment amount has been refunded
   */
  public boolean isFullyRefunded() {
    return this.refundedAmount.compareTo(this.amount) == 0;
  }

  /**
   * Checks if the payment is partially refunded.
   *
   * @return true if some but not all of the payment has been refunded
   */
  public boolean isPartiallyRefunded() {
    return (
      this.refundedAmount.compareTo(BigDecimal.ZERO) > 0 &&
      this.refundedAmount.compareTo(this.amount) < 0
    );
  }

  /**
   * Gets the payment status message using switch expression.
   *
   * @return a human-readable status message
   */
  public String getStatusMessage() {
    return switch (this.status) {
      case PENDING -> "Payment is being processed";
      case SUCCEEDED -> "Payment completed successfully";
      case FAILED -> "Payment failed: " + (failureReason != null ? failureReason : "Unknown reason");
      case REFUNDED -> isFullyRefunded()
        ? "Payment fully refunded"
        : String.format("Payment partially refunded (%s of %s)", refundedAmount, amount);
    };
  }

  /**
   * Adds metadata to the payment.
   *
   * @param key the metadata key
   * @param value the metadata value
   */
  public void addMetadata(final String key, final Object value) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, value);
  }

  /**
   * Validates that the amount is positive.
   *
   * @param amount the amount to validate
   * @throws IllegalArgumentException if amount is null or not positive
   */
  private void validateAmount(final BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be positive. Got: " + amount);
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

  public Invoice getInvoice() {
    return invoice;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public String getProviderPaymentId() {
    return providerPaymentId;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public BigDecimal getRefundedAmount() {
    return refundedAmount;
  }

  public Map<String, Object> getMetadata() {
    return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
