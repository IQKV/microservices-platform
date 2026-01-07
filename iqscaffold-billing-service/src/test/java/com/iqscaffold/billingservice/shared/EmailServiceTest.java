package com.iqscaffold.billingservice.shared;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

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
        
        IqScaffoldProperties.Email emailConfig = mock(IqScaffoldProperties.Email.class);
        IqScaffoldProperties.Email.Sender senderConfig = mock(IqScaffoldProperties.Email.Sender.class);
        
        when(properties.email()).thenReturn(emailConfig);
        when(emailConfig.sender()).thenReturn(senderConfig);
        when(senderConfig.fromEmail()).thenReturn("noreply@example.com");
        when(senderConfig.fromName()).thenReturn("Billing Service");
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any())).thenReturn("<html>Email Content</html>");
    }

    @Test
    void sendMerchantOnboardingEmail_shouldSendSuccessfully() {
        // Given
        String merchantEmail = "merchant@example.com";
        String merchantName = "Test Merchant";
        String onboardingUrl = "https://example.com/onboard";
        
        when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Merchant Onboarding");

        // When
        emailService.sendMerchantOnboardingEmail(merchantEmail, merchantName, onboardingUrl);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("email/merchant-onboarding"), any());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPaymentSuccessfulEmail_shouldSendSuccessfully() {
        // Given
        String customerEmail = "customer@example.com";
        String customerName = "John Doe";
        String paymentId = "pay_123";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String description = "Test payment";
        LocalDateTime paymentDate = LocalDateTime.now();
        String paymentMethod = "card";
        String receiptUrl = "https://example.com/receipt";
        
        when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Payment Successful");

        // When
        emailService.sendPaymentSuccessfulEmail(customerEmail, customerName, paymentId, 
            amount, currency, description, paymentDate, paymentMethod, receiptUrl);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("email/payment-successful"), any());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPaymentFailedEmail_shouldSendSuccessfully() {
        // Given
        String customerEmail = "customer@example.com";
        String customerName = "John Doe";
        String paymentId = "pay_123";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String description = "Test payment";
        LocalDateTime attemptDate = LocalDateTime.now();
        String errorMessage = "Insufficient funds";
        String retryUrl = "https://example.com/retry";
        
        when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Payment Failed");

        // When
        emailService.sendPaymentFailedEmail(customerEmail, customerName, paymentId, 
            amount, currency, description, attemptDate, errorMessage, retryUrl);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("email/payment-failed"), any());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPaymentRefundedEmail_shouldSendSuccessfully() {
        // Given
        String customerEmail = "customer@example.com";
        String customerName = "John Doe";
        String paymentId = "pay_123";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String refundId = "ref_123";
        LocalDateTime refundDate = LocalDateTime.now();
        
        when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Payment Refunded");

        // When
        emailService.sendPaymentRefundedEmail(customerEmail, customerName, paymentId, 
            amount, currency, refundId, refundDate);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("email/payment-refunded"), any());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceGeneratedEmail_shouldSendSuccessfully() {
        // Given
        String customerEmail = "customer@example.com";
        String customerName = "John Doe";
        String invoiceNumber = "INV-001";
        BigDecimal amount = new BigDecimal("500.00");
        String currency = "USD";
        LocalDateTime issueDate = LocalDateTime.now();
        LocalDateTime dueDate = LocalDateTime.now().plusDays(30);
        String description = "Monthly subscription";
        String invoiceUrl = "https://example.com/invoice";
        
        when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Invoice Generated");

        // When
        emailService.sendInvoiceGeneratedEmail(customerEmail, customerName, invoiceNumber, 
            amount, currency, issueDate, dueDate, description, invoiceUrl);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("email/invoice-generated"), any());
        verify(mailSender).send(mimeMessage);
    }
}
