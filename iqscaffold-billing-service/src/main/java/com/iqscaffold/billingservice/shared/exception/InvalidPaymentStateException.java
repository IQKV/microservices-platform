package com.iqscaffold.billingservice.shared.exception;

public class InvalidPaymentStateException extends RuntimeException {
  public InvalidPaymentStateException(String message) {
    super(message);
  }
}
