package com.iqscaffold.billingservice.payout;

public class PayoutNotFoundException extends RuntimeException {
  public PayoutNotFoundException(final String message) {
    super(message);
  }
}
