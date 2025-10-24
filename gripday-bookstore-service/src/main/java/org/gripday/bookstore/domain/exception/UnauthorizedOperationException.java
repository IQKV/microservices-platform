package org.gripday.bookstore.domain.exception;

public class UnauthorizedOperationException extends RuntimeException {
    
    public UnauthorizedOperationException(String operation) {
        super("Unauthorized to perform operation: " + operation);
    }
    
    public UnauthorizedOperationException(String operation, String requiredRole) {
        super("Unauthorized to perform operation: " + operation + ". Required role: " + requiredRole);
    }
}