package com.iqscaffold.billingservice.payout;

public class PayoutNotFoundException extends RuntimeException {
  public PayoutNotFoundException(String message) {
    super(message);
  }
}
