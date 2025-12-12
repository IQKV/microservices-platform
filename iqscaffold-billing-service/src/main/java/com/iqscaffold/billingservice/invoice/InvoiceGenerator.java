package com.iqscaffold.billingservice.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.iqscaffold.billingservice.billing.ProrationResult;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.springframework.stereotype.Service;

/**
 * Domain service for generating invoices for subscriptions.
 *
 * <p>This service encapsulates the complex business logic for creating invoices
 * based on subscription billing periods, plan changes, and usage. It handles:
 * <ul>
 *   <li>Regular subscription billing invoices</li>
 *   <li>Proration invoices for mid-period plan changes</li>
 *   <li>Usage-based billing line items</li>
 *   <li>Tax calculations</li>
 *   <li>Invoice number generation</li>
 * </ul>
 *
 * <p>This logic spans multiple aggregates (Subscription, SubscriptionPlan, UsageRecord)
 * and involves complex calculations that don't naturally belong to any single aggregate.
 * Therefore, it's implemented as a stateless domain service.
 *
 * <p>Usage example:
 * <pre>{@code
 * Invoice invoice = invoiceGenerator.generate(
 *   subscription,
 *   periodStart,
 *   periodEnd
 * );
 *
 * // Or for proration
 * Invoice prorationInvoice = invoiceGenerator.generateProrationInvoice(
 *   subscription,
 *   prorationResult
 * );
 * }</pre>
 *
 * <p>Design Rationale: Domain services keep business logic in the domain layer while
 * avoiding artificial assignment to aggregates. They remain stateless and focused on
 * domain operations.
 *
 * @see Invoice
 * @see InvoiceLineItem
 * @see Subscription
 * @see ProrationResult
 */
@Service
public class InvoiceGenerator {

  private static final String INVOICE_NUMBER_FORMAT = "INV-%s-%05d";
  private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMM");
  private static final int DEFAULT_DUE_DAYS = 7;
  private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.00"); // 0% default
  private static final java.util.Random RANDOM = new java.util.Random();

  /**
   * Generates an invoice for a subscription billing period.
   *
   * <p>This method creates a complete invoice including:
   * <ul>
   *   <li>Subscription plan charge for the period</li>
   *   <li>Usage-based charges (if applicable)</li>
   *   <li>Tax calculations</li>
   *   <li>Unique invoice number</li>
   * </ul>
   *
   * <p>The invoice is created in DRAFT status and must be finalized before payment.
   *
   * @param subscription the subscription to invoice
   * @param periodStart  the billing period start date
   * @param periodEnd    the billing period end date
   * @return a new Invoice in DRAFT status
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Invoice generate(
      final Subscription subscription,
      final LocalDateTime periodStart,
      final LocalDateTime periodEnd) {

    validateSubscription(subscription);
    validatePeriod(periodStart, periodEnd);

    // Generate unique invoice number
    var invoiceNumber = generateInvoiceNumber(periodStart);

    // Create draft invoice
    var invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        subscription.getPlan().getCurrency(),
        periodStart,
        periodEnd,
        DEFAULT_DUE_DAYS
    );

    // Add subscription plan charge line item
    var planCharge = createPlanChargeLineItem(subscription, periodStart, periodEnd);
    invoice.addLineItem(planCharge);

    // Calculate and add tax
    var taxAmount = calculateTax(invoice.getSubtotal());
    invoice.setTax(taxAmount);

    return invoice;
  }

  /**
   * Generates an invoice for a subscription billing period with usage charges.
   *
   * <p>This overloaded method includes usage-based charges in addition to the
   * subscription plan charge.
   *
   * @param subscription the subscription to invoice
   * @param periodStart  the billing period start date
   * @param periodEnd    the billing period end date
   * @param usageCharges list of usage-based charges to include
   * @return a new Invoice in DRAFT status with usage charges
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Invoice generateWithUsage(
      final Subscription subscription,
      final LocalDateTime periodStart,
      final LocalDateTime periodEnd,
      final List<UsageCharge> usageCharges) {

    validateSubscription(subscription);
    validatePeriod(periodStart, periodEnd);

    // Generate base invoice
    var invoice = generate(subscription, periodStart, periodEnd);

    // Add usage charge line items
    if (usageCharges != null && !usageCharges.isEmpty()) {
      for (final var usageCharge : usageCharges) {
        var lineItem = createUsageChargeLineItem(usageCharge);
        invoice.addLineItem(lineItem);
      }

      // Recalculate tax with usage charges included
      var taxAmount = calculateTax(invoice.getSubtotal());
      invoice.setTax(taxAmount);
    }

    return invoice;
  }

  /**
   * Generates a proration invoice for a mid-period plan change.
   *
   * <p>This method creates an invoice with:
   * <ul>
   *   <li>Credit line item for unused time on old plan</li>
   *   <li>Charge line item for new plan for remaining period</li>
   *   <li>Net amount calculation</li>
   * </ul>
   *
   * <p>The invoice may have a negative total (credit) or positive total (charge)
   * depending on whether it's an upgrade or downgrade.
   *
   * @param subscription    the subscription being changed
   * @param prorationResult the proration calculation result
   * @return a new Invoice in DRAFT status for the proration
   * @throws IllegalArgumentException if any parameter is null
   */
  public Invoice generateProrationInvoice(
      final Subscription subscription,
      final ProrationResult prorationResult) {

    validateSubscription(subscription);
    validateProrationResult(prorationResult);

    // Generate unique invoice number
    var invoiceNumber = generateInvoiceNumber(LocalDateTime.now());

    // Create draft invoice
    var invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        subscription.getPlan().getCurrency(),
        subscription.getCurrentPeriodStart(),
        subscription.getCurrentPeriodEnd(),
        DEFAULT_DUE_DAYS
    );

    // Add proration credit line item (for unused time on old plan)
    if (prorationResult.creditAmount().compareTo(BigDecimal.ZERO) > 0) {
      var creditLineItem = InvoiceLineItem.prorationCredit(
          "Credit for unused time: " + prorationResult.description(),
          prorationResult.creditAmount() // Pass positive amount, will be negated by factory
      );
      invoice.addLineItem(creditLineItem);
    }

    // Add proration charge line item (for new plan for remaining period)
    if (prorationResult.chargeAmount().compareTo(BigDecimal.ZERO) > 0) {
      var chargeLineItem = InvoiceLineItem.prorationCharge(
          "Charge for new plan: " + prorationResult.description(),
          prorationResult.chargeAmount()
      );
      invoice.addLineItem(chargeLineItem);
    }

    // Calculate and add tax on the net amount
    var taxAmount = calculateTax(invoice.getSubtotal());
    invoice.setTax(taxAmount);

    // Add metadata about the proration
    invoice.addMetadata("prorationDaysRemaining", prorationResult.daysRemaining());
    invoice.addMetadata("prorationDaysInPeriod", prorationResult.daysInPeriod());
    invoice.addMetadata("prorationDescription", prorationResult.description());

    return invoice;
  }

  /**
   * Generates an invoice for a one-time charge.
   *
   * <p>This method creates an invoice for charges that are not part of the
   * regular subscription billing cycle, such as:
   * <ul>
   *   <li>Setup fees</li>
   *   <li>One-time add-ons</li>
   *   <li>Manual adjustments</li>
   * </ul>
   *
   * @param subscription the subscription to invoice
   * @param description  description of the charge
   * @param amount       the charge amount
   * @return a new Invoice in DRAFT status for the one-time charge
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Invoice generateOneTimeCharge(
      final Subscription subscription,
      final String description,
      final BigDecimal amount) {

    validateSubscription(subscription);
    validateDescription(description);
    validateAmount(amount);

    // Generate unique invoice number
    var invoiceNumber = generateInvoiceNumber(LocalDateTime.now());

    // Create draft invoice (no specific period for one-time charges)
    var now = LocalDateTime.now();
    var periodEnd = now.plusSeconds(1); // Period end must be after period start
    var invoice = Invoice.createDraft(
        subscription,
        subscription.getTenantId(),
        invoiceNumber,
        subscription.getPlan().getCurrency(),
        now,
        periodEnd,
        DEFAULT_DUE_DAYS
    );

    // Add one-time charge line item
    var lineItem = InvoiceLineItem.subscriptionFee(description, amount);
    invoice.addLineItem(lineItem);

    // Calculate and add tax
    var taxAmount = calculateTax(invoice.getSubtotal());
    invoice.setTax(taxAmount);

    return invoice;
  }

  /**
   * Generates an invoice number in the format INV-YYYYMM-XXXXX.
   *
   * <p>The sequence number should be obtained from a database sequence or
   * counter to ensure uniqueness. This implementation uses a placeholder
   * that should be replaced with actual sequence generation.
   *
   * @param date the date to use for the year-month portion
   * @return a unique invoice number
   */
  public String generateInvoiceNumber(final LocalDateTime date) {
    if (date == null) {
      throw new IllegalArgumentException("Date cannot be null");
    }

    var yearMonth = date.format(YEAR_MONTH_FORMATTER);

    // TODO: Replace with actual sequence from database
    // This is a placeholder - in production, use a database sequence
    var sequence = RANDOM.nextInt(99999);

    return String.format(INVOICE_NUMBER_FORMAT, yearMonth, sequence);
  }

  /**
   * Calculates the next invoice date for a subscription.
   *
   * <p>This is typically the current period end date, when the next
   * invoice should be generated.
   *
   * @param subscription the subscription
   * @return the next invoice date
   */
  public LocalDateTime calculateNextInvoiceDate(final Subscription subscription) {
    validateSubscription(subscription);

    var periodEnd = subscription.getCurrentPeriodEnd();
    if (periodEnd == null) {
      throw new IllegalStateException("Subscription must have a current period end date");
    }

    return periodEnd;
  }

  /**
   * Checks if an invoice should be generated for a subscription.
   *
   * <p>Returns true if the subscription is in an active billing state
   * and the current period is ending soon (within the next 3 days).
   *
   * @param subscription the subscription to check
   * @return true if an invoice should be generated
   */
  public boolean shouldGenerateInvoice(final Subscription subscription) {
    validateSubscription(subscription);

    if (!subscription.isActiveBilling()) {
      return false;
    }

    var periodEnd = subscription.getCurrentPeriodEnd();
    if (periodEnd == null) {
      return false;
    }

    var now = LocalDateTime.now();
    var daysUntilPeriodEnd = java.time.Duration.between(now, periodEnd).toDays();

    // Generate invoice 3 days before period end
    return daysUntilPeriodEnd <= 3 && daysUntilPeriodEnd >= 0;
  }

  // Private helper methods

  private InvoiceLineItem createPlanChargeLineItem(
      Subscription subscription,
      LocalDateTime periodStart,
      LocalDateTime periodEnd) {

    var plan = subscription.getPlan();
    var description = String.format(
        "%s subscription (%s to %s)",
        plan.getName(),
        periodStart.toLocalDate(),
        periodEnd.toLocalDate()
    );

    return InvoiceLineItem.subscriptionFee(description, plan.getBasePrice());
  }

  private InvoiceLineItem createUsageChargeLineItem(UsageCharge usageCharge) {
    return InvoiceLineItem.subscriptionFee(
        usageCharge.description(),
        usageCharge.amount()
    );
  }

  private BigDecimal calculateTax(BigDecimal subtotal) {
    // TODO: Implement proper tax calculation based on tenant location
    // This is a placeholder - in production, integrate with tax service
    return subtotal.multiply(DEFAULT_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
  }

  private void validateSubscription(Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }
    if (subscription.getPlan() == null) {
      throw new IllegalArgumentException("Subscription must have a plan");
    }
  }

  private void validatePeriod(LocalDateTime start, LocalDateTime end) {
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

  private void validateProrationResult(ProrationResult prorationResult) {
    if (prorationResult == null) {
      throw new IllegalArgumentException("Proration result cannot be null");
    }
  }

  private void validateDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description cannot be blank");
    }
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
  }

  /**
   * Value object representing a usage-based charge.
   *
   * @param description description of the usage charge
   * @param amount      the charge amount
   */
  public record UsageCharge(
      String description,
      BigDecimal amount
  ) {
    public UsageCharge {
      if (description == null || description.isBlank()) {
        throw new IllegalArgumentException("Description cannot be blank");
      }
      if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Amount must be non-negative");
      }
    }
  }
}
