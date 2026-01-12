package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Subscription invoice entity.
 * <p>
 * Tracks billing invoices generated for tenant subscriptions.
 * Stored in public schema.
 */
@Entity
@Table(name = "subscription_invoice", schema = "public")
public class SubscriptionInvoice {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_subscription_id", nullable = false)
  private TenantSubscription tenantSubscription;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "invoice_number", length = 100)
  private String invoiceNumber;

  @Column(name = "amount_due", nullable = false, precision = 19, scale = 2)
  private BigDecimal amountDue;

  @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
  private BigDecimal amountPaid = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private InvoiceStatus status;

  @Column(name = "due_date")
  private Instant dueDate;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "stripe_invoice_id")
  private String stripeInvoiceId;

  @Column(name = "hosted_invoice_url", length = 500)
  private String hostedInvoiceUrl;

  @Column(name = "invoice_pdf_url", length = 500)
  private String invoicePdfUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SubscriptionInvoice() {
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

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public BigDecimal getAmountDue() {
    return amountDue;
  }

  public void setAmountDue(BigDecimal amountDue) {
    this.amountDue = amountDue;
  }

  public BigDecimal getAmountPaid() {
    return amountPaid;
  }

  public void setAmountPaid(BigDecimal amountPaid) {
    this.amountPaid = amountPaid;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public InvoiceStatus getStatus() {
    return status;
  }

  public void setStatus(InvoiceStatus status) {
    this.status = status;
  }

  public Instant getDueDate() {
    return dueDate;
  }

  public void setDueDate(Instant dueDate) {
    this.dueDate = dueDate;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(Instant paidAt) {
    this.paidAt = paidAt;
  }

  public String getStripeInvoiceId() {
    return stripeInvoiceId;
  }

  public void setStripeInvoiceId(String stripeInvoiceId) {
    this.stripeInvoiceId = stripeInvoiceId;
  }

  public String getHostedInvoiceUrl() {
    return hostedInvoiceUrl;
  }

  public void setHostedInvoiceUrl(String hostedInvoiceUrl) {
    this.hostedInvoiceUrl = hostedInvoiceUrl;
  }

  public String getInvoicePdfUrl() {
    return invoicePdfUrl;
  }

  public void setInvoicePdfUrl(String invoicePdfUrl) {
    this.invoicePdfUrl = invoicePdfUrl;
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

  public boolean isPaid() {
    return status == InvoiceStatus.PAID;
  }

  public boolean isOpen() {
    return status == InvoiceStatus.OPEN;
  }

  public boolean isOverdue() {
    return isOpen() && dueDate != null && dueDate.isBefore(Instant.now());
  }

  public BigDecimal getAmountRemaining() {
    return amountDue.subtract(amountPaid);
  }
}
