package org.gripday.authservice.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.gripday.authservice.domain.service.AuthenticationService;
import org.gripday.authservice.domain.service.EmailVerificationService;
import org.gripday.authservice.domain.service.UserRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler using Java 21 switch expressions and records. Provides consistent error responses across all endpoints with OpenAPI documentation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Helper to create a ProblemDetail with common properties.
   */
  private ProblemDetail problem(String type, String title, HttpStatus status, String detail, HttpServletRequest request) {
    var pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(java.net.URI.create(type));
    pd.setTitle(title);
    pd.setInstance(java.net.URI.create(request.getRequestURI()));
    pd.setProperty("path", request.getRequestURI());
    pd.setProperty("method", request.getMethod());
    pd.setProperty("correlationId", MDC.get("correlationId"));
    pd.setProperty("requestId", generateRequestId());
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Validation failed - invalid request data",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(this::createErrorDetail)
        .toList();
    var pd = problem("https://problems.pynity.com/validation-error",
        "Request validation failed",
        HttpStatus.BAD_REQUEST,
        "One or more fields contain invalid values",
        request);
    pd.setProperty("code", "VALIDATION_ERROR");
    pd.setProperty("fields", fieldErrors);
    logger.warn("Validation error: {} - {}", MDC.get("correlationId"), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      ConstraintViolationException ex, HttpServletRequest request) {
    var fieldErrors = ex.getConstraintViolations().stream()
        .map(violation -> new ErrorDetail(
            violation.getPropertyPath().toString(),
            "CONSTRAINT_VIOLATION",
            violation.getMessage(),
            violation.getInvalidValue()
        ))
        .toList();
    var pd = problem("https://problems.pynity.com/validation-error",
        "Constraint validation failed",
        HttpStatus.BAD_REQUEST,
        ex.getMessage(),
        request);
    pd.setProperty("code", "VALIDATION_ERROR");
    pd.setProperty("fields", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(AuthenticationService.AuthenticationException.class)
  @ApiResponse(
      responseCode = "401",
      description = "Authentication failed - invalid credentials or token",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationService.AuthenticationException ex, HttpServletRequest request) {
    var errorCode = determineAuthErrorCode(ex.getMessage());
    var pd = problem("https://problems.pynity.com/authentication-error",
        "Authentication failed",
        HttpStatus.UNAUTHORIZED,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    logger.warn("Authentication failed: {} - {}", MDC.get("correlationId"), ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(AuthenticationService.AccountLockedException.class)
  public ResponseEntity<ProblemDetail> handleAccountLockedException(
      AuthenticationService.AccountLockedException ex, HttpServletRequest request) {
    var pd = problem("https://problems.pynity.com/account-locked",
        "Account temporarily locked",
        HttpStatus.LOCKED,
        ex.getMessage(),
        request);
    pd.setProperty("code", "AUTH_ACCOUNT_LOCKED");
    return ResponseEntity.status(HttpStatus.LOCKED).body(pd);
  }

  @ExceptionHandler(AuthenticationService.EmailVerificationRequiredException.class)
  @ApiResponse(
      responseCode = "401",
      description = "Email verification required - user must verify email before login",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleEmailVerificationRequiredException(
      AuthenticationService.EmailVerificationRequiredException ex, HttpServletRequest request) {
    var pd = problem("https://problems.pynity.com/email-verification-required",
        "Email verification required",
        HttpStatus.UNAUTHORIZED,
        "Please check your email and click the verification link to activate your account",
        request);
    pd.setProperty("code", "EMAIL_VERIFICATION_REQUIRED");
    pd.setProperty("actions", new Actions(
        "/api/v1/auth/email/resend",
        "/api/v1/auth/email/status"
    ));
    logger.warn("Email verification required: {} - {}", MDC.get("correlationId"), ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(UserRegistrationService.UserRegistrationException.class)
  public ResponseEntity<ProblemDetail> handleUserRegistrationException(
      UserRegistrationService.UserRegistrationException ex, HttpServletRequest request) {
    var errorCode = determineRegistrationErrorCode(ex.getMessage());
    var status = errorCode.equals("USER_ALREADY_EXISTS") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    var pd = problem("https://problems.pynity.com/user-registration",
        "User registration failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - insufficient permissions",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    var pd = problem("https://problems.pynity.com/access-denied",
        "Insufficient permissions for this operation",
        HttpStatus.FORBIDDEN,
        ex.getMessage(),
        request);
    pd.setProperty("code", "AUTH_INSUFFICIENT_PERMISSIONS");
    logger.warn("Access denied: {} - {}", MDC.get("correlationId"), ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(org.gripday.authservice.domain.service.UserManagementService.UserManagementException.class)
  public ResponseEntity<ProblemDetail> handleUserManagementException(
      org.gripday.authservice.domain.service.UserManagementService.UserManagementException ex,
      HttpServletRequest request) {
    var errorCode = determineUserManagementErrorCode(ex.getMessage());
    var status = errorCode.equals("USER_ALREADY_EXISTS") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    var pd = problem("https://problems.pynity.com/user-management",
        "User management operation failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(EmailVerificationService.EmailVerificationException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Email verification failed - invalid token, rate limit, or already verified",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleEmailVerificationException(
      EmailVerificationService.EmailVerificationException ex, HttpServletRequest request) {
    var errorCode = determineEmailVerificationErrorCode(ex.getMessage());
    var status = determineEmailVerificationStatus(errorCode);
    var pd = problem("https://problems.pynity.com/email-verification",
        "Email verification failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    logger.warn("Email verification failed: {} - {}", MDC.get("correlationId"), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex, HttpServletRequest request) {
    var pd = problem("https://problems.pynity.com/internal-error",
        "Internal system error",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        request);
    pd.setProperty("code", "SYSTEM_INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }

  /**
   * Determine authentication error code using switch expression.
   */
  private String determineAuthErrorCode(String message) {
    return switch (message.toLowerCase()) {
      case String msg when msg.contains("invalid username") || msg.contains("invalid password") -> "AUTH_INVALID_CREDENTIALS";
      case String msg when msg.contains("account is disabled") -> "AUTH_ACCOUNT_DISABLED";
      case String msg when msg.contains("account is locked") -> "AUTH_ACCOUNT_LOCKED";
      case String msg when msg.contains("invalid token") || msg.contains("expired token") -> "AUTH_INVALID_TOKEN";
      case String msg when msg.contains("invalid input") -> "AUTH_INVALID_INPUT";
      case String msg when msg.contains("suspicious") || msg.contains("injection") -> "AUTH_SUSPICIOUS_ACTIVITY";
      default -> "AUTH_AUTHENTICATION_FAILED";
    };
  }

  /**
   * Determine registration error code using switch expression.
   */
  private String determineRegistrationErrorCode(String message) {
    return switch (message.toLowerCase()) {
      case String msg when msg.contains("username already exists") || msg.contains("email already exists") -> "USER_ALREADY_EXISTS";
      case String msg when msg.contains("invalid email") -> "VALIDATION_INVALID_EMAIL";
      case String msg when msg.contains("password") -> "VALIDATION_INVALID_PASSWORD";
      case String msg when msg.contains("invalid input") -> "VALIDATION_INVALID_INPUT";
      case String msg when msg.contains("suspicious") || msg.contains("injection") -> "VALIDATION_SUSPICIOUS_ACTIVITY";
      default -> "USER_REGISTRATION_FAILED";
    };
  }

  /**
   * Determine user management error code using switch expression.
   */
  private String determineUserManagementErrorCode(String message) {
    return switch (message.toLowerCase()) {
      case String msg when msg.contains("username already exists") || msg.contains("email already exists") -> "USER_ALREADY_EXISTS";
      case String msg when msg.contains("user not found") -> "USER_NOT_FOUND";
      case String msg when msg.contains("cannot delete your own account") -> "USER_SELF_DELETE_FORBIDDEN";
      case String msg when msg.contains("unknown roles") -> "USER_INVALID_ROLES";
      default -> "USER_MANAGEMENT_FAILED";
    };
  }

  /**
   * Determine email verification error code using switch expression.
   */
  private String determineEmailVerificationErrorCode(String message) {
    return switch (message.toLowerCase()) {
      case String msg when msg.contains("invalid") && msg.contains("token") -> "EMAIL_VERIFICATION_TOKEN_INVALID";
      case String msg when msg.contains("expired") && msg.contains("token") -> "EMAIL_VERIFICATION_TOKEN_EXPIRED";
      case String msg when msg.contains("already verified") -> "EMAIL_ALREADY_VERIFIED";
      case String msg when msg.contains("rate limit") -> "EMAIL_RESEND_RATE_LIMITED";
      case String msg when msg.contains("user not found") -> "USER_NOT_FOUND";
      case String msg when msg.contains("failed to send") -> "EMAIL_SEND_FAILED";
      default -> "EMAIL_VERIFICATION_FAILED";
    };
  }

  /**
   * Determine HTTP status for email verification errors.
   */
  private HttpStatus determineEmailVerificationStatus(String errorCode) {
    return switch (errorCode) {
      case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
      case "EMAIL_RESEND_RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
      case "EMAIL_SEND_FAILED" -> HttpStatus.INTERNAL_SERVER_ERROR;
      default -> HttpStatus.BAD_REQUEST;
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
  ) {

  }

  /**
   * Actions record for actionable error responses.
   */
  @Schema(
      name = "Actions",
      description = "Actionable links for error resolution"
  )
  public record Actions(
      @Schema(
          description = "Endpoint to resend verification email",
          example = "/api/v1/auth/email/resend"
      )
      String resendEmail,

      @Schema(
          description = "Endpoint to check verification status",
          example = "/api/v1/auth/email/status"
      )
      String checkStatus
  ) {

  }
}