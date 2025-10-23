package org.gripday.authservice.presentation.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.gripday.authservice.domain.service.AuthenticationService;
import org.gripday.authservice.domain.service.UserRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Provides consistent error responses across all endpoints with comprehensive OpenAPI documentation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed - invalid request data",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(
                name = "Validation Error",
                summary = "Field validation failure",
                value = """
                {
                  "code": "VALIDATION_ERROR",
                  "message": "Request validation failed",
                  "details": "One or more fields contain invalid values",
                  "timestamp": "2024-01-15T10:30:00Z",
                  "path": "/api/v1/auth/signup",
                  "method": "POST",
                  "correlationId": "abc123-def456-ghi789",
                  "requestId": "req-001-2024",
                  "fields": [
                    {
                      "field": "email",
                      "code": "Email",
                      "message": "Email must be valid",
                      "rejectedValue": "invalid-email"
                    }
                  ]
                }
                """
            )
        )
    )
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
        
        logger.warn("Validation error: {} - {}", MDC.get("correlationId"), ex.getMessage());
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
    @ApiResponse(
        responseCode = "401",
        description = "Authentication failed - invalid credentials or token",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(
                name = "Authentication Failed",
                summary = "Invalid credentials",
                value = """
                {
                  "code": "AUTH_INVALID_CREDENTIALS",
                  "message": "Authentication failed",
                  "details": "Invalid username or password",
                  "timestamp": "2024-01-15T10:30:00Z",
                  "path": "/api/v1/auth/login",
                  "method": "POST",
                  "correlationId": "abc123-def456-ghi789",
                  "requestId": "req-001-2024",
                  "fields": []
                }
                """
            )
        )
    )
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
        
        logger.warn("Authentication failed: {} - {}", MDC.get("correlationId"), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(AuthenticationService.AccountLockedException.class)
    public ResponseEntity<ApiError> handleAccountLockedException(
            AuthenticationService.AccountLockedException ex, HttpServletRequest request) {
        
        var errorResponse = createErrorResponse(
            "AUTH_ACCOUNT_LOCKED",
            "Account temporarily locked",
            ex.getMessage(),
            request,
            List.of()
        );
        
        return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
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
    @ApiResponse(
        responseCode = "403",
        description = "Access denied - insufficient permissions",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(
                name = "Access Denied",
                summary = "Insufficient permissions",
                value = """
                {
                  "code": "AUTH_INSUFFICIENT_PERMISSIONS",
                  "message": "Insufficient permissions for this operation",
                  "details": "User does not have required ADMIN role",
                  "timestamp": "2024-01-15T10:30:00Z",
                  "path": "/api/v1/users",
                  "method": "GET",
                  "correlationId": "abc123-def456-ghi789",
                  "requestId": "req-001-2024",
                  "fields": []
                }
                """
            )
        )
    )
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        var errorResponse = createErrorResponse(
            "AUTH_INSUFFICIENT_PERMISSIONS",
            "Insufficient permissions for this operation",
            ex.getMessage(),
            request,
            List.of()
        );
        
        logger.warn("Access denied: {} - {}", MDC.get("correlationId"), ex.getMessage());
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
            case String msg when msg.contains("invalid input") -> 
                "AUTH_INVALID_INPUT";
            case String msg when msg.contains("suspicious") || msg.contains("injection") -> 
                "AUTH_SUSPICIOUS_ACTIVITY";
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
            case String msg when msg.contains("invalid input") -> 
                "VALIDATION_INVALID_INPUT";
            case String msg when msg.contains("suspicious") || msg.contains("injection") -> 
                "VALIDATION_SUSPICIOUS_ACTIVITY";
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
    @Schema(
        name = "ErrorDetail",
        description = "Field-specific validation error details"
    )
    public record ErrorDetail(
        @Schema(
            description = "Name of the field that failed validation",
            example = "email"
        )
        String field,
        
        @Schema(
            description = "Validation error code",
            example = "Email"
        )
        String code,
        
        @Schema(
            description = "Human-readable error message",
            example = "Email must be valid"
        )
        String message,
        
        @Schema(
            description = "The value that was rejected",
            example = "invalid-email"
        )
        Object rejectedValue
    ) {}
    
    /**
     * API error response record.
     */
    @Schema(
        name = "ApiError",
        description = "Standard error response format with correlation tracking"
    )
    public record ApiError(
        @Schema(
            description = "Specific error code for programmatic handling",
            example = "VALIDATION_ERROR"
        )
        String code,
        
        @Schema(
            description = "Human-readable error message",
            example = "Request validation failed"
        )
        String message,
        
        @Schema(
            description = "Additional error details and context",
            example = "One or more fields contain invalid values"
        )
        String details,
        
        @Schema(
            description = "Timestamp when the error occurred",
            example = "2024-01-15T10:30:00Z",
            format = "date-time"
        )
        Instant timestamp,
        
        @Schema(
            description = "Request path that caused the error",
            example = "/api/v1/auth/login"
        )
        String path,
        
        @Schema(
            description = "HTTP method used in the request",
            example = "POST"
        )
        String method,
        
        @Schema(
            description = "Correlation ID for distributed request tracing",
            example = "abc123-def456-ghi789"
        )
        String correlationId,
        
        @Schema(
            description = "Unique request identifier",
            example = "req-001-2024"
        )
        String requestId,
        
        @Schema(
            description = "List of field-specific validation errors"
        )
        List<ErrorDetail> fields
    ) {}
}