package org.gripday.bookstore.presentation.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.gripday.bookstore.domain.dto.ErrorResponse;
import org.gripday.bookstore.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class BookstoreExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BookstoreExceptionHandler.class);
    
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(
            BookNotFoundException ex, 
            HttpServletRequest request) {
        
        logger.warn("Book not found: {}", ex.getMessage());
        
        var error = ErrorResponse.of(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            "The requested book could not be found",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex, 
            HttpServletRequest request) {
        
        logger.warn("Category not found: {}", ex.getMessage());
        
        var error = ErrorResponse.of(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            "The requested category could not be found",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientInventory(
            InsufficientInventoryException ex, 
            HttpServletRequest request) {
        
        logger.warn("Insufficient inventory: {}", ex.getMessage());
        
        var error = ErrorResponse.of(
            "DOMAIN_INSUFFICIENT_INVENTORY",
            ex.getMessage(),
            "Not enough inventory available for the requested operation",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIsbn(
            DuplicateIsbnException ex, 
            HttpServletRequest request) {
        
        logger.warn("Duplicate ISBN: {}", ex.getMessage());
        
        var error = ErrorResponse.of(
            "DOMAIN_DUPLICATE_ISBN",
            ex.getMessage(),
            "A book with this ISBN already exists in the catalog",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOperation(
            UnauthorizedOperationException ex, 
            HttpServletRequest request) {
        
        logger.warn("Unauthorized operation: {}", ex.getMessage());
        
        var error = ErrorResponse.of(
            "AUTH_INSUFFICIENT_PRIVILEGES",
            ex.getMessage(),
            "You do not have sufficient privileges to perform this operation",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        logger.warn("Validation failed: {}", ex.getMessage());
        
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldError)
            .toList();
        
        var error = ErrorResponse.of(
            "VALIDATION_ERROR",
            "Request validation failed",
            "One or more fields contain invalid values",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId())
         .withFields(fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        var fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> new ErrorResponse.FieldError(
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                violation.getMessage()
            ))
            .toList();
        
        var error = ErrorResponse.of(
            "VALIDATION_ERROR",
            "Request validation failed",
            "One or more constraints were violated",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId())
         .withFields(fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, 
            HttpServletRequest request) {
        
        logger.warn("Type mismatch: {}", ex.getMessage());
        
        var fieldError = new ErrorResponse.FieldError(
            ex.getName(),
            ex.getValue(),
            "Invalid value type. Expected: " + ex.getRequiredType().getSimpleName()
        );
        
        var error = ErrorResponse.of(
            "VALIDATION_ERROR",
            "Invalid parameter type",
            "The provided parameter value has an incorrect type",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId())
         .withFields(List.of(fieldError));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        var error = ErrorResponse.of(
            "VALIDATION_ERROR",
            ex.getMessage(),
            "The provided argument is invalid",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        logger.error("Unexpected error occurred", ex);
        
        var error = ErrorResponse.of(
            "SYSTEM_INTERNAL_ERROR",
            "An unexpected error occurred",
            "Please try again later or contact support if the problem persists",
            request.getRequestURI(),
            request.getMethod()
        ).withCorrelationId(getCorrelationId())
         .withRequestId(generateRequestId());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private ErrorResponse.FieldError mapFieldError(FieldError fieldError) {
        return new ErrorResponse.FieldError(
            fieldError.getField(),
            fieldError.getRejectedValue(),
            fieldError.getDefaultMessage()
        );
    }
    
    private String getCorrelationId() {
        var correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : generateCorrelationId();
    }
    
    private String generateCorrelationId() {
        return "corr-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }
}