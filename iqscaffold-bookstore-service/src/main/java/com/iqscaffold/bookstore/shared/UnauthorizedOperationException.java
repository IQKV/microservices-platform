package com.iqscaffold.bookstore.shared;

public class UnauthorizedOperationException extends RuntimeException {

  public UnauthorizedOperationException(final String operation) {
    super("Unauthorized to perform operation: " + operation);
  }

  public UnauthorizedOperationException(final String operation, final String requiredRole) {
    super("Unauthorized to perform operation: " + operation + ". Required role: " + requiredRole);
  }
}
