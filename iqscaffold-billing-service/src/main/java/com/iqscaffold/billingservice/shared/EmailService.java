package com.iqscaffold.billingservice.shared;

import jakarta.mail.MessagingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending billing-related emails. Uses Spring Boot Mail with SMTP integration and Thymeleaf templates.
 */
@Service
public class EmailService implements EmailOperations {

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final IqScaffoldProperties properties;
  private final MessageService messageService;

  public EmailService(final JavaMailSender mailSender,
                      final TemplateEngine templateEngine,
                      final IqScaffoldProperties properties,
                      final MessageService messageService) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.properties = properties;
    this.messageService = messageService;
  }

  @Override
  public void sendMerchantOnboardingEmail(String merchantEmail, String merchantName, String onboardingUrl) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      var emailConfig = properties.email();
      var senderConfig = emailConfig.sender();

      // Set email properties with localized subject
      helper.setFrom(senderConfig.fromEmail(), senderConfig.fromName());
      helper.setTo(merchantEmail);
      helper.setSubject(messageService.getMessage("email.merchant.onboarding.subject", Locale.ENGLISH));

      // Create template context with localized messages
      var context = new Context(Locale.ENGLISH);
      context.setVariable("merchantName", merchantName);
      context.setVariable("merchantEmail", merchantEmail);
      context.setVariable("onboardingUrl", onboardingUrl);

      // Process HTML template
      var htmlContent = templateEngine.process("email/merchant-onboarding", context);
      helper.setText(htmlContent, true);

      // Send email
      mailSender.send(mimeMessage);

      logger.info("Merchant onboarding email sent successfully to: {} ({})",
          merchantName, merchantEmail);

    } catch (final MessagingException e) {
      logger.error("Failed to create merchant onboarding email for: {} ({})",
          merchantName, merchantEmail, e);
      throw new EmailServiceException("Failed to create merchant onboarding email", e);
    } catch (final MailException e) {
      logger.error("Failed to send merchant onboarding email to: {} ({})",
          merchantName, merchantEmail, e);
      throw new EmailServiceException("Failed to send merchant onboarding email", e);
    } catch (final Exception e) {
      logger.error("Unexpected error sending merchant onboarding email to: {} ({})",
          merchantName, merchantEmail, e);
      throw new EmailServiceException("Unexpected error sending merchant onboarding email", e);
    }
  }

  @Override
  public void sendPaymentSuccessfulEmail(String customerEmail, String customerName, String paymentId,
                                         BigDecimal amount, String currency, String description,
                                         LocalDateTime paymentDate, String paymentMethod, String receiptUrl) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      var emailConfig = properties.email();
      var senderConfig = emailConfig.sender();

      helper.setFrom(senderConfig.fromEmail(), senderConfig.fromName());
      helper.setTo(customerEmail);
      helper.setSubject(messageService.getMessage("email.payment.successful.subject", Locale.ENGLISH));

      // Create template context
      var context = new Context(Locale.ENGLISH);
      context.setVariable("customerName", customerName);
      context.setVariable("paymentId", paymentId);
      context.setVariable("amount", amount);
      context.setVariable("currency", currency);
      context.setVariable("description", description);
      context.setVariable("paymentDate", paymentDate);
      context.setVariable("paymentMethod", paymentMethod);
      context.setVariable("receiptUrl", receiptUrl);

      var htmlContent = templateEngine.process("email/payment-successful", context);
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      logger.info("Payment successful email sent to: {} for payment: {}", customerEmail, paymentId);

    } catch (final Exception e) {
      logger.error("Failed to send payment successful email to: {} for payment: {}",
          customerEmail, paymentId, e);
      throw new EmailServiceException("Failed to send payment successful email", e);
    }
  }

  @Override
  public void sendPaymentFailedEmail(String customerEmail, String customerName, String paymentId,
                                     BigDecimal amount, String currency, String description,
                                     LocalDateTime attemptDate, String errorMessage, String retryUrl) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      var emailConfig = properties.email();
      var senderConfig = emailConfig.sender();

      helper.setFrom(senderConfig.fromEmail(), senderConfig.fromName());
      helper.setTo(customerEmail);
      helper.setSubject(messageService.getMessage("email.payment.failed.subject", Locale.ENGLISH));

      // Create template context
      var context = new Context(Locale.ENGLISH);
      context.setVariable("customerName", customerName);
      context.setVariable("paymentId", paymentId);
      context.setVariable("amount", amount);
      context.setVariable("currency", currency);
      context.setVariable("description", description);
      context.setVariable("attemptDate", attemptDate);
      context.setVariable("errorMessage", errorMessage);
      context.setVariable("retryUrl", retryUrl);

      var htmlContent = templateEngine.process("email/payment-failed", context);
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      logger.info("Payment failed email sent to: {} for payment: {}", customerEmail, paymentId);

    } catch (final Exception e) {
      logger.error("Failed to send payment failed email to: {} for payment: {}",
          customerEmail, paymentId, e);
      throw new EmailServiceException("Failed to send payment failed email", e);
    }
  }

  @Override
  public void sendPaymentRefundedEmail(String customerEmail, String customerName, String paymentId,
                                       BigDecimal amount, String currency, String refundId, LocalDateTime refundDate) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      var emailConfig = properties.email();
      var senderConfig = emailConfig.sender();

      helper.setFrom(senderConfig.fromEmail(), senderConfig.fromName());
      helper.setTo(customerEmail);
      helper.setSubject(messageService.getMessage("email.payment.refunded.subject", Locale.ENGLISH));

      // Create template context
      var context = new Context(Locale.ENGLISH);
      context.setVariable("customerName", customerName);
      context.setVariable("paymentId", paymentId);
      context.setVariable("amount", amount);
      context.setVariable("currency", currency);
      context.setVariable("refundId", refundId);
      context.setVariable("refundDate", refundDate);

      var htmlContent = templateEngine.process("email/payment-refunded", context);
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      logger.info("Payment refunded email sent to: {} for payment: {}", customerEmail, paymentId);

    } catch (final Exception e) {
      logger.error("Failed to send payment refunded email to: {} for payment: {}",
          customerEmail, paymentId, e);
      throw new EmailServiceException("Failed to send payment refunded email", e);
    }
  }

  @Override
  public void sendInvoiceGeneratedEmail(String customerEmail, String customerName, String invoiceNumber,
                                        BigDecimal amount, String currency, LocalDateTime issueDate,
                                        LocalDateTime dueDate, String description, String invoiceUrl) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      var emailConfig = properties.email();
      var senderConfig = emailConfig.sender();

      helper.setFrom(senderConfig.fromEmail(), senderConfig.fromName());
      helper.setTo(customerEmail);
      helper.setSubject(messageService.getMessage("email.invoice.generated.subject", Locale.ENGLISH));

      // Create template context
      var context = new Context(Locale.ENGLISH);
      context.setVariable("customerName", customerName);
      context.setVariable("invoiceNumber", invoiceNumber);
      context.setVariable("amount", amount);
      context.setVariable("currency", currency);
      context.setVariable("issueDate", issueDate);
      context.setVariable("dueDate", dueDate);
      context.setVariable("description", description);
      context.setVariable("invoiceUrl", invoiceUrl);

      var htmlContent = templateEngine.process("email/invoice-generated", context);
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      logger.info("Invoice generated email sent to: {} for invoice: {}", customerEmail, invoiceNumber);

    } catch (final Exception e) {
      logger.error("Failed to send invoice generated email to: {} for invoice: {}",
          customerEmail, invoiceNumber, e);
      throw new EmailServiceException("Failed to send invoice generated email", e);
    }
  }

  /**
   * Custom exception for email service errors.
   */
  public static class EmailServiceException extends RuntimeException {

    public EmailServiceException(final String message) {
      super(message);
    }

    public EmailServiceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
