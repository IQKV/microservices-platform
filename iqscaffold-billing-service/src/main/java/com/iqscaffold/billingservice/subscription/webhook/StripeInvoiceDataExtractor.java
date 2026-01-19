package com.iqscaffold.billingservice.subscription.webhook;

import java.math.BigDecimal;
import java.time.Instant;

import com.iqscaffold.billingservice.subscription.InvoiceStatus;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoice;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for extracting and mapping Stripe-specific invoice data.
 * <p>
 * This service isolates all Stripe-specific logic from the main webhook handler,
 * making it easier to support multiple payment providers in the future.
 */
@Service
public class StripeInvoiceDataExtractor {

  private static final Logger logger = LoggerFactory.getLogger(StripeInvoiceDataExtractor.class);

  /**
   * Extract basic invoice data from webhook event metadata.
   */
  public InvoiceData extractBasicData(WebhookEvent event) {
    String subscriptionId = event.getMetadataString("subscription").orElse(null);
    return new InvoiceData(
        event.resourceId(),
        subscriptionId
    );
  }

  /**
   * Extract Stripe invoice object from webhook event metadata.
   */
  public com.stripe.model.Invoice extractStripeInvoice(WebhookEvent event) {
    Object rawInvoice = event.metadata().get("invoice");
    if (rawInvoice instanceof com.stripe.model.Invoice stripeInvoice) {
      return stripeInvoice;
    }
    return null;
  }

  /**
   * Populate invoice entity with detailed Stripe invoice data.
   */
  public void populateFromStripeInvoice(SubscriptionInvoice invoice, com.stripe.model.Invoice stripeInvoice) {
    // Set basic invoice information
    invoice.setStripeInvoiceId(stripeInvoice.getId());
    invoice.setInvoiceNumber(stripeInvoice.getNumber());

    // Set amounts (convert from cents to decimal)
    populateAmounts(invoice, stripeInvoice);

    // Set currency and status
    invoice.setCurrency(stripeInvoice.getCurrency().toUpperCase());
    invoice.setStatus(mapStripeStatusToLocal(stripeInvoice.getStatus()));

    // Set URLs
    populateUrls(invoice, stripeInvoice);

    // Set due date
    populateDueDate(invoice, stripeInvoice);
  }

  /**
   * Map Stripe invoice status to local status enum.
   */
  public InvoiceStatus mapStripeStatusToLocal(String stripeStatus) {
    return switch (stripeStatus.toLowerCase()) {
      case "draft" -> InvoiceStatus.DRAFT;
      case "open" -> InvoiceStatus.OPEN;
      case "paid" -> InvoiceStatus.PAID;
      case "void" -> InvoiceStatus.VOID;
      case "uncollectible" -> InvoiceStatus.UNCOLLECTIBLE;
      default -> {
        logger.warn("Unknown Stripe invoice status: {}, defaulting to OPEN", stripeStatus);
        yield InvoiceStatus.OPEN;
      }
    };
  }

  private void populateAmounts(SubscriptionInvoice invoice, com.stripe.model.Invoice stripeInvoice) {
    if (stripeInvoice.getAmountDue() != null) {
      invoice.setAmountDue(
          BigDecimal.valueOf(stripeInvoice.getAmountDue()).divide(BigDecimal.valueOf(100))
      );
    }

    if (stripeInvoice.getAmountPaid() != null) {
      invoice.setAmountPaid(
          BigDecimal.valueOf(stripeInvoice.getAmountPaid()).divide(BigDecimal.valueOf(100))
      );
    }
  }

  private void populateUrls(SubscriptionInvoice invoice, com.stripe.model.Invoice stripeInvoice) {
    invoice.setHostedInvoiceUrl(stripeInvoice.getHostedInvoiceUrl());
    invoice.setInvoicePdfUrl(stripeInvoice.getInvoicePdf());
  }

  private void populateDueDate(SubscriptionInvoice invoice, com.stripe.model.Invoice stripeInvoice) {
    if (stripeInvoice.getDueDate() != null) {
      invoice.setDueDate(Instant.ofEpochSecond(stripeInvoice.getDueDate()));
    }
  }

  /**
   * Data transfer object for basic invoice information.
   */
  public record InvoiceData(
      String invoiceId,
      String subscriptionId
  ) {
  }
}
