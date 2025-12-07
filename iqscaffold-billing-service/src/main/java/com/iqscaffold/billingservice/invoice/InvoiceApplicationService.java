package com.iqscaffold.billingservice.invoice;

import com.iqscaffold.billingservice.billing.ProrationResult;
import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.InvoiceGenerated;
import com.iqscaffold.billingservice.shared.event.InvoicePaid;
import com.iqscaffold.billingservice.shared.event.InvoiceVoided;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for invoice management operations.
 * 
 * <p>This service acts as a thin orchestration layer that coordinates invoice
 * operations across multiple domain services and aggregates. It handles:
 * <ul>
 *   <li>Invoice generation for regular billing periods</li>
 *   <li>Proration invoice generation for mid-period changes</li>
 *   <li>Invoice finalization and voiding</li>
 *   <li>PDF generation (async)</li>
 *   <li>Domain event publishing</li>
 *   <li>Transaction management</li>
 *   <li>DTO translation</li>
 * </ul>
 * 
 * <p>The service delegates business logic to domain services (InvoiceGenerator,
 * InvoiceFactory) and aggregates (Invoice, Subscription), maintaining a clean
 * separation between application orchestration and domain logic.
 * 
 * <p><strong>Async Processing Strategy:</strong>
 * <ul>
 *   <li>Invoice generation is triggered by scheduled jobs and processed asynchronously</li>
 *   <li>PDF generation is published to queue and processed by InvoiceGenerationConsumer</li>
 *   <li>Long-running operations (PDF generation) don't block HTTP requests</li>
 * </ul>
 * 
 * <p><strong>Event-Driven Architecture:</strong>
 * <ul>
 *   <li>InvoiceGenerated event triggers async email notification with PDF attachment</li>
 *   <li>InvoicePaid event triggers async receipt email and analytics update</li>
 *   <li>InvoiceVoided event triggers async notification</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Generate invoice for billing period
 * InvoiceDto invoice = invoiceApplicationService.generateInvoice(
 *     subscriptionId,
 *     periodStart,
 *     periodEnd
 * );
 * 
 * // Finalize invoice (triggers PDF generation and email)
 * invoiceApplicationService.finalizeInvoice(invoiceId);
 * 
 * // Void invoice
 * invoiceApplicationService.voidInvoice(invoiceId, "Customer requested cancellation");
 * }</pre>
 * 
 * @see InvoiceGenerator
 * @see InvoiceFactory
 * @see Invoice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceApplicationService {

  private final InvoiceRepository invoiceRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final InvoiceFactory invoiceFactory;
  private final InvoiceGenerator invoiceGenerator;
  private final DomainEventPublisher eventPublisher;
  private final MessageService messageService;
  private final BillingProperties billingProperties;

  /**
   * Generates an invoice for a subscription's regular billing period.
   * 
   * <p>This method creates a draft invoice with subscription charges for the
   * specified billing period. The invoice must be finalized before it can be paid.
   * 
   * <p><strong>Async Processing:</strong> Invoice generation is typically triggered
   * by a scheduled job and processed asynchronously. This method completes quickly
   * (< 2 seconds) to avoid blocking.
   * 
   * @param subscriptionId the subscription to invoice
   * @param periodStart the billing period start date
   * @param periodEnd the billing period end date
   * @return the generated invoice DTO
   * @throws InvoiceException.SubscriptionNotFoundException if subscription not found
   */
  @Transactional
  public InvoiceDto generateInvoice(
      final Long subscriptionId,
      final LocalDateTime periodStart,
      final LocalDateTime periodEnd) {

    log.info("Generating invoice for subscription {} for period {} to {}", 
        subscriptionId, periodStart, periodEnd);

    // Load subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new InvoiceException.SubscriptionNotFoundException(
            messageService.getMessage("invoice.subscription.not.found", subscriptionId)));

    // Use domain service to generate invoice
    var invoice = invoiceGenerator.generate(subscription, periodStart, periodEnd);

    // Persist invoice
    var savedInvoice = invoiceRepository.save(invoice);

    log.info("Generated invoice {} for subscription {}", 
        savedInvoice.getInvoiceNumber(), subscriptionId);

    // Translate to DTO
    return toDto(savedInvoice);
  }

  /**
   * Generates a proration invoice for a mid-period subscription change.
   * 
   * <p>This method creates a draft invoice with proration line items (credits
   * and charges) based on the proration calculation. The invoice is due immediately.
   * 
   * @param subscriptionId the subscription being changed
   * @param prorationResult the proration calculation result
   * @param effectiveDate the date when the change takes effect
   * @return the generated proration invoice DTO
   * @throws InvoiceException.SubscriptionNotFoundException if subscription not found
   */
  @Transactional
  public InvoiceDto generateProrationInvoice(
      final Long subscriptionId,
      final ProrationResult prorationResult,
      final LocalDateTime effectiveDate) {

    log.info("Generating proration invoice for subscription {} effective {}", 
        subscriptionId, effectiveDate);

    // Load subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new InvoiceException.SubscriptionNotFoundException(
            messageService.getMessage("invoice.subscription.not.found", subscriptionId)));

    // Use factory to create proration invoice
    var invoice = invoiceFactory.createProrationInvoice(
        subscription, prorationResult, effectiveDate);

    // Persist invoice
    var savedInvoice = invoiceRepository.save(invoice);

    log.info("Generated proration invoice {} for subscription {}", 
        savedInvoice.getInvoiceNumber(), subscriptionId);

    // Translate to DTO
    return toDto(savedInvoice);
  }

  /**
   * Finalizes a draft invoice, making it ready for payment.
   * 
   * <p>This method:
   * <ul>
   *   <li>Changes invoice status from DRAFT to OPEN</li>
   *   <li>Publishes InvoiceGenerated event (triggers async PDF generation and email)</li>
   *   <li>Returns the finalized invoice</li>
   * </ul>
   * 
   * <p><strong>Event-Driven:</strong> The InvoiceGenerated event triggers:
   * <ul>
   *   <li>Async PDF generation via InvoiceGenerationConsumer</li>
   *   <li>Async email notification with PDF attachment</li>
   * </ul>
   * 
   * @param invoiceId the invoice to finalize
   * @return the finalized invoice DTO
   * @throws InvoiceException.InvoiceNotFoundException if invoice not found
   * @throws InvoiceException.InvalidInvoiceStateException if invoice is not in DRAFT status
   */
  @Transactional
  public InvoiceDto finalizeInvoice(final Long invoiceId) {
    log.info("Finalizing invoice {}", invoiceId);

    // Load invoice
    var invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new InvoiceException.InvoiceNotFoundException(
            messageService.getMessage("invoice.not.found", invoiceId)));

    // Finalize invoice (domain logic)
    invoice.finalize();

    // Persist changes
    var savedInvoice = invoiceRepository.save(invoice);

    // Publish domain event (triggers async PDF generation and email)
    publishInvoiceGeneratedEvent(savedInvoice);

    log.info("Finalized invoice {} with number {}", 
        invoiceId, savedInvoice.getInvoiceNumber());

    // Translate to DTO
    return toDto(savedInvoice);
  }

  /**
   * Voids an unpaid invoice, preventing payment.
   * 
   * <p>This method:
   * <ul>
   *   <li>Changes invoice status to VOID</li>
   *   <li>Records the void reason</li>
   *   <li>Publishes InvoiceVoided event (triggers async notification)</li>
   * </ul>
   * 
   * <p>Only unpaid invoices (DRAFT or OPEN status) can be voided.
   * 
   * @param invoiceId the invoice to void
   * @param reason the reason for voiding
   * @return the voided invoice DTO
   * @throws InvoiceException.InvoiceNotFoundException if invoice not found
   * @throws InvoiceException.InvalidInvoiceStateException if invoice is already paid
   */
  @Transactional
  public InvoiceDto voidInvoice(final Long invoiceId, final String reason) {
    log.info("Voiding invoice {} with reason: {}", invoiceId, reason);

    // Load invoice
    var invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new InvoiceException.InvoiceNotFoundException(
            messageService.getMessage("invoice.not.found", invoiceId)));

    // Void invoice (domain logic)
    invoice.voidInvoice(reason);

    // Persist changes
    var savedInvoice = invoiceRepository.save(invoice);

    // Publish domain event (triggers async notification)
    publishInvoiceVoidedEvent(savedInvoice, reason);

    log.info("Voided invoice {} with number {}", 
        invoiceId, savedInvoice.getInvoiceNumber());

    // Translate to DTO
    return toDto(savedInvoice);
  }

  /**
   * Marks an invoice as paid.
   * 
   * <p>This method is typically called after successful payment processing.
   * It updates the invoice status and publishes events for downstream processing.
   * 
   * <p><strong>Event-Driven:</strong> The InvoicePaid event triggers:
   * <ul>
   *   <li>Async receipt email to customer</li>
   *   <li>Async analytics update (MRR, revenue tracking)</li>
   * </ul>
   * 
   * @param invoiceId the invoice that was paid
   * @param paymentMethodId the payment method used
   * @param paidAt the payment timestamp
   * @return the updated invoice DTO
   * @throws InvoiceException.InvoiceNotFoundException if invoice not found
   */
  @Transactional
  public InvoiceDto markInvoiceAsPaid(
      final Long invoiceId,
      final Long paymentMethodId,
      final LocalDateTime paidAt) {

    log.info("Marking invoice {} as paid", invoiceId);

    // Load invoice
    var invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new InvoiceException.InvoiceNotFoundException(
            messageService.getMessage("invoice.not.found", invoiceId)));

    // Mark as paid (domain logic)
    invoice.markAsPaid(paymentMethodId, paidAt);

    // Persist changes
    var savedInvoice = invoiceRepository.save(invoice);

    // Publish domain event (triggers async receipt email and analytics)
    publishInvoicePaidEvent(savedInvoice);

    log.info("Marked invoice {} as paid", invoiceId);

    // Translate to DTO
    return toDto(savedInvoice);
  }

  /**
   * Retrieves an invoice by ID.
   * 
   * @param invoiceId the invoice ID
   * @return the invoice DTO
   * @throws InvoiceException.InvoiceNotFoundException if invoice not found
   */
  @Transactional(readOnly = true)
  public InvoiceDto getInvoice(final Long invoiceId) {
    log.debug("Retrieving invoice {}", invoiceId);

    var invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new InvoiceException.InvoiceNotFoundException(
            messageService.getMessage("invoice.not.found", invoiceId)));

    return toDto(invoice);
  }

  /**
   * Retrieves all invoices for a subscription.
   * 
   * @param subscriptionId the subscription ID
   * @return list of invoice DTOs
   */
  @Transactional(readOnly = true)
  public List<InvoiceDto> getInvoicesBySubscription(final Long subscriptionId) {
    log.debug("Retrieving invoices for subscription {}", subscriptionId);

    var invoices = invoiceRepository.findBySubscriptionId(subscriptionId);
    return invoices.stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Retrieves all invoices for a tenant.
   * 
   * @param tenantId the tenant ID
   * @return list of invoice DTOs
   */
  @Transactional(readOnly = true)
  public List<InvoiceDto> getInvoicesByTenant(final UUID tenantId) {
    log.debug("Retrieving invoices for tenant {}", tenantId);

    var invoices = invoiceRepository.findByTenantId(tenantId.toString());
    return invoices.stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Generates invoice number following the configured format.
   * 
   * <p>Format: INV-{YEAR}{MONTH}-{SEQUENCE}
   * <p>Example: INV-202412-00001
   * 
   * <p>This method delegates to InvoiceGenerator for consistent number generation.
   * 
   * @return a unique invoice number
   */
  public String generateInvoiceNumber() {
    return invoiceGenerator.generateInvoiceNumber(LocalDateTime.now());
  }

  // Private helper methods

  /**
   * Publishes InvoiceGenerated domain event.
   * 
   * <p>This event triggers:
   * <ul>
   *   <li>Async PDF generation via message queue</li>
   *   <li>Async email notification with PDF attachment</li>
   * </ul>
   */
  private void publishInvoiceGeneratedEvent(Invoice invoice) {
    var event = new InvoiceGenerated(
        null, // eventId - will be auto-generated
        null, // occurredAt - will be auto-generated
        invoice.getId(),
        invoice.getTenantId(),
        invoice.getSubscription().getId(),
        invoice.getInvoiceNumber(),
        invoice.getTotal(),
        invoice.getCurrency(),
        invoice.getDueDate() != null ? 
            invoice.getDueDate().atZone(java.time.ZoneId.systemDefault()).toInstant() : 
            null
    );

    eventPublisher.publish(event);
    log.debug("Published InvoiceGenerated event for invoice {}", invoice.getId());
  }

  /**
   * Publishes InvoicePaid domain event.
   * 
   * <p>This event triggers:
   * <ul>
   *   <li>Async receipt email to customer</li>
   *   <li>Async analytics update (MRR, revenue tracking)</li>
   * </ul>
   */
  private void publishInvoicePaidEvent(Invoice invoice) {
    var event = new InvoicePaid(
        null, // eventId - will be auto-generated
        null, // occurredAt - will be auto-generated
        invoice.getId(),
        invoice.getTenantId(),
        invoice.getSubscription().getId(),
        invoice.getInvoiceNumber(),
        invoice.getPaymentMethodId(),
        invoice.getTotal(),
        invoice.getCurrency(),
        invoice.getPaidAt() != null ? 
            invoice.getPaidAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : 
            null
    );

    eventPublisher.publish(event);
    log.debug("Published InvoicePaid event for invoice {}", invoice.getId());
  }

  /**
   * Publishes InvoiceVoided domain event.
   * 
   * <p>This event triggers async notification to customer.
   */
  private void publishInvoiceVoidedEvent(Invoice invoice, String reason) {
    var event = new InvoiceVoided(
        null, // eventId - will be auto-generated
        null, // occurredAt - will be auto-generated
        invoice.getId(),
        invoice.getTenantId(),
        invoice.getSubscription().getId(),
        invoice.getInvoiceNumber(),
        reason,
        null // voidedBy - could be extracted from security context if needed
    );

    eventPublisher.publish(event);
    log.debug("Published InvoiceVoided event for invoice {}", invoice.getId());
  }

  /**
   * Translates Invoice entity to InvoiceDto.
   * 
   * @param invoice the invoice entity
   * @return the invoice DTO
   */
  private InvoiceDto toDto(Invoice invoice) {
    return new InvoiceDto(
        invoice.getId(),
        invoice.getSubscription().getId(),
        invoice.getTenantId(),
        invoice.getInvoiceNumber(),
        invoice.getStatus(),
        invoice.getSubtotal(),
        invoice.getTax(),
        invoice.getTotal(),
        invoice.getCurrency(),
        invoice.getPeriodStart(),
        invoice.getPeriodEnd(),
        invoice.getDueDate(),
        invoice.getPaidAt(),
        invoice.getPaymentMethodId(),
        invoice.getProviderInvoiceId(),
        invoice.getLineItems().stream()
            .map(this::toLineItemDto)
            .toList(),
        invoice.getMetadata(),
        invoice.getCreatedAt(),
        invoice.getUpdatedAt()
    );
  }

  /**
   * Translates InvoiceLineItem to InvoiceLineItemDto.
   * 
   * @param lineItem the line item entity
   * @return the line item DTO
   */
  private InvoiceLineItemDto toLineItemDto(InvoiceLineItem lineItem) {
    return new InvoiceLineItemDto(
        lineItem.type(),
        lineItem.description(),
        lineItem.quantity(),
        lineItem.unitPrice(),
        lineItem.amount()
    );
  }
}
