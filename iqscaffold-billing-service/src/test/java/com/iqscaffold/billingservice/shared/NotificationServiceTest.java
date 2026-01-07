package com.iqscaffold.billingservice.shared;

import com.iqscaffold.billingservice.infrastructure.messaging.MessagingService;
import com.iqscaffold.billingservice.infrastructure.messaging.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailOperations emailService;

    @Mock
    private MessagingService messagingService;

    @Mock
    private MessageService messageService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(emailService, messagingService, messageService);
        when(messageService.getMessage(anyString())).thenReturn("Test Subject");
    }

    @Test
    void sendMerchantOnboardingNotification_shouldSendEmailAndPublishEvent() {
        // Given
        String merchantEmail = "merchant@example.com";
        String merchantName = "Test Merchant";
        String onboardingUrl = "https://example.com/onboard";
        String tenantId = "tenant123";

        // When
        notificationService.sendMerchantOnboardingNotification(merchantEmail, merchantName, 
            onboardingUrl, tenantId);

        // Then
        verify(emailService).sendMerchantOnboardingEmail(merchantEmail, merchantName, onboardingUrl);
        verify(messagingService).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void sendPaymentSuccessfulNotification_shouldSendEmailAndPublishEvent() {
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
        String tenantId = "tenant123";

        // When
        notificationService.sendPaymentSuccessfulNotification(customerEmail, customerName, 
            paymentId, amount, currency, description, paymentDate, paymentMethod, receiptUrl, tenantId);

        // Then
        verify(emailService).sendPaymentSuccessfulEmail(customerEmail, customerName, paymentId, 
            amount, currency, description, paymentDate, paymentMethod, receiptUrl);
        verify(messagingService).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void sendPaymentFailedNotification_shouldSendEmailAndPublishEvent() {
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
        String tenantId = "tenant123";

        // When
        notificationService.sendPaymentFailedNotification(customerEmail, customerName, 
            paymentId, amount, currency, description, attemptDate, errorMessage, retryUrl, tenantId);

        // Then
        verify(emailService).sendPaymentFailedEmail(customerEmail, customerName, paymentId, 
            amount, currency, description, attemptDate, errorMessage, retryUrl);
        verify(messagingService).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void sendPaymentRefundedNotification_shouldSendEmailAndPublishEvent() {
        // Given
        String customerEmail = "customer@example.com";
        String customerName = "John Doe";
        String paymentId = "pay_123";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String refundId = "ref_123";
        LocalDateTime refundDate = LocalDateTime.now();
        String tenantId = "tenant123";

        // When
        notificationService.sendPaymentRefundedNotification(customerEmail, customerName, 
            paymentId, amount, currency, refundId, refundDate, tenantId);

        // Then
        verify(emailService).sendPaymentRefundedEmail(customerEmail, customerName, paymentId, 
            amount, currency, refundId, refundDate);
        verify(messagingService).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void sendInvoiceGeneratedNotification_shouldSendEmailAndPublishEvent() {
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
        String tenantId = "tenant123";

        // When
        notificationService.sendInvoiceGeneratedNotification(customerEmail, customerName, 
            invoiceNumber, amount, currency, issueDate, dueDate, description, invoiceUrl, tenantId);

        // Then
        verify(emailService).sendInvoiceGeneratedEmail(customerEmail, customerName, invoiceNumber, 
            amount, currency, issueDate, dueDate, description, invoiceUrl);
        verify(messagingService).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void sendMerchantOnboardingNotification_shouldThrowExceptionOnFailure() {
        // Given
        String merchantEmail = "merchant@example.com";
        String merchantName = "Test Merchant";
        String onboardingUrl = "https://example.com/onboard";
        String tenantId = "tenant123";
        
        doThrow(new RuntimeException("Email failed"))
            .when(emailService).sendMerchantOnboardingEmail(anyString(), anyString(), anyString());

        // When & Then
        assertThrows(NotificationService.NotificationException.class, () ->
            notificationService.sendMerchantOnboardingNotification(merchantEmail, merchantName, 
                onboardingUrl, tenantId)
        );
    }
}
