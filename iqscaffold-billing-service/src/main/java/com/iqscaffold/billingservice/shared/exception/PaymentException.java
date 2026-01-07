package com.iqscaffold.billingservice.shared.exception;

public class PaymentException extends RuntimeException {
  public PaymentException(final String message) {
    super(message);
  }

  public PaymentException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
