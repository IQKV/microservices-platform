package com.iqscaffold.billingservice.shared;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.billingservice.infrastructure.messaging.MessagingService;
import com.iqscaffold.billingservice.infrastructure.messaging.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for handling billing notifications. Integrates email sending with business logic.
 */
@Service
public class NotificationService {

  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

  private final EmailOperations emailService;
  private final MessagingService messagingService;
  private final MessageService messageService;

  public NotificationService(final EmailOperations emailService,
                             final MessagingService messagingService,
                             final MessageService messageService) {
    this.emailService = emailService;
    this.messagingService = messagingService;
    this.messageService = messageService;
  }

  /**
   * Send merchant onboarding notification
   */
  public void sendMerchantOnboardingNotification(String merchantEmail, String merchantName,
                                                 String onboardingUrl, String tenantId) {
    try {
      // Send email directly
      emailService.sendMerchantOnboardingEmail(merchantEmail, merchantName, onboardingUrl);

      // Also publish notification event for other services
      var templateData = createMerchantOnboardingTemplateData(merchantName, onboardingUrl);
      var notificationEvent = NotificationEvent.emailNotification(
          merchantEmail,
          merchantName,
          messageService.getMessage("email.merchant.onboarding.subject"),
          "email/merchant-onboarding",
          templateData,
          tenantId,
          null
      );
      messagingService.publishNotificationEvent(notificationEvent);

      logger.info("Merchant onboarding notification sent to: {} ({})", merchantName, merchantEmail);
    } catch (final Exception e) {
      logger.error("Failed to send merchant onboarding notification to: {} ({})",
          merchantName, merchantEmail, e);
      throw new NotificationException("Failed to send merchant onboarding notification", e);
    }
  }

  /**
   * Send payment successful notification
   */
  public void sendPaymentSuccessfulNotification(String customerEmail, String customerName,
                                                String paymentId, BigDecimal amount, String currency,
                                                String description, LocalDateTime paymentDate,
                                                String paymentMethod, String receiptUrl, String tenantId) {
    try {
      emailService.sendPaymentSuccessfulEmail(customerEmail, customerName, paymentId, amount,
          currency, description, paymentDate, paymentMethod, receiptUrl);

      var templateData = createPaymentSuccessfulTemplateData(paymentId, amount, currency,
          description, paymentDate, paymentMethod, receiptUrl);
      var notificationEvent = NotificationEvent.emailNotification(
          customerEmail,
          customerName,
          messageService.getMessage("email.payment.successful.subject"),
          "email/payment-successful",
          templateData,
          tenantId,
          null
      );
      messagingService.publishNotificationEvent(notificationEvent);

      logger.info("Payment successful notification sent to: {} for payment: {}", customerEmail, paymentId);
    } catch (final Exception e) {
      logger.error("Failed to send payment successful notification to: {} for payment: {}",
          customerEmail, paymentId, e);
      throw new NotificationException("Failed to send payment successful notification", e);
    }
  }

  /**
   * Send payment failed notification
   */
  public void sendPaymentFailedNotification(String customerEmail, String customerName,
                                            String paymentId, BigDecimal amount, String currency,
                                            String description, LocalDateTime attemptDate,
                                            String errorMessage, String retryUrl, String tenantId) {
    try {
      emailService.sendPaymentFailedEmail(customerEmail, customerName, paymentId, amount,
          currency, description, attemptDate, errorMessage, retryUrl);

      var templateData = createPaymentFailedTemplateData(paymentId, amount, currency,
          description, attemptDate, errorMessage, retryUrl);
      var notificationEvent = NotificationEvent.emailNotification(
          customerEmail,
          customerName,
          messageService.getMessage("email.payment.failed.subject"),
          "email/payment-failed",
          templateData,
          tenantId,
          null
      );
      messagingService.publishNotificationEvent(notificationEvent);

      logger.info("Payment failed notification sent to: {} for payment: {}", customerEmail, paymentId);
    } catch (final Exception e) {
      logger.error("Failed to send payment failed notification to: {} for payment: {}",
          customerEmail, paymentId, e);
      throw new NotificationException("Failed to send payment failed notification", e);
    }
  }

  /**
   * Send payment refunded notification
   */
  public void sendPaymentRefundedNotification(String customerEmail, String customerName,
                                              String paymentId, BigDecimal amount, String currency,
                                              String refundId, LocalDateTime refundDate, String tenantId) {
    try {
      emailService.sendPaymentRefundedEmail(customerEmail, customerName, paymentId, amount,
          currency, refundId, refundDate);

      var templateData = createPaymentRefundedTemplateData(paymentId, amount, currency, refundId, refundDate);
      var notificationEvent = NotificationEvent.emailNotification(
          customerEmail,
          customerName,
          messageService.getMessage("email.payment.refunded.subject"),
          "email/payment-refunded",
          templateData,
          tenantId,
          null
      );
      messagingService.publishNotificationEvent(notificationEvent);

      logger.info("Payment refunded notification sent to: {} for payment: {}", customerEmail, paymentId);
    } catch (final Exception e) {
      logger.error("Failed to send payment refunded notification to: {} for payment: {}",
          customerEmail, paymentId, e);
      throw new NotificationException("Failed to send payment refunded notification", e);
    }
  }

  /**
   * Send invoice generated notification
   */
  public void sendInvoiceGeneratedNotification(String customerEmail, String customerName,
                                               String invoiceNumber, BigDecimal amount, String currency,
                                               LocalDateTime issueDate, LocalDateTime dueDate,
                                               String description, String invoiceUrl, String tenantId) {
    try {
      emailService.sendInvoiceGeneratedEmail(customerEmail, customerName, invoiceNumber, amount,
          currency, issueDate, dueDate, description, invoiceUrl);

      var templateData = createInvoiceGeneratedTemplateData(invoiceNumber, amount, currency,
          issueDate, dueDate, description, invoiceUrl);
      var notificationEvent = NotificationEvent.emailNotification(
          customerEmail,
          customerName,
          messageService.getMessage("email.invoice.generated.subject"),
          "email/invoice-generated",
          templateData,
          tenantId,
          null
      );
      messagingService.publishNotificationEvent(notificationEvent);

      logger.info("Invoice generated notification sent to: {} for invoice: {}", customerEmail, invoiceNumber);
    } catch (final Exception e) {
      logger.error("Failed to send invoice generated notification to: {} for invoice: {}",
          customerEmail, invoiceNumber, e);
      throw new NotificationException("Failed to send invoice generated notification", e);
    }
  }

  private Map<String, Object> createMerchantOnboardingTemplateData(String merchantName, String onboardingUrl) {
    var data = new HashMap<String, Object>();
    data.put("merchantName", merchantName);
    data.put("onboardingUrl", onboardingUrl);
    return data;
  }

  private Map<String, Object> createPaymentSuccessfulTemplateData(String paymentId, BigDecimal amount,
                                                                  String currency, String description,
                                                                  LocalDateTime paymentDate, String paymentMethod,
                                                                  String receiptUrl) {
    var data = new HashMap<String, Object>();
    data.put("paymentId", paymentId);
    data.put("amount", amount);
    data.put("currency", currency);
    data.put("description", description);
    data.put("paymentDate", paymentDate);
    data.put("paymentMethod", paymentMethod);
    data.put("receiptUrl", receiptUrl);
    return data;
  }

  private Map<String, Object> createPaymentFailedTemplateData(String paymentId, BigDecimal amount,
                                                              String currency, String description,
                                                              LocalDateTime attemptDate, String errorMessage,
                                                              String retryUrl) {
    var data = new HashMap<String, Object>();
    data.put("paymentId", paymentId);
    data.put("amount", amount);
    data.put("currency", currency);
    data.put("description", description);
    data.put("attemptDate", attemptDate);
    data.put("errorMessage", errorMessage);
    data.put("retryUrl", retryUrl);
    return data;
  }

  private Map<String, Object> createPaymentRefundedTemplateData(String paymentId, BigDecimal amount,
                                                                String currency, String refundId,
                                                                LocalDateTime refundDate) {
    var data = new HashMap<String, Object>();
    data.put("paymentId", paymentId);
    data.put("amount", amount);
    data.put("currency", currency);
    data.put("refundId", refundId);
    data.put("refundDate", refundDate);
    return data;
  }

  private Map<String, Object> createInvoiceGeneratedTemplateData(String invoiceNumber, BigDecimal amount,
                                                                 String currency, LocalDateTime issueDate,
                                                                 LocalDateTime dueDate, String description,
                                                                 String invoiceUrl) {
    var data = new HashMap<String, Object>();
    data.put("invoiceNumber", invoiceNumber);
    data.put("amount", amount);
    data.put("currency", currency);
    data.put("issueDate", issueDate);
    data.put("dueDate", dueDate);
    data.put("description", description);
    data.put("invoiceUrl", invoiceUrl);
    return data;
  }

  /**
   * Custom exception for notification errors.
   */
  public static class NotificationException extends RuntimeException {

    public NotificationException(final String message) {
      super(message);
    }

    public NotificationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
