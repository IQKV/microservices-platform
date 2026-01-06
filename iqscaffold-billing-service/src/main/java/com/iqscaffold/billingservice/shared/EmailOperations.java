package com.iqscaffold.billingservice.shared;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service interface for email operations. Handles sending billing-related emails.
 */
public interface EmailOperations {

  /**
   * Send merchant onboarding email with setup instructions.
   *
   * @param merchantEmail The merchant's email address
   * @param merchantName  The merchant's business name
   * @param onboardingUrl The URL to complete onboarding
   */
  void sendMerchantOnboardingEmail(String merchantEmail, String merchantName, String onboardingUrl);

  /**
   * Send payment successful confirmation email.
   *
   * @param customerEmail   The customer's email address
   * @param customerName    The customer's name
   * @param paymentId       The payment identifier
   * @param amount          The payment amount
   * @param currency        The payment currency
   * @param description     Payment description (optional)
   * @param paymentDate     Payment processing date (optional)
   * @param paymentMethod   Payment method used (optional)
   * @param receiptUrl      Receipt download URL (optional)
   */
  void sendPaymentSuccessfulEmail(String customerEmail, String customerName, String paymentId,
                                 BigDecimal amount, String currency, String description,
                                 LocalDateTime paymentDate, String paymentMethod, String receiptUrl);

  /**
   * Send payment failed notification email.
   *
   * @param customerEmail The customer's email address
   * @param customerName  The customer's name
   * @param paymentId     The payment attempt identifier
   * @param amount        The attempted payment amount
   * @param currency      The payment currency
   * @param description   Payment description (optional)
   * @param attemptDate   Date of payment attempt (optional)
   * @param errorMessage  Specific error message (optional)
   * @param retryUrl      URL to retry payment (optional)
   */
  void sendPaymentFailedEmail(String customerEmail, String customerName, String paymentId,
                             BigDecimal amount, String currency, String description,
                             LocalDateTime attemptDate, String errorMessage, String retryUrl);

  /**
   * Send payment refunded notification email.
   *
   * @param customerEmail The customer's email address
   * @param customerName  The customer's name (optional)
   * @param paymentId     The original payment identifier
   * @param amount        The refund amount
   * @param currency      The refund currency
   * @param refundId      The refund transaction identifier (optional)
   * @param refundDate    Date when refund was processed (optional)
   */
  void sendPaymentRefundedEmail(String customerEmail, String customerName, String paymentId,
                               BigDecimal amount, String currency, String refundId, LocalDateTime refundDate);

  /**
   * Send invoice generated notification email.
   *
   * @param customerEmail   The customer's email address
   * @param customerName    The customer's name (optional)
   * @param invoiceNumber   The invoice identifier
   * @param amount          The invoice amount
   * @param currency        The invoice currency
   * @param issueDate       Invoice issue date (optional)
   * @param dueDate         Payment due date (optional)
   * @param description     Invoice description (optional)
   * @param invoiceUrl      Download invoice URL (optional)
   */
  void sendInvoiceGeneratedEmail(String customerEmail, String customerName, String invoiceNumber,
                                BigDecimal amount, String currency, LocalDateTime issueDate,
                                LocalDateTime dueDate, String description, String invoiceUrl);
}