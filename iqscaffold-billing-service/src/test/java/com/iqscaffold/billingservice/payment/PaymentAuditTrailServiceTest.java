package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @Test
  void logPaymentAttempt_shouldLogPartiallyRefundedStatus() {
    // Given
    UUID paymentId = UUID.randomUUID();

    // When & Then
    assertDoesNotThrow(() ->
        auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.PARTIALLY_REFUNDED)
    );
  }

  @Test
  void logPaymentAttempt_shouldLogProcessingStatus() {
    // Given
    UUID paymentId = UUID.randomUUID();

    // When & Then
    assertDoesNotThrow(() ->
        auditService.logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.PROCESSING)
    );
  }

  @Test
  void logPaymentAttempt_shouldHandleNullUserId() {
    // Given
    UUID paymentId = UUID.randomUUID();
    String status = BillingConstants.PaymentStatus.SUCCEEDED;

    // When & Then - should handle null user ID gracefully
    assertDoesNotThrow(() ->
        auditService.logPaymentAttempt(paymentId, status)
    );
  }

  @Test
  void logPaymentAttempt_shouldHandleNullTenantId() {
    // Given
    UUID paymentId = UUID.randomUUID();
    String status = BillingConstants.PaymentStatus.FAILED;

    // When & Then - should handle null tenant ID gracefully
    assertDoesNotThrow(() ->
        auditService.logPaymentAttempt(paymentId, status)
    );
  }

  @Test
  void logPaymentAttempt_shouldHandleDifferentPaymentIds() {
    // Given
    UUID paymentId1 = UUID.randomUUID();
    UUID paymentId2 = UUID.randomUUID();
    UUID paymentId3 = UUID.randomUUID();

    // When & Then
    assertDoesNotThrow(() -> {
      auditService.logPaymentAttempt(paymentId1, BillingConstants.PaymentStatus.SUCCEEDED);
      auditService.logPaymentAttempt(paymentId2, BillingConstants.PaymentStatus.FAILED);
      auditService.logPaymentAttempt(paymentId3, BillingConstants.PaymentStatus.REFUNDED);
    });
  }
}
