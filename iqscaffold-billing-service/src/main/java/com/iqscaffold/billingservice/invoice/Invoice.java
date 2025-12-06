package com.iqscaffold.billingservice.invoice;

import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.subscription.Subscription;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Type;

/**
 * Invoice aggregate root entity representing a billing document for a subscription period.
 * 
 * <p>Invoices manage the billing lifecycle including line items, payment tracking,
 * and status transitions. Invoices are stored in tenant-scoped schemas for data isolation.
 * 
 * <p>This aggregate enforces the following invariants:
 * <ul>
 *   <li>Invoice number must be unique</li>
 *   <li>Invoice total must equal sum of line items plus tax</li>
 *   <li>Line items cannot be empty when invoice is finalized</li>
 *   <li>Paid invoices cannot be modified</li>
 *   <li>Voided invoices cannot be paid</li>
 *   <li>Period end must be after period start</li>
 *   <li>Due date must be after invoice creation</li>
 * </ul>
 * 
 * <p>All modifications to line items must go through this aggregate root
 * to ensure business rules and invariants are maintained.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id")
  private Subscription subscription;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(unique = true, nullable = false, length = 50)
  private String invoiceNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvoiceStatus status;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal subtotal;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal tax;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal total;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column
  private LocalDateTime periodStart;

  @Column
  private LocalDateTime periodEnd;

  @Column
  private LocalDateTime dueDate;

  @Column
  private LocalDateTime paidAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_method_id")
  private PaymentMethod paymentMethod;

  @Column(length = 255)
  private String providerInvoiceId;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb", nullable = false)
  private List<InvoiceLineItem> lineItems;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Default constructor for JPA.
   */
  protected Invoice() {
    // JPA requires a no-arg constructor
  }

  /**
   * Creates a new invoice with the specified attributes.
   * 
   * @param subscription the subscription this invoice is for
   * @param tenantId tenant identifier
   * @param invoiceNumber unique invoice number
   * @param currency currency code (e.g., USD, EUR)
   */
  private Invoice(
      Subscription subscription,
      UUID tenantId,
      String invoiceNumber,
      String currency
  ) {
    this.subscription = subscription;
    this.tenantId = tenantId;
    this.invoiceNumber = invoiceNumber;
    this.currency = currency;
    this.status = InvoiceStatus.DRAFT;
    this.lineItems = new ArrayList<>();
    this.metadata = new HashMap<>();
    this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    this.total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Factory method to create a new draft invoice.
   * 
   * @param subscription the subscription this invoice is for
   * @param tenantId tenant identifier
   * @param invoiceNumber unique invoice number
   * @param currency currency code
   * @param periodStart billing period start date
   * @param periodEnd billing period end date
   * @param dueDays number of days until payment is due
   * @return a new Invoice in DRAFT status
   */
  public static Invoice createDraft(
      Subscription subscription,
      UUID tenantId,
      String invoiceNumber,
      String currency,
      LocalDateTime periodStart,
      LocalDateTime periodEnd,
      int dueDays
  ) {
    validateSubscription(subscription);
    validateTenantId(tenantId);
    validateInvoiceNumber(invoiceNumber);
    validateCurrency(currency);
    validatePeriod(periodStart, periodEnd);
    validateDueDays(dueDays);

    var invoice = new Invoice(subscription, tenantId, invoiceNumber, currency);
    invoice.periodStart = periodStart;
    invoice.periodEnd = periodEnd;
    invoice.dueDate = LocalDateTime.now().plusDays(dueDays);

    return invoice;
  }

  /**
   * Adds a line item to the invoice.
   * Can only add line items to DRAFT invoices.
   * 
   * @param lineItem the line item to add
   * @throws IllegalStateException if invoice is not in DRAFT status
   * @throws IllegalArgumentException if line item is null
   */
  public void addLineItem(InvoiceLineItem lineItem) {
    if (!status.canModify()) {
      throw new IllegalStateException(
          "Cannot add line items to invoice in " + status + " status"
      );
    }

    if (lineItem == null) {
      throw new IllegalArgumentException("Line item cannot be null");
    }

    this.lineItems.add(lineItem);
    recalculateTotals();
  }

  /**
   * Removes a line item from the invoice by index.
   * Can only remove line items from DRAFT invoices.
   * 
   * @param index the index of the line item to remove
   * @throws IllegalStateException if invoice is not in DRAFT status
   * @throws IndexOutOfBoundsException if index is invalid
   */
  public void removeLineItem(int index) {
    if (!status.canModify()) {
      throw new IllegalStateException(
          "Cannot remove line items from invoice in " + status + " status"
      );
    }

    this.lineItems.remove(index);
    recalculateTotals();
  }

  /**
   * Clears all line items from the invoice.
   * Can only clear line items from DRAFT invoices.
   * 
   * @throws IllegalStateException if invoice is not in DRAFT status
   */
  public void clearLineItems() {
    if (!status.canModify()) {
      throw new IllegalStateException(
          "Cannot clear line items from invoice in " + status + " status"
      );
    }

    this.lineItems.clear();
    recalculateTotals();
  }

  /**
   * Sets the tax amount for the invoice.
   * Can only set tax on DRAFT invoices.
   * 
   * @param taxAmount the tax amount
   * @throws IllegalStateException if invoice is not in DRAFT status
   * @throws IllegalArgumentException if tax amount is negative
   */
  public void setTax(BigDecimal taxAmount) {
    if (!status.canModify()) {
      throw new IllegalStateException(
          "Cannot set tax on invoice in " + status + " status"
      );
    }

    if (taxAmount == null || taxAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Tax amount must be non-negative");
    }

    this.tax = taxAmount.setScale(2, RoundingMode.HALF_UP);
    recalculateTotals();
  }

  /**
   * Finalizes the invoice, making it ready for payment.
   * Transitions from DRAFT to OPEN status.
   * 
   * @throws IllegalStateException if invoice cannot be finalized
   */
  public void finalize() {
    if (!status.canFinalize()) {
      throw new IllegalStateException(
          "Cannot finalize invoice in " + status + " status"
      );
    }

    if (lineItems.isEmpty()) {
      throw new IllegalStateException(
          "Cannot finalize invoice with no line items"
      );
    }

    validateInvoiceTotals();
    transitionTo(InvoiceStatus.OPEN);
  }

  /**
   * Marks the invoice as paid.
   * Transitions from OPEN to PAID status.
   * 
   * @param paymentDate the date payment was received
   * @throws IllegalStateException if invoice cannot be paid
   */
  public void markAsPaid(LocalDateTime paymentDate) {
    if (!status.canPay()) {
      throw new IllegalStateException(
          "Cannot mark invoice as paid in " + status + " status"
      );
    }

    if (paymentDate == null) {
      throw new IllegalArgumentException("Payment date cannot be null");
    }

    this.paidAt = paymentDate;
    transitionTo(InvoiceStatus.PAID);
  }

  /**
   * Voids the invoice, preventing payment.
   * Can void DRAFT or OPEN invoices.
   * 
   * @param reason the reason for voiding
   * @throws IllegalStateException if invoice cannot be voided
   */
  public void voidInvoice(String reason) {
    if (!status.canVoid()) {
      throw new IllegalStateException(
          "Cannot void invoice in " + status + " status"
      );
    }

    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Void reason cannot be blank");
    }

    addMetadata("voidReason", reason);
    addMetadata("voidedAt", LocalDateTime.now().toString());
    transitionTo(InvoiceStatus.VOID);
  }

  /**
   * Marks the invoice as uncollectible after payment failures.
   * Transitions from OPEN to UNCOLLECTIBLE status.
   * 
   * @param reason the reason for marking uncollectible
   * @throws IllegalStateException if invoice is not OPEN
   */
  public void markAsUncollectible(String reason) {
    if (status != InvoiceStatus.OPEN) {
      throw new IllegalStateException(
          "Can only mark OPEN invoices as uncollectible"
      );
    }

    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Uncollectible reason cannot be blank");
    }

    addMetadata("uncollectibleReason", reason);
    addMetadata("markedUncollectibleAt", LocalDateTime.now().toString());
    transitionTo(InvoiceStatus.UNCOLLECTIBLE);
  }

  /**
   * Sets the payment method for this invoice.
   * 
   * @param paymentMethod the payment method to use
   */
  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  /**
   * Sets the provider invoice ID (e.g., Stripe invoice ID).
   * 
   * @param providerInvoiceId the provider's invoice identifier
   */
  public void setProviderInvoiceId(String providerInvoiceId) {
    this.providerInvoiceId = providerInvoiceId;
  }

  /**
   * Adds or updates metadata for the invoice.
   * 
   * @param key metadata key
   * @param value metadata value
   */
  public void addMetadata(String key, Object value) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, value);
  }

  /**
   * Removes metadata from the invoice.
   * 
   * @param key metadata key to remove
   */
  public void removeMetadata(String key) {
    if (this.metadata != null) {
      this.metadata.remove(key);
    }
  }

  /**
   * Checks if the invoice is overdue.
   * 
   * @return true if invoice is OPEN and past due date
   */
  public boolean isOverdue() {
    return status == InvoiceStatus.OPEN &&
           dueDate != null &&
           LocalDateTime.now().isAfter(dueDate);
  }

  /**
   * Checks if the invoice can be modified.
   * 
   * @return true if invoice is in DRAFT status
   */
  public boolean canModify() {
    return status.canModify();
  }

  /**
   * Gets the number of days until the invoice is due.
   * 
   * @return days until due, or 0 if already due or no due date
   */
  public long getDaysUntilDue() {
    if (dueDate == null || status != InvoiceStatus.OPEN) {
      return 0;
    }

    var now = LocalDateTime.now();
    if (now.isAfter(dueDate)) {
      return 0;
    }

    return java.time.Duration.between(now, dueDate).toDays();
  }

  /**
   * Gets an unmodifiable view of the line items.
   * 
   * @return unmodifiable list of line items
   */
  public List<InvoiceLineItem> getLineItems() {
    return Collections.unmodifiableList(lineItems);
  }

  /**
   * Gets the number of line items.
   * 
   * @return line item count
   */
  public int getLineItemCount() {
    return lineItems.size();
  }

  // Private helper methods

  private void transitionTo(InvoiceStatus newStatus) {
    validateStatusTransition(this.status, newStatus);
    this.status = newStatus;
  }

  private static void validateStatusTransition(InvoiceStatus from, InvoiceStatus to) {
    var isValid = switch (from) {
      case DRAFT -> to == InvoiceStatus.OPEN || to == InvoiceStatus.VOID;
      case OPEN -> to == InvoiceStatus.PAID || 
                   to == InvoiceStatus.VOID || 
                   to == InvoiceStatus.UNCOLLECTIBLE;
      case PAID, VOID, UNCOLLECTIBLE -> false; // Final states
    };

    if (!isValid) {
      throw new IllegalStateException(
          "Invalid status transition from " + from + " to " + to
      );
    }
  }

  private void recalculateTotals() {
    // Calculate subtotal from line items (excluding tax line items)
    this.subtotal = lineItems.stream()
        .filter(item -> item.type() != InvoiceLineItem.LineItemType.TAX)
        .map(InvoiceLineItem::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);

    // Calculate total (subtotal + tax)
    this.total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
  }

  private void validateInvoiceTotals() {
    // Verify that calculated totals match stored totals
    BigDecimal calculatedSubtotal = lineItems.stream()
        .filter(item -> item.type() != InvoiceLineItem.LineItemType.TAX)
        .map(InvoiceLineItem::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal calculatedTotal = calculatedSubtotal.add(tax)
        .setScale(2, RoundingMode.HALF_UP);

    if (subtotal.compareTo(calculatedSubtotal) != 0) {
      throw new IllegalStateException(
          "Invoice subtotal mismatch. Expected: " + calculatedSubtotal + 
          ", Got: " + subtotal
      );
    }

    if (total.compareTo(calculatedTotal) != 0) {
      throw new IllegalStateException(
          "Invoice total mismatch. Expected: " + calculatedTotal + 
          ", Got: " + total
      );
    }
  }

  private static void validateSubscription(Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }
  }

  private static void validateTenantId(UUID tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
  }

  private static void validateInvoiceNumber(String invoiceNumber) {
    if (invoiceNumber == null || invoiceNumber.isBlank()) {
      throw new IllegalArgumentException("Invoice number cannot be blank");
    }
  }

  private static void validateCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency cannot be blank");
    }
    if (currency.length() != 3) {
      throw new IllegalArgumentException("Currency must be a 3-letter code");
    }
  }

  private static void validatePeriod(LocalDateTime start, LocalDateTime end) {
    if (start == null) {
      throw new IllegalArgumentException("Period start cannot be null");
    }
    if (end == null) {
      throw new IllegalArgumentException("Period end cannot be null");
    }
    if (!end.isAfter(start)) {
      throw new IllegalArgumentException("Period end must be after period start");
    }
  }

  private static void validateDueDays(int dueDays) {
    if (dueDays < 0) {
      throw new IllegalArgumentException("Due days cannot be negative");
    }
  }

  @PrePersist
  protected void onCreate() {
    var now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Getters

  public Long getId() {
    return id;
  }

  public Subscription getSubscription() {
    return subscription;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public InvoiceStatus getStatus() {
    return status;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getTax() {
    return tax;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDateTime getPeriodStart() {
    return periodStart;
  }

  public LocalDateTime getPeriodEnd() {
    return periodEnd;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public String getProviderInvoiceId() {
    return providerInvoiceId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Invoice that)) {
      return false;
    }
    return Objects.equals(id, that.id) && 
           Objects.equals(invoiceNumber, that.invoiceNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, invoiceNumber);
  }

  @Override
  public String toString() {
    return "Invoice{" +
        "id=" + id +
        ", invoiceNumber='" + invoiceNumber + '\'' +
        ", status=" + status +
        ", total=" + total +
        ", currency='" + currency + '\'' +
        ", tenantId=" + tenantId +
        '}';
  }
}
