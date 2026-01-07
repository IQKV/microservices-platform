package com.iqscaffold.billingservice.payout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import com.stripe.model.Payout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

  @Mock
  private PayoutRepository payoutRepository;

  private PayoutService payoutService;

  @BeforeEach
  void setUp() {
    payoutService = new PayoutService(payoutRepository);
  }

  @Test
  void processPayout_shouldSavePayoutSuccessfully() {
    // Given
    Payout stripePayout = mock(Payout.class);
    when(stripePayout.getId()).thenReturn("po_123");
    when(stripePayout.getAmount()).thenReturn(10000L); // 100.00 in cents
    when(stripePayout.getCurrency()).thenReturn("usd");
    when(stripePayout.getStatus()).thenReturn("paid");
    when(stripePayout.getArrivalDate()).thenReturn(1609459200L); // Unix timestamp

    when(payoutRepository.save(any(com.iqscaffold.billingservice.payout.Payout.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    payoutService.processPayout(stripePayout);

    // Then
    ArgumentCaptor<com.iqscaffold.billingservice.payout.Payout> captor =
        ArgumentCaptor.forClass(com.iqscaffold.billingservice.payout.Payout.class);
    verify(payoutRepository).save(captor.capture());

    com.iqscaffold.billingservice.payout.Payout savedPayout = captor.getValue();
    assertEquals("po_123", savedPayout.getId());
    assertEquals(0, new BigDecimal("100.00").compareTo(savedPayout.getAmount()));
    assertEquals("usd", savedPayout.getCurrency());
    assertEquals("paid", savedPayout.getStatus());
    assertNotNull(savedPayout.getArrivalDate());
  }

  @Test
  void processPayout_shouldConvertAmountFromMinorUnits() {
    // Given
    Payout stripePayout = mock(Payout.class);
    when(stripePayout.getId()).thenReturn("po_456");
    when(stripePayout.getAmount()).thenReturn(25050L); // 250.50 in cents
    when(stripePayout.getCurrency()).thenReturn("eur");
    when(stripePayout.getStatus()).thenReturn("in_transit");
    when(stripePayout.getArrivalDate()).thenReturn(1609459200L);

    when(payoutRepository.save(any(com.iqscaffold.billingservice.payout.Payout.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    payoutService.processPayout(stripePayout);

    // Then
    ArgumentCaptor<com.iqscaffold.billingservice.payout.Payout> captor =
        ArgumentCaptor.forClass(com.iqscaffold.billingservice.payout.Payout.class);
    verify(payoutRepository).save(captor.capture());

    com.iqscaffold.billingservice.payout.Payout savedPayout = captor.getValue();
    assertEquals(0, new BigDecimal("250.50").compareTo(savedPayout.getAmount()));
  }

  @Test
  void processPayout_shouldHandleDifferentStatuses() {
    // Given
    Payout stripePayout = mock(Payout.class);
    when(stripePayout.getId()).thenReturn("po_789");
    when(stripePayout.getAmount()).thenReturn(5000L);
    when(stripePayout.getCurrency()).thenReturn("gbp");
    when(stripePayout.getStatus()).thenReturn("pending");
    when(stripePayout.getArrivalDate()).thenReturn(1609459200L);

    when(payoutRepository.save(any(com.iqscaffold.billingservice.payout.Payout.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    payoutService.processPayout(stripePayout);

    // Then
    ArgumentCaptor<com.iqscaffold.billingservice.payout.Payout> captor =
        ArgumentCaptor.forClass(com.iqscaffold.billingservice.payout.Payout.class);
    verify(payoutRepository).save(captor.capture());

    com.iqscaffold.billingservice.payout.Payout savedPayout = captor.getValue();
    assertEquals("pending", savedPayout.getStatus());
  }

  @Test
  void processPayout_shouldHandleFailedStatus() {
    // Given
    Payout stripePayout = mock(Payout.class);
    when(stripePayout.getId()).thenReturn("po_failed");
    when(stripePayout.getAmount()).thenReturn(15000L);
    when(stripePayout.getCurrency()).thenReturn("usd");
    when(stripePayout.getStatus()).thenReturn("failed");
    when(stripePayout.getArrivalDate()).thenReturn(1609459200L);

    when(payoutRepository.save(any(com.iqscaffold.billingservice.payout.Payout.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    payoutService.processPayout(stripePayout);

    // Then
    ArgumentCaptor<com.iqscaffold.billingservice.payout.Payout> captor =
        ArgumentCaptor.forClass(com.iqscaffold.billingservice.payout.Payout.class);
    verify(payoutRepository).save(captor.capture());

    com.iqscaffold.billingservice.payout.Payout savedPayout = captor.getValue();
    assertEquals("failed", savedPayout.getStatus());
  }

  @Test
  void processPayout_shouldHandleZeroAmount() {
    // Given
    Payout stripePayout = mock(Payout.class);
    when(stripePayout.getId()).thenReturn("po_zero");
    when(stripePayout.getAmount()).thenReturn(0L);
    when(stripePayout.getCurrency()).thenReturn("usd");
    when(stripePayout.getStatus()).thenReturn("paid");
    when(stripePayout.getArrivalDate()).thenReturn(1609459200L);

    when(payoutRepository.save(any(com.iqscaffold.billingservice.payout.Payout.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    payoutService.processPayout(stripePayout);

    // Then
    ArgumentCaptor<com.iqscaffold.billingservice.payout.Payout> captor =
        ArgumentCaptor.forClass(com.iqscaffold.billingservice.payout.Payout.class);
    verify(payoutRepository).save(captor.capture());

    com.iqscaffold.billingservice.payout.Payout savedPayout = captor.getValue();
    assertEquals(BigDecimal.ZERO, savedPayout.getAmount());
  }

  @Test
  void getPayouts_shouldReturnPagedResults() {
    // Given
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
    com.iqscaffold.billingservice.payout.Payout payout1 = new com.iqscaffold.billingservice.payout.Payout();
    payout1.setId("po_1");
    payout1.setAmount(new BigDecimal("100.00"));
    payout1.setCurrency("usd");
    payout1.setStatus("paid");
    payout1.setArrivalDate(java.time.Instant.now());

    java.util.List<com.iqscaffold.billingservice.payout.Payout> payouts = java.util.List.of(payout1);
    org.springframework.data.domain.Page<com.iqscaffold.billingservice.payout.Payout> page =
        new org.springframework.data.domain.PageImpl<>(payouts, pageable, 1);

    when(payoutRepository.findAll(pageable)).thenReturn(page);

    // When
    org.springframework.data.domain.Page<com.iqscaffold.billingservice.payout.dto.PayoutDtos.PayoutResponse> result =
        payoutService.getPayouts(pageable);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals("po_1", result.getContent().get(0).id());
  }

  @Test
  void getPayout_shouldReturnPayoutById() {
    // Given
    String payoutId = "po_123";
    com.iqscaffold.billingservice.payout.Payout payout = new com.iqscaffold.billingservice.payout.Payout();
    payout.setId(payoutId);
    payout.setAmount(new BigDecimal("200.00"));
    payout.setCurrency("eur");
    payout.setStatus("paid");
    payout.setArrivalDate(java.time.Instant.now());

    when(payoutRepository.findById(payoutId)).thenReturn(java.util.Optional.of(payout));

    // When
    com.iqscaffold.billingservice.payout.dto.PayoutDtos.PayoutResponse result = payoutService.getPayout(payoutId);

    // Then
    assertNotNull(result);
    assertEquals(payoutId, result.id());
    assertEquals(0, new BigDecimal("200.00").compareTo(result.amount()));
    assertEquals("eur", result.currency());
  }

  @Test
  void getPayout_shouldThrowExceptionWhenNotFound() {
    // Given
    String payoutId = "po_notfound";
    when(payoutRepository.findById(payoutId)).thenReturn(java.util.Optional.empty());

    // When & Then
    org.junit.jupiter.api.Assertions.assertThrows(
        PayoutNotFoundException.class,
        () -> payoutService.getPayout(payoutId)
    );
  }
}
