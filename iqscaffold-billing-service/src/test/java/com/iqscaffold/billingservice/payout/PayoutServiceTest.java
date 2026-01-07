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
}
