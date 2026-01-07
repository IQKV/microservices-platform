package com.iqscaffold.billingservice.shared.exception;

public class PaymentNotFoundException extends RuntimeException {
  public PaymentNotFoundException(final String message) {
    super(message);
  }
}
