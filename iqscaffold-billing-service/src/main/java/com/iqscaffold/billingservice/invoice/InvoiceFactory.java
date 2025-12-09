package com.iqscaffold.billingservice.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.iqscaffold.billingservice.billing.ProrationResult;
import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.springframework.stereotype.Component;

/**
 * Factory for creating Invoice aggregates with complex business logic.
 *
 * <p>This factory encapsulates the complex logic required to create invoices
 * for various scenarios (subscription billing, proration, usage charges) while
 * ensuring all business rules and invariants are satisfied before object creation.
 *
 * <p>The factory handles:
 * <ul>
 *   <li>Invoice number generation following configured format</li>
 *   <li>Line item validation and total calculation</li>
 *   <li>Period and due date calculation</li>
 *   <li>Currency and tenant validation</li>
 *   <li>All invoice invariants</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a subscription invoice
 * List<InvoiceLineItem> lineItems = List.of(
 *     InvoiceLineItem.subscriptionFee("Pro Plan - Monthly", new BigDecimal("29.99"))
 * );
 * Invoice invoice = invoiceFactory.createSubscriptionInvoice(
 *     subscription, lineItems, periodStart, periodEnd
 * );
 *
 * // Create a proration invoice
 * ProrationResult proration = prorationCalculator.calculate(...);
 * Invoice prorationInvoice = invoiceFactory.createProrationInvoice(
 *     subscription, proration
 * );
 * }</pre>
 *
 * <p>Design Rationale: Factories ensure that complex objects are created in a
 * valid state with all invariants satisfied. They centralize creation logic,
 * making it testable and maintainable.
 *
 * @see Invoice
 * @see InvoiceLineItem
 * @see ProrationResult
 */
@Component
public class InvoiceFactory {

  private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMM");

  private final BillingProperties billingProperties;

  /**
   * Constructs a new InvoiceFactory.
   *
   * @param billingProperties billing configuration properties
   */
  public InvoiceFactory(BillingProperties billingProperties) {
    this.billingProperties = billingProperties;
  }

  /**
   * Creates a new subscription invoice for a regular billing period.
   *
   * <p>This method performs the following validations:
   * <ul>
   *   <li>Validates subscription, line items, and period dates</li>
   *   <li>Ensures line items are not empty</li>
   *   <li>Generates unique invoice number</li>
   *   <li>Calculates totals from line items</li>
   *   <li>Sets due date based on configuration</li>
   *   <li>Ensures all invoice invariants are satisfied</li>
   * </ul>
   *
   * <p>The created invoice will be in DRAFT status and must be finalized
   * before it can be paid.
   *
   * @param subscription the subscription this invoice is for
   * @param lineItems    the line items to include on the invoice
   * @param periodStart  the billing period start date
   * @param periodEnd    the billing period end date
   * @return a new Invoice in DRAFT status
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws IllegalStateException    if line items are empty
   */
  public Invoice createSubscriptionInvoice(
      Subscription subscription,
      List<InvoiceLineItem> lineItems,
      LocalDateTime periodStart,
      LocalDateTime periodEnd
  ) {
    // Validate input parameters
    validateSubscription(subscription);
    validateLineItems(lineItems);
    validatePeriod(periodStart, periodEnd);

    // Generate invoice number
    String invoiceNumber = generateInvoiceNumber();

    // Get currency from subscription plan
    String currency = subscription.getPlan().getCurrency();

    // Get due days from configuration
    int dueDays = billingProperties.invoice().dueDays();

    // Create draft invoice
    Invoice invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        currency,
        periodStart,
        periodEnd,
        dueDays
    );

    // Add all line items
    for (final InvoiceLineItem lineItem : lineItems) {
      invoice.addLineItem(lineItem);
    }

    return invoice;
  }

  /**
   * Creates a new proration invoice for mid-period plan changes.
   *
   * <p>This method creates an invoice with proration line items based on
   * the proration calculation result. The invoice will include:
   * <ul>
   *   <li>Credit for unused time on the old plan (if downgrade)</li>
   *   <li>Charge for remaining time on the new plan (if upgrade)</li>
   *   <li>Net amount to charge or credit</li>
   * </ul>
   *
   * <p>The created invoice will be in DRAFT status and must be finalized
   * before it can be paid.
   *
   * @param subscription  the subscription this invoice is for
   * @param proration     the proration calculation result
   * @param effectiveDate the date when the plan change takes effect
   * @return a new Invoice in DRAFT status with proration line items
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Invoice createProrationInvoice(
      Subscription subscription,
      ProrationResult proration,
      LocalDateTime effectiveDate
  ) {
    // Validate input parameters
    validateSubscription(subscription);
    validateProration(proration);
    validateEffectiveDate(effectiveDate);

    // Generate invoice number
    String invoiceNumber = generateInvoiceNumber();

    // Get currency from subscription plan
    String currency = subscription.getPlan().getCurrency();

    // Get due days from configuration (immediate for proration)
    int dueDays = 0; // Proration invoices are due immediately

    // Create draft invoice
    Invoice invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        currency,
        effectiveDate,
        effectiveDate, // Single point in time for proration
        dueDays
    );

    // Add proration line items
    if (proration.creditAmount().compareTo(BigDecimal.ZERO) > 0) {
      // Add credit for unused time on old plan
      InvoiceLineItem creditItem = InvoiceLineItem.prorationCredit(
          proration.description() + " - Credit",
          proration.creditAmount()
      );
      invoice.addLineItem(creditItem);
    }

    if (proration.chargeAmount().compareTo(BigDecimal.ZERO) > 0) {
      // Add charge for remaining time on new plan
      InvoiceLineItem chargeItem = InvoiceLineItem.prorationCharge(
          proration.description() + " - Charge",
          proration.chargeAmount()
      );
      invoice.addLineItem(chargeItem);
    }

    return invoice;
  }

  /**
   * Creates a new invoice with custom line items and period.
   *
   * <p>This is a flexible factory method for creating invoices with custom
   * line items that don't fit the standard subscription or proration patterns.
   * Use cases include:
   * <ul>
   *   <li>One-time charges</li>
   *   <li>Manual adjustments</li>
   *   <li>Custom billing scenarios</li>
   * </ul>
   *
   * @param subscription the subscription this invoice is for
   * @param lineItems    the line items to include
   * @param periodStart  the billing period start date
   * @param periodEnd    the billing period end date
   * @param dueDays      number of days until payment is due
   * @return a new Invoice in DRAFT status
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Invoice createCustomInvoice(
      Subscription subscription,
      List<InvoiceLineItem> lineItems,
      LocalDateTime periodStart,
      LocalDateTime periodEnd,
      int dueDays
  ) {
    // Validate input parameters
    validateSubscription(subscription);
    validateLineItems(lineItems);
    validatePeriod(periodStart, periodEnd);
    validateDueDays(dueDays);

    // Generate invoice number
    String invoiceNumber = generateInvoiceNumber();

    // Get currency from subscription plan
    String currency = subscription.getPlan().getCurrency();

    // Create draft invoice
    Invoice invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        currency,
        periodStart,
        periodEnd,
        dueDays
    );

    // Add all line items
    for (final InvoiceLineItem lineItem : lineItems) {
      invoice.addLineItem(lineItem);
    }

    return invoice;
  }

  /**
   * Creates a usage-based invoice with metered charges.
   *
   * <p>This method creates an invoice for usage-based billing, typically
   * including both the base subscription fee and usage charges for the period.
   *
   * @param subscription    the subscription this invoice is for
   * @param subscriptionFee the base subscription fee line item
   * @param usageCharges    the usage charge line items
   * @param periodStart     the billing period start date
   * @param periodEnd       the billing period end date
   * @return a new Invoice in DRAFT status with subscription and usage charges
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Invoice createUsageInvoice(
      Subscription subscription,
      InvoiceLineItem subscriptionFee,
      List<InvoiceLineItem> usageCharges,
      LocalDateTime periodStart,
      LocalDateTime periodEnd
  ) {
    // Validate input parameters
    validateSubscription(subscription);
    validateLineItem(subscriptionFee);
    validatePeriod(periodStart, periodEnd);

    // Generate invoice number
    String invoiceNumber = generateInvoiceNumber();

    // Get currency from subscription plan
    String currency = subscription.getPlan().getCurrency();

    // Get due days from configuration
    int dueDays = billingProperties.invoice().dueDays();

    // Create draft invoice
    Invoice invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        currency,
        periodStart,
        periodEnd,
        dueDays
    );

    // Add subscription fee
    invoice.addLineItem(subscriptionFee);

    // Add usage charges if any
    if (usageCharges != null && !usageCharges.isEmpty()) {
      for (final InvoiceLineItem usageCharge : usageCharges) {
        invoice.addLineItem(usageCharge);
      }
    }

    return invoice;
  }

  // Private helper methods

  /**
   * Generates a unique invoice number following the configured format.
   *
   * <p>Default format: INV-{YEAR}{MONTH}-{SEQUENCE}
   * Example: INV-202412-00001
   *
   * <p>Note: This is a simplified implementation. In production, you would
   * typically use a database sequence or distributed ID generator to ensure
   * uniqueness across multiple instances.
   *
   * @return a unique invoice number
   */
  private String generateInvoiceNumber() {
    String format = billingProperties.invoice().numberFormat();
    LocalDateTime now = LocalDateTime.now();

    // Generate year-month part
    String yearMonth = now.format(YEAR_MONTH_FORMATTER);

    // Generate sequence (simplified - in production use database sequence)
    String sequence = String.format("%05d", generateSequence());

    // Replace placeholders in format
    return format
        .replace("{YEAR}", String.valueOf(now.getYear()))
        .replace("{MONTH}", String.format("%02d", now.getMonthValue()))
        .replace("{YEARMONTH}", yearMonth)
        .replace("{SEQUENCE}", sequence);
  }

  /**
   * Generates a sequence number for invoice numbering.
   *
   * <p>This is a simplified implementation using timestamp-based sequence.
   * In production, use a database sequence or distributed ID generator.
   *
   * @return a sequence number
   */
  private long generateSequence() {
    // Simplified implementation - use last 5 digits of current time millis
    // In production, use database sequence or Redis counter
    return System.currentTimeMillis() % 100000;
  }

  // Validation methods

  private void validateSubscription(Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }

    if (subscription.getTenantId() == null) {
      throw new IllegalArgumentException("Subscription must have a tenant ID");
    }

    if (subscription.getPlan() == null) {
      throw new IllegalArgumentException("Subscription must have a plan");
    }
  }

  private void validateLineItems(List<InvoiceLineItem> lineItems) {
    if (lineItems == null) {
      throw new IllegalArgumentException("Line items cannot be null");
    }

    if (lineItems.isEmpty()) {
      throw new IllegalArgumentException("Line items cannot be empty");
    }

    // Validate each line item
    for (final InvoiceLineItem lineItem : lineItems) {
      validateLineItem(lineItem);
    }
  }

  private void validateLineItem(InvoiceLineItem lineItem) {
    if (lineItem == null) {
      throw new IllegalArgumentException("Line item cannot be null");
    }
  }

  private void validatePeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
    if (periodStart == null) {
      throw new IllegalArgumentException("Period start cannot be null");
    }

    if (periodEnd == null) {
      throw new IllegalArgumentException("Period end cannot be null");
    }

    if (!periodEnd.isAfter(periodStart)) {
      throw new IllegalArgumentException("Period end must be after period start");
    }
  }

  private void validateProration(ProrationResult proration) {
    if (proration == null) {
      throw new IllegalArgumentException("Proration result cannot be null");
    }
  }

  private void validateEffectiveDate(LocalDateTime effectiveDate) {
    if (effectiveDate == null) {
      throw new IllegalArgumentException("Effective date cannot be null");
    }
  }

  private void validateDueDays(int dueDays) {
    if (dueDays < 0) {
      throw new IllegalArgumentException("Due days cannot be negative");
    }
  }
}
