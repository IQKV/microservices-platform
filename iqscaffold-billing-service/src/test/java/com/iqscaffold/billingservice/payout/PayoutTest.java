package com.iqscaffold.billingservice.payout;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class PayoutTest {

  @Test
  void constructor_shouldCreateEmptyPayout() {
    // When
    Payout payout = new Payout();

    // Then
    assertNotNull(payout);
    assertNull(payout.getId());
    assertNull(payout.getAmount());
    assertNull(payout.getCurrency());
    assertNull(payout.getStatus());
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    Payout payout = new Payout();
    String id = "po_123";
    BigDecimal amount = new BigDecimal("500.00");
    String currency = "usd";
    String status = "paid";
    Instant arrivalDate = Instant.now();
    String merchantAccountId = "acct_456";

    // When
    payout.setId(id);
    payout.setAmount(amount);
    payout.setCurrency(currency);
    payout.setStatus(status);
    payout.setArrivalDate(arrivalDate);
    payout.setMerchantAccountId(merchantAccountId);

    // Then
    assertEquals(id, payout.getId());
    assertEquals(amount, payout.getAmount());
    assertEquals(currency, payout.getCurrency());
    assertEquals(status, payout.getStatus());
    assertEquals(arrivalDate, payout.getArrivalDate());
    assertEquals(merchantAccountId, payout.getMerchantAccountId());
  }

  @Test
  void payout_shouldHandleNullOptionalFields() {
    // Given
    Payout payout = new Payout();
    payout.setId("po_123");
    payout.setAmount(new BigDecimal("100.00"));
    payout.setCurrency("usd");

    // When & Then - should not throw
    assertDoesNotThrow(() -> {
      payout.setStatus(null);
      payout.setArrivalDate(null);
      payout.setMerchantAccountId(null);
    });

    assertNull(payout.getStatus());
    assertNull(payout.getArrivalDate());
    assertNull(payout.getMerchantAccountId());
  }

  @Test
  void payout_shouldHandleDifferentStatuses() {
    // Given
    Payout payout = new Payout();

    // When & Then
    payout.setStatus("paid");
    assertEquals("paid", payout.getStatus());

    payout.setStatus("pending");
    assertEquals("pending", payout.getStatus());

    payout.setStatus("in_transit");
    assertEquals("in_transit", payout.getStatus());

    payout.setStatus("failed");
    assertEquals("failed", payout.getStatus());

    payout.setStatus("canceled");
    assertEquals("canceled", payout.getStatus());
  }

  @Test
  void payout_shouldHandleZeroAmount() {
    // Given
    Payout payout = new Payout();
    payout.setAmount(BigDecimal.ZERO);

    // When & Then
    assertEquals(BigDecimal.ZERO, payout.getAmount());
  }

  @Test
  void payout_shouldHandleLargeAmount() {
    // Given
    Payout payout = new Payout();
    BigDecimal largeAmount = new BigDecimal("9999999.99");
    payout.setAmount(largeAmount);

    // When & Then
    assertEquals(largeAmount, payout.getAmount());
  }

  @Test
  void payout_shouldHandleDifferentCurrencies() {
    // Given
    Payout payout = new Payout();

    // When & Then
    payout.setCurrency("usd");
    assertEquals("usd", payout.getCurrency());

    payout.setCurrency("eur");
    assertEquals("eur", payout.getCurrency());

    payout.setCurrency("gbp");
    assertEquals("gbp", payout.getCurrency());

    payout.setCurrency("jpy");
    assertEquals("jpy", payout.getCurrency());
  }

  @Test
  void payout_shouldHandleFutureArrivalDate() {
    // Given
    Payout payout = new Payout();
    Instant futureDate = Instant.now().plusSeconds(86400); // 1 day in future

    // When
    payout.setArrivalDate(futureDate);

    // Then
    assertEquals(futureDate, payout.getArrivalDate());
    assertTrue(payout.getArrivalDate().isAfter(Instant.now()));
  }

  @Test
  void payout_shouldHandlePastArrivalDate() {
    // Given
    Payout payout = new Payout();
    Instant pastDate = Instant.now().minusSeconds(86400); // 1 day in past

    // When
    payout.setArrivalDate(pastDate);

    // Then
    assertEquals(pastDate, payout.getArrivalDate());
    assertTrue(payout.getArrivalDate().isBefore(Instant.now()));
  }
}
