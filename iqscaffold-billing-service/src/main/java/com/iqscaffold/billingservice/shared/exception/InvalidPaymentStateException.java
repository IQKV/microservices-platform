package com.iqscaffold.billingservice.shared.exception;

public class InvalidPaymentStateException extends RuntimeException {
  public InvalidPaymentStateException(final String message) {
    super(message);
  }
}
