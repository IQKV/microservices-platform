package com.iqscaffold.billingservice.shared;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private TemplateEngine templateEngine;

  @Mock
  private IqScaffoldProperties properties;

  @Mock
  private MessageService messageService;

  @Mock
  private MimeMessage mimeMessage;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    emailService = new EmailService(mailSender, templateEngine, properties, messageService);
    
    // Setup common mocks
    IqScaffoldProperties.Email emailConfig = mock(IqScaffoldProperties.Email.class);
    IqScaffoldProperties.Email.Sender senderConfig = mock(IqScaffoldProperties.Email.Sender.class);
    
    lenient().when(properties.email()).thenReturn(emailConfig);
    lenient().when(emailConfig.sender()).thenReturn(senderConfig);
    lenient().when(senderConfig.fromEmail()).thenReturn("noreply@example.com");
    lenient().when(senderConfig.fromName()).thenReturn("Billing Service");
    
    lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    lenient().when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>Test Email</html>");
  }

  @Test
  void sendMerchantOnboardingEmail_shouldSendEmailSuccessfully() throws MessagingException {
    // Given
    String merchantEmail = "merchant@example.com";
    String merchantName = "Test Merchant";
    String onboardingUrl = "https://example.com/onboard";
    
    when(messageService.getMessage("email.merchant.onboarding.subject", Locale.ENGLISH))
        .thenReturn("Complete Your Merchant Onboarding");

    // When
    emailService.sendMerchantOnboardingEmail(merchantEmail, merchantName, onboardingUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/merchant-onboarding"), any(Context.class));
  }

  @Test
  void sendMerchantOnboardingEmail_shouldHandleMessagingException() throws MessagingException {
    // Given
    String merchantEmail = "merchant@example.com";
    String merchantName = "Test Merchant";
    String onboardingUrl = "https://example.com/onboard";
    
    lenient().when(messageService.getMessage("email.merchant.onboarding.subject", Locale.ENGLISH))
        .thenReturn("Complete Your Merchant Onboarding");
    when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("MIME creation failed"));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendMerchantOnboardingEmail(merchantEmail, merchantName, onboardingUrl)
    );
  }

  @Test
  void sendMerchantOnboardingEmail_shouldHandleMailException() throws MessagingException {
    // Given
    String merchantEmail = "merchant@example.com";
    String merchantName = "Test Merchant";
    String onboardingUrl = "https://example.com/onboard";
    
    when(messageService.getMessage("email.merchant.onboarding.subject", Locale.ENGLISH))
        .thenReturn("Complete Your Merchant Onboarding");
    doThrow(new MailException("Mail sending failed") {}).when(mailSender).send(any(MimeMessage.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendMerchantOnboardingEmail(merchantEmail, merchantName, onboardingUrl)
    );
  }

  @Test
  void sendPaymentSuccessfulEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String paymentId = "pay_123";
    BigDecimal amount = new BigDecimal("99.99");
    String currency = "USD";
    String description = "Premium subscription";
    LocalDateTime paymentDate = LocalDateTime.now();
    String paymentMethod = "Visa ending in 4242";
    String receiptUrl = "https://example.com/receipt";
    
    when(messageService.getMessage("email.payment.successful.subject", Locale.ENGLISH))
        .thenReturn("Payment Successful");

    // When
    emailService.sendPaymentSuccessfulEmail(customerEmail, customerName, paymentId, amount, currency,
        description, paymentDate, paymentMethod, receiptUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/payment-successful"), any(Context.class));
  }

  @Test
  void sendPaymentFailedEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String paymentId = "pay_123";
    BigDecimal amount = new BigDecimal("99.99");
    String currency = "USD";
    String description = "Premium subscription";
    LocalDateTime attemptDate = LocalDateTime.now();
    String errorMessage = "Card declined";
    String retryUrl = "https://example.com/retry";
    
    when(messageService.getMessage("email.payment.failed.subject", Locale.ENGLISH))
        .thenReturn("Payment Failed");

    // When
    emailService.sendPaymentFailedEmail(customerEmail, customerName, paymentId, amount, currency,
        description, attemptDate, errorMessage, retryUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/payment-failed"), any(Context.class));
  }

  @Test
  void sendPaymentRefundedEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String paymentId = "pay_123";
    BigDecimal amount = new BigDecimal("99.99");
    String currency = "USD";
    String refundId = "ref_123";
    LocalDateTime refundDate = LocalDateTime.now();
    
    when(messageService.getMessage("email.payment.refunded.subject", Locale.ENGLISH))
        .thenReturn("Payment Refunded");

    // When
    emailService.sendPaymentRefundedEmail(customerEmail, customerName, paymentId, amount, currency,
        refundId, refundDate);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/payment-refunded"), any(Context.class));
  }

  @Test
  void sendInvoiceGeneratedEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String invoiceNumber = "INV-001";
    BigDecimal amount = new BigDecimal("199.99");
    String currency = "USD";
    LocalDateTime issueDate = LocalDateTime.now();
    LocalDateTime dueDate = LocalDateTime.now().plusDays(30);
    String description = "Monthly subscription";
    String invoiceUrl = "https://example.com/invoice";
    
    when(messageService.getMessage("email.invoice.generated.subject", Locale.ENGLISH))
        .thenReturn("New Invoice Generated");

    // When
    emailService.sendInvoiceGeneratedEmail(customerEmail, customerName, invoiceNumber, amount, currency,
        issueDate, dueDate, description, invoiceUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/invoice-generated"), any(Context.class));
  }

  @Test
  void sendInvoicePaidEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String invoiceNumber = "INV-001";
    BigDecimal amount = new BigDecimal("199.99");
    String currency = "USD";
    LocalDateTime paymentDate = LocalDateTime.now();
    String paymentMethod = "Visa ending in 4242";
    String transactionId = "txn_123";
    String receiptUrl = "https://example.com/receipt";
    
    when(messageService.getMessage("email.invoice.paid.subject", Locale.ENGLISH))
        .thenReturn("Invoice Paid");

    // When
    emailService.sendInvoicePaidEmail(customerEmail, customerName, invoiceNumber, amount, currency,
        paymentDate, paymentMethod, transactionId, receiptUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/invoice-paid"), any(Context.class));
  }

  @Test
  void sendSubscriptionCreatedEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String planName = "Premium Plan";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "USD";
    String interval = "month";
    String status = "active";
    LocalDateTime trialEnd = LocalDateTime.now().plusDays(7);
    LocalDateTime nextBillingDate = LocalDateTime.now().plusDays(30);
    String dashboardUrl = "https://example.com/dashboard";
    
    when(messageService.getMessage("email.subscription.created.subject", Locale.ENGLISH))
        .thenReturn("Subscription Created");

    // When
    emailService.sendSubscriptionCreatedEmail(customerEmail, customerName, planName, amount, currency,
        interval, status, trialEnd, nextBillingDate, dashboardUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/subscription-created"), any(Context.class));
  }

  @Test
  void sendSubscriptionTrialEndingEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String planName = "Premium Plan";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "USD";
    String interval = "month";
    LocalDateTime trialEnd = LocalDateTime.now().plusDays(3);
    LocalDateTime nextBillingDate = LocalDateTime.now().plusDays(3);
    String manageUrl = "https://example.com/manage";
    
    when(messageService.getMessage("email.subscription.trial.ending.subject", Locale.ENGLISH))
        .thenReturn("Trial Ending Soon");

    // When
    emailService.sendSubscriptionTrialEndingEmail(customerEmail, customerName, planName, amount, currency,
        interval, trialEnd, nextBillingDate, manageUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/subscription-trial-ending"), any(Context.class));
  }

  @Test
  void sendSubscriptionRenewedEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String planName = "Premium Plan";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "USD";
    LocalDateTime renewalDate = LocalDateTime.now();
    LocalDateTime nextBillingDate = LocalDateTime.now().plusDays(30);
    String invoiceUrl = "https://example.com/invoice";
    String dashboardUrl = "https://example.com/dashboard";
    
    when(messageService.getMessage("email.subscription.renewed.subject", Locale.ENGLISH))
        .thenReturn("Subscription Renewed");

    // When
    emailService.sendSubscriptionRenewedEmail(customerEmail, customerName, planName, amount, currency,
        renewalDate, nextBillingDate, invoiceUrl, dashboardUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/subscription-renewed"), any(Context.class));
  }

  @Test
  void sendSubscriptionPaymentFailedEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String planName = "Premium Plan";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "USD";
    LocalDateTime attemptDate = LocalDateTime.now();
    String errorMessage = "Card declined";
    LocalDateTime retryDate = LocalDateTime.now().plusDays(3);
    String updatePaymentUrl = "https://example.com/update-payment";
    
    when(messageService.getMessage("email.subscription.payment.failed.subject", Locale.ENGLISH))
        .thenReturn("Subscription Payment Failed");

    // When
    emailService.sendSubscriptionPaymentFailedEmail(customerEmail, customerName, planName, amount, currency,
        attemptDate, errorMessage, retryDate, updatePaymentUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/subscription-payment-failed"), any(Context.class));
  }

  @Test
  void sendSubscriptionCanceledEmail_shouldSendEmailSuccessfully() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String planName = "Premium Plan";
    LocalDateTime cancellationDate = LocalDateTime.now();
    LocalDateTime accessEndDate = LocalDateTime.now().plusDays(30);
    String reason = "User requested";
    String feedbackUrl = "https://example.com/feedback";
    String reactivateUrl = "https://example.com/reactivate";
    
    when(messageService.getMessage("email.subscription.canceled.subject", Locale.ENGLISH))
        .thenReturn("Subscription Canceled");

    // When
    emailService.sendSubscriptionCanceledEmail(customerEmail, customerName, planName, cancellationDate,
        accessEndDate, reason, feedbackUrl, reactivateUrl);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/subscription-canceled"), any(Context.class));
  }

  @Test
  void sendPaymentSuccessfulEmail_shouldHandleException() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String paymentId = "pay_123";
    BigDecimal amount = new BigDecimal("99.99");
    String currency = "USD";
    
    when(messageService.getMessage("email.payment.successful.subject", Locale.ENGLISH))
        .thenReturn("Payment Successful");
    doThrow(new RuntimeException("Template processing failed")).when(templateEngine)
        .process(any(String.class), any(Context.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendPaymentSuccessfulEmail(customerEmail, customerName, paymentId, amount, currency,
            null, null, null, null)
    );
  }

  @Test
  void sendPaymentFailedEmail_shouldHandleException() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String paymentId = "pay_123";
    BigDecimal amount = new BigDecimal("99.99");
    String currency = "USD";
    
    when(messageService.getMessage("email.payment.failed.subject", Locale.ENGLISH))
        .thenReturn("Payment Failed");
    doThrow(new RuntimeException("Template processing failed")).when(templateEngine)
        .process(any(String.class), any(Context.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendPaymentFailedEmail(customerEmail, customerName, paymentId, amount, currency,
            null, null, null, null)
    );
  }

  @Test
  void sendPaymentRefundedEmail_shouldHandleException() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String paymentId = "pay_123";
    BigDecimal amount = new BigDecimal("99.99");
    String currency = "USD";
    
    when(messageService.getMessage("email.payment.refunded.subject", Locale.ENGLISH))
        .thenReturn("Payment Refunded");
    doThrow(new RuntimeException("Template processing failed")).when(templateEngine)
        .process(any(String.class), any(Context.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendPaymentRefundedEmail(customerEmail, customerName, paymentId, amount, currency,
            null, null)
    );
  }

  @Test
  void sendInvoiceGeneratedEmail_shouldHandleException() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String invoiceNumber = "INV-001";
    BigDecimal amount = new BigDecimal("199.99");
    String currency = "USD";
    
    when(messageService.getMessage("email.invoice.generated.subject", Locale.ENGLISH))
        .thenReturn("New Invoice Generated");
    doThrow(new RuntimeException("Template processing failed")).when(templateEngine)
        .process(any(String.class), any(Context.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendInvoiceGeneratedEmail(customerEmail, customerName, invoiceNumber, amount, currency,
            null, null, null, null)
    );
  }

  @Test
  void sendInvoicePaidEmail_shouldHandleException() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String invoiceNumber = "INV-001";
    BigDecimal amount = new BigDecimal("199.99");
    String currency = "USD";
    LocalDateTime paymentDate = LocalDateTime.now();
    
    when(messageService.getMessage("email.invoice.paid.subject", Locale.ENGLISH))
        .thenReturn("Invoice Paid");
    doThrow(new RuntimeException("Template processing failed")).when(templateEngine)
        .process(any(String.class), any(Context.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendInvoicePaidEmail(customerEmail, customerName, invoiceNumber, amount, currency,
            paymentDate, null, null, null)
    );
  }

  @Test
  void sendSubscriptionCreatedEmail_shouldHandleException() {
    // Given
    String customerEmail = "customer@example.com";
    String customerName = "John Doe";
    String planName = "Premium Plan";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "USD";
    String interval = "month";
    String status = "active";
    
    when(messageService.getMessage("email.subscription.created.subject", Locale.ENGLISH))
        .thenReturn("Subscription Created");
    doThrow(new RuntimeException("Template processing failed")).when(templateEngine)
        .process(any(String.class), any(Context.class));

    // When & Then
    assertThrows(EmailService.EmailServiceException.class, () ->
        emailService.sendSubscriptionCreatedEmail(customerEmail, customerName, planName, amount, currency,
            interval, status, null, null, null)
    );
  }
}