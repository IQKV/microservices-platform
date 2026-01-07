package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.exception.InvalidPaymentStateException;
import com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentProviderAdapter paymentProvider;

  @Mock
  private PaymentAuditTrailService auditService;

  @Mock
  private EmailService emailService;

  private RefundServiceImpl refundService;

  @BeforeEach
  void setUp() {
    refundService = new RefundServiceImpl(
        paymentRepository,
        paymentProvider,
        auditService,
        emailService
    );
  }

  @Test
  void processRefund_shouldRefundSuccessfulPayment() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
    doNothing().when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When
    refundService.processRefund(paymentId);

    // Then
    verify(paymentProvider).refundPayment(
        eq("pi_123"),
        eq(Optional.empty()),
        eq("usd"),
        eq(Optional.empty())
    );
    verify(paymentRepository).save(payment);
    verify(auditService).logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.REFUNDED);
    assertEquals(BillingConstants.PaymentStatus.REFUNDED, payment.getStatus());
  }

  @Test
  void processRefund_shouldRefundPaymentWithMerchantAccount() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);
    payment.setMerchantAccountId("acct_123");

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
    doNothing().when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When
    refundService.processRefund(paymentId);

    // Then
    verify(paymentProvider).refundPayment(
        eq("pi_123"),
        eq(Optional.empty()),
        eq("usd"),
        eq(Optional.of("acct_123"))
    );
  }

  @Test
  void processRefund_shouldThrowExceptionWhenPaymentNotFound() {
    // Given
    UUID paymentId = UUID.randomUUID();
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(PaymentNotFoundException.class, () ->
        refundService.processRefund(paymentId)
    );
    verify(paymentProvider, never()).refundPayment(any(), any(), any(), any());
  }

  @Test
  void processRefund_shouldThrowExceptionWhenPaymentNotSucceeded() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);
    payment.setStatus(BillingConstants.PaymentStatus.PENDING);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    // When & Then
    assertThrows(InvalidPaymentStateException.class, () ->
        refundService.processRefund(paymentId)
    );
    verify(paymentProvider, never()).refundPayment(any(), any(), any(), any());
  }

  @Test
  void processRefund_shouldNotRefundFailedPayment() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);
    payment.setStatus(BillingConstants.PaymentStatus.FAILED);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    // When & Then
    assertThrows(InvalidPaymentStateException.class, () ->
        refundService.processRefund(paymentId)
    );
  }

  @Test
  void processRefund_shouldNotRefundAlreadyRefundedPayment() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);
    payment.setStatus(BillingConstants.PaymentStatus.REFUNDED);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    // When & Then
    assertThrows(InvalidPaymentStateException.class, () ->
        refundService.processRefund(paymentId)
    );
  }

  @Test
  void processRefund_shouldPropagateProviderException() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    doThrow(new RuntimeException("Provider error"))
        .when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When & Then
    assertThrows(RuntimeException.class, () ->
        refundService.processRefund(paymentId)
    );
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void processRefund_shouldHandlePartiallyRefundedPayment() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);
    payment.setStatus(BillingConstants.PaymentStatus.PARTIALLY_REFUNDED);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    // When & Then
    assertThrows(InvalidPaymentStateException.class, () ->
        refundService.processRefund(paymentId)
    );
  }

  @Test
  void processRefund_shouldHandleNullUserContext() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
    doNothing().when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When - should not throw even if user context is null
    refundService.processRefund(paymentId);

    // Then
    verify(paymentRepository).save(payment);
    verify(auditService).logPaymentAttempt(paymentId, BillingConstants.PaymentStatus.REFUNDED);
  }

  @Test
  void processRefund_shouldHandleNullMerchantAccountId() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);
    payment.setMerchantAccountId(null);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
    doNothing().when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When
    refundService.processRefund(paymentId);

    // Then
    verify(paymentProvider).refundPayment(
        eq("pi_123"),
        eq(Optional.empty()),
        eq("usd"),
        eq(Optional.empty())
    );
  }

  @Test
  void processRefund_shouldHandleProviderExceptionDuringRefund() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    RuntimeException providerException = new RuntimeException("Stripe API error");
    doThrow(providerException).when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When & Then
    RuntimeException thrown = assertThrows(RuntimeException.class, () ->
        refundService.processRefund(paymentId)
    );
    assertEquals("Stripe API error", thrown.getMessage());
    verify(paymentRepository, never()).save(any());
    verify(auditService, never()).logPaymentAttempt(any(), any());
  }

  @Test
  void processRefund_shouldNotSendEmailWhenUserContextIsNull() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createSuccessfulPayment(paymentId);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
    doNothing().when(paymentProvider).refundPayment(any(), any(), any(), any());

    // When
    refundService.processRefund(paymentId);

    // Then
    verify(emailService, never()).sendEmail(any(), any(), any(), any(), any());
  }

  private Payment createSuccessfulPayment(UUID paymentId) {
    Payment payment = new Payment();
    payment.setId(paymentId);
    payment.setAmount(new BigDecimal("100.00"));
    payment.setCurrency("usd");
    payment.setStatus(BillingConstants.PaymentStatus.SUCCEEDED);
    payment.setPaymentIntentId("pi_123");
    return payment;
  }
}
