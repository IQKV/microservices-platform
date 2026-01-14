package com.iqscaffold.contactservice.shared.exception;

public class ContactNotFoundException extends RuntimeException {

  public ContactNotFoundException(final String message) {
    super(message);
  }

  public ContactNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
