package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.admin.MerchantStripeConfig;
import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.payment.dto.PaymentDtos;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentProviderAdapter paymentProvider;

  @Mock
  private MerchantStripeConfigRepository merchantConfigRepository;

  @Mock
  private PaymentStateMachine stateMachine;

  @Mock
  private PaymentAuditTrailService auditService;

  private PaymentServiceImpl paymentService;

  @BeforeEach
  void setUp() {
    paymentService = new PaymentServiceImpl(
        paymentRepository,
        paymentProvider,
        merchantConfigRepository,
        stateMachine,
        auditService
    );
  }

  @Test
  void createPaymentIntent_shouldCreatePaymentSuccessfully() {
    // Given
    PaymentDtos.CreatePaymentRequest request = new PaymentDtos.CreatePaymentRequest(
        new BigDecimal("100.00"),
        "usd",
        "Test payment",
        "test@example.com",
        "Test User",
        Map.of("orderId", "123")
    );

    Payment savedPayment = createTestPayment();
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

    PaymentProviderAdapter.ProviderPaymentIntent providerIntent =
        new PaymentProviderAdapter.ProviderPaymentIntent("pi_123", "secret_123");
    when(paymentProvider.createPaymentIntent(
        any(), any(), any(), any(), any(), any(), any(), any(), any()
    )).thenReturn(providerIntent);

    when(merchantConfigRepository.findByTenantId(any())).thenReturn(Optional.empty());

    // When
    PaymentDtos.PaymentResponse response = paymentService.createPaymentIntent(request);

    // Then
    assertNotNull(response);
    assertEquals("secret_123", response.clientSecret());
    assertEquals(new BigDecimal("100.00"), response.amount());
    assertEquals("usd", response.currency());
    verify(stateMachine).validateTransition(null, BillingConstants.PaymentStatus.PENDING);
    verify(paymentRepository, times(2)).save(any(Payment.class));
    verify(auditService).logPaymentAttempt(any(UUID.class), eq(BillingConstants.PaymentStatus.PENDING));
  }

  @Test
  void createPaymentIntent_shouldCalculateApplicationFeeForConnectedAccount() {
    // Given
    PaymentDtos.CreatePaymentRequest request = new PaymentDtos.CreatePaymentRequest(
        new BigDecimal("100.00"),
        "usd",
        "Test payment",
        "test@example.com",
        "Test User",
        null
    );

    MerchantStripeConfig merchantConfig = new MerchantStripeConfig();
    merchantConfig.setStripeAccountId("acct_123");
    merchantConfig.setApplicationFeePercent(new BigDecimal("15.0"));

    when(merchantConfigRepository.findByTenantId(any())).thenReturn(Optional.of(merchantConfig));

    Payment savedPayment = createTestPayment();
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

    PaymentProviderAdapter.ProviderPaymentIntent providerIntent =
        new PaymentProviderAdapter.ProviderPaymentIntent("pi_123", "secret_123");
    when(paymentProvider.createPaymentIntent(
        any(), any(), any(), any(), any(), any(), any(), any(), any()
    )).thenReturn(providerIntent);

    // When
    paymentService.createPaymentIntent(request);

    // Then
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());

    Payment capturedPayment = paymentCaptor.getAllValues().get(0);
    assertEquals(new BigDecimal("15.00"), capturedPayment.getApplicationFeeAmount());
    assertEquals("acct_123", capturedPayment.getMerchantAccountId());
  }

  @Test
  void getPayment_shouldReturnPaymentWhenFound() {
    // Given
    UUID paymentId = UUID.randomUUID();
    Payment payment = createTestPayment();
    payment.setId(paymentId);
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    // When
    PaymentDtos.PaymentResponse response = paymentService.getPayment(paymentId);

    // Then
    assertNotNull(response);
    assertEquals(paymentId, response.id());
    assertEquals(new BigDecimal("100.00"), response.amount());
  }

  @Test
  void getPayment_shouldThrowExceptionWhenNotFound() {
    // Given
    UUID paymentId = UUID.randomUUID();
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(PaymentNotFoundException.class, () ->
        paymentService.getPayment(paymentId)
    );
  }

  @Test
  void getPayments_shouldReturnPagedResults() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    List<Payment> payments = Arrays.asList(createTestPayment(), createTestPayment());
    Page<Payment> paymentPage = new PageImpl<>(payments, pageable, payments.size());
    when(paymentRepository.findAll(pageable)).thenReturn(paymentPage);

    // When
    Page<PaymentDtos.PaymentResponse> result = paymentService.getPayments(pageable);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getContent().size());
    verify(paymentRepository).findAll(pageable);
  }

  @Test
  void updateStatus_shouldUpdatePaymentStatus() {
    // Given
    String paymentIntentId = "pi_123";
    Payment payment = createTestPayment();
    payment.setPaymentIntentId(paymentIntentId);
    payment.setStatus(BillingConstants.PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentIntentId(paymentIntentId))
        .thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

    // When
    paymentService.updateStatus(paymentIntentId, BillingConstants.PaymentStatus.SUCCEEDED);

    // Then
    verify(stateMachine).validateTransition(
        BillingConstants.PaymentStatus.PENDING,
        BillingConstants.PaymentStatus.SUCCEEDED
    );
    verify(paymentRepository).save(payment);
    verify(auditService).logPaymentAttempt(payment.getId(), BillingConstants.PaymentStatus.SUCCEEDED);
    assertEquals(BillingConstants.PaymentStatus.SUCCEEDED, payment.getStatus());
  }

  @Test
  void updateStatus_shouldThrowExceptionWhenPaymentNotFound() {
    // Given
    String paymentIntentId = "pi_nonexistent";
    when(paymentRepository.findByPaymentIntentId(paymentIntentId))
        .thenReturn(Optional.empty());

    // When & Then
    assertThrows(PaymentNotFoundException.class, () ->
        paymentService.updateStatus(paymentIntentId, BillingConstants.PaymentStatus.SUCCEEDED)
    );
  }

  private Payment createTestPayment() {
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID());
    payment.setAmount(new BigDecimal("100.00"));
    payment.setCurrency("usd");
    payment.setStatus(BillingConstants.PaymentStatus.PENDING);
    payment.setClientSecret("secret_123");
    payment.setPaymentIntentId("pi_123");
    payment.setCreatedAt(Instant.now());
    payment.setUpdatedAt(Instant.now());
    return payment;
  }
}
