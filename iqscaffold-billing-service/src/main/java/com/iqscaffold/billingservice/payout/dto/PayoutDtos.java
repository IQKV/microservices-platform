package com.iqscaffold.billingservice.payout.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PayoutDtos {
  private PayoutDtos() {
  }

  public record PayoutResponse(
      String id,
      BigDecimal amount,
      String currency,
      String status,
      Instant arrivalDate,
      String merchantAccountId) {
  }
}
