package org.gripday.authservice.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.gripday.authservice.domain.service.AuthenticationService;
import org.gripday.authservice.domain.service.UserRegistrationService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Global exception handler using Java 21 switch expressions and records.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::createErrorDetail)
            .toList();
            
        var errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            "One or more fields contain invalid values",
            request,
            fieldErrors
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        var fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> new ErrorDetail(
                violation.getPropertyPath().toString(),
                "CONSTRAINT_VIOLATION",
                violation.getMessage(),
                violation.getInvalidValue()
            ))
            .toList();
            
        var errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            "Constraint validation failed",
            ex.getMessage(),
            request,
            fieldErrors
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(AuthenticationService.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationService.AuthenticationException ex, HttpServletRequest request) {
        
        var errorCode = determineAuthErrorCode(ex.getMessage());
        var errorResponse = createErrorResponse(
            errorCode,
            "Authentication failed",
            ex.getMessage(),
            request,
            List.of()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(UserRegistrationService.UserRegistrationException.class)
    public ResponseEntity<ApiError> handleUserRegistrationException(
            UserRegistrationService.UserRegistrationException ex, HttpServletRequest request) {
        
        var errorCode = determineRegistrationErrorCode(ex.getMessage());
        var status = errorCode.equals("USER_ALREADY_EXISTS") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        
        var errorResponse = createErrorResponse(
            errorCode,
            "User registration failed",
            ex.getMessage(),
            request,
            List.of()
        );
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        var errorResponse = createErrorResponse(
            "AUTH_INSUFFICIENT_PERMISSIONS",
            "Insufficient permissions for this operation",
            ex.getMessage(),
            request,
            List.of()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    @ExceptionHandler(org.gripday.authservice.domain.service.UserManagementService.UserManagementException.class)
    public ResponseEntity<ApiError> handleUserManagementException(
            org.gripday.authservice.domain.service.UserManagementService.UserManagementException ex, 
            HttpServletRequest request) {
        
        var errorCode = determineUserManagementErrorCode(ex.getMessage());
        var status = errorCode.equals("USER_ALREADY_EXISTS") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        
        var errorResponse = createErrorResponse(
            errorCode,
            "User management operation failed",
            ex.getMessage(),
            request,
            List.of()
        );
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        var errorResponse = createErrorResponse(
            "SYSTEM_INTERNAL_ERROR",
            "Internal system error",
            "An unexpected error occurred",
            request,
            List.of()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Determine authentication error code using switch expression.
     */
    private String determineAuthErrorCode(String message) {
        return switch (message.toLowerCase()) {
            case String msg when msg.contains("invalid username") || msg.contains("invalid password") -> 
                "AUTH_INVALID_CREDENTIALS";
            case String msg when msg.contains("account is disabled") -> 
                "AUTH_ACCOUNT_DISABLED";
            case String msg when msg.contains("account is locked") -> 
                "AUTH_ACCOUNT_LOCKED";
            case String msg when msg.contains("invalid token") || msg.contains("expired token") -> 
                "AUTH_INVALID_TOKEN";
            default -> "AUTH_AUTHENTICATION_FAILED";
        };
    }
    
    /**
     * Determine registration error code using switch expression.
     */
    private String determineRegistrationErrorCode(String message) {
        return switch (message.toLowerCase()) {
            case String msg when msg.contains("username already exists") || msg.contains("email already exists") -> 
                "USER_ALREADY_EXISTS";
            case String msg when msg.contains("invalid email") -> 
                "VALIDATION_INVALID_EMAIL";
            case String msg when msg.contains("password") -> 
                "VALIDATION_INVALID_PASSWORD";
            default -> "USER_REGISTRATION_FAILED";
        };
    }
    
    /**
     * Determine user management error code using switch expression.
     */
    private String determineUserManagementErrorCode(String message) {
        return switch (message.toLowerCase()) {
            case String msg when msg.contains("username already exists") || msg.contains("email already exists") -> 
                "USER_ALREADY_EXISTS";
            case String msg when msg.contains("user not found") -> 
                "USER_NOT_FOUND";
            case String msg when msg.contains("cannot delete your own account") -> 
                "USER_SELF_DELETE_FORBIDDEN";
            case String msg when msg.contains("unknown roles") -> 
                "USER_INVALID_ROLES";
            default -> "USER_MANAGEMENT_FAILED";
        };
    }
    
    /**
     * Create error detail from field error.
     */
    private ErrorDetail createErrorDetail(FieldError fieldError) {
        return new ErrorDetail(
            fieldError.getField(),
            fieldError.getCode(),
            fieldError.getDefaultMessage(),
            fieldError.getRejectedValue()
        );
    }
    
    /**
     * Create standardized error response.
     */
    private ApiError createErrorResponse(String code, String message, String details, 
                                       HttpServletRequest request, List<ErrorDetail> fields) {
        return new ApiError(
            code,
            message,
            details,
            Instant.now(),
            request.getRequestURI(),
            request.getMethod(),
            MDC.get("correlationId"),
            generateRequestId(),
            fields
        );
    }
    
    /**
     * Generate unique request ID.
     */
    private String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Error detail record for field-level errors.
     */
    public record ErrorDetail(
        String field,
        String code,
        String message,
        Object rejectedValue
    ) {}
    
    /**
     * API error response record.
     */
    public record ApiError(
        String code,
        String message,
        String details,
        Instant timestamp,
        String path,
        String method,
        String correlationId,
        String requestId,
        List<ErrorDetail> fields
    ) {}
}