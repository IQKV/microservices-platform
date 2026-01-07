package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAuditTrailServiceTest {

    private PaymentAuditTrailService auditService;

    @BeforeEach
    void setUp() {
        auditService = new PaymentAuditTrailService();
    }

    @Test
    void logPaymentAttempt_shouldLogWithPaymentId() {
        // Given
        UUID paymentId = UUID.randomUUID();
        String status = BillingConstants.PaymentStatus.SUCCEEDED;

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            auditService.logPaymentAttempt(paymentId, status)
        );
    }

    @Test
    void logPaymentAttempt_shouldLogPendingStatus() {
        // Given
        UUID paymentId = UUID.randomUUID();

        // When & Then
        assertDoesNotThrow(() -> 
            auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.PENDING)
        );
    }

    @Test
    void logPaymentAttempt_shouldLogFailedStatus() {
        // Given
        UUID paymentId = UUID.randomUUID();

        // When & Then
        assertDoesNotThrow(() -> 
            auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.FAILED)
        );
    }

    @Test
    void logPaymentAttempt_shouldLogRefundedStatus() {
        // Given
        UUID paymentId = UUID.randomUUID();

        // When & Then
        assertDoesNotThrow(() -> 
            auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.REFUNDED)
        );
    }

    @Test
    void logPaymentAttempt_shouldHandleMultipleConsecutiveCalls() {
        // Given
        UUID paymentId = UUID.randomUUID();

        // When & Then
        assertDoesNotThrow(() -> {
            auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.PENDING);
            auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.PROCESSING);
            auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.SUCCEEDED);
        });
    }
}
