package com.iqscaffold.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;

import com.iqscaffold.userservice.authentication.AccountLockedException;
import com.iqscaffold.userservice.authentication.AuthenticationException;
import com.iqscaffold.userservice.authentication.EmailVerificationRequiredException;
import com.iqscaffold.userservice.registration.UserRegistrationService;
import com.iqscaffold.userservice.shared.UserServiceConstants;
import com.iqscaffold.userservice.shared.exception.EmailVerificationException;
import com.iqscaffold.userservice.shared.exception.TenantContextException;
import com.iqscaffold.userservice.shared.exception.TenantContextMismatchException;
import com.iqscaffold.userservice.shared.exception.TenantManagementException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
 * Global exception handler providing centralized error handling with RFC 9457 Problem Details compliance.
 * 
 * <p>This handler implements comprehensive error handling across the entire user service, providing
 * consistent error responses, detailed problem information, and proper HTTP status codes. It follows
 * the RFC 9457 Problem Details standard for machine-readable error responses.
 * 
 * <h3>Error Handling Features</h3>
 * <ul>
 *   <li><strong>RFC 9457 Compliance</strong> - Standard Problem Details format for all errors</li>
 *   <li><strong>Centralized Handling</strong> - Single point for all exception processing</li>
 *   <li><strong>Structured Responses</strong> - Consistent error format across all endpoints</li>
 *   <li><strong>Context Enrichment</strong> - Automatic addition of request context and correlation IDs</li>
 *   <li><strong>Security Aware</strong> - Prevents information leakage in error responses</li>
 * </ul>
 * 
 * <h3>Problem Details Structure</h3>
 * <ul>
 *   <li><strong>type</strong> - URI identifying the problem type</li>
 *   <li><strong>title</strong> - Human-readable summary of the problem</li>
 *   <li><strong>status</strong> - HTTP status code</li>
 *   <li><strong>detail</strong> - Detailed explanation of the problem</li>
 *   <li><strong>instance</strong> - URI identifying the specific occurrence</li>
 *   <li><strong>Additional Properties</strong> - Context-specific error information</li>
 * </ul>
 * 
 * <h3>Handled Exception Types</h3>
 * <ul>
 *   <li><strong>Validation Errors</strong> - Bean validation and constraint violations</li>
 *   <li><strong>Authentication Errors</strong> - Login failures and token issues</li>
 *   <li><strong>Authorization Errors</strong> - Access denied and permission issues</li>
 *   <li><strong>Business Logic Errors</strong> - Domain-specific exceptions</li>
 *   <li><strong>System Errors</strong> - Database, network, and infrastructure issues</li>
 * </ul>
 * 
 * <h3>Security Considerations</h3>
 * <ul>
 *   <li><strong>Information Hiding</strong> - Sensitive details excluded from error responses</li>
 *   <li><strong>Stack Trace Protection</strong> - Stack traces never exposed to clients</li>
 *   <li><strong>Error Code Mapping</strong> - Generic error codes prevent system enumeration</li>
 *   <li><strong>Audit Integration</strong> - Security-relevant errors logged for monitoring</li>
 * </ul>
 * 
 * <h3>Context Enrichment</h3>
 * <p>All error responses automatically include:
 * <ul>
 *   <li><strong>Correlation ID</strong> - For request tracing and debugging</li>
 *   <li><strong>Request ID</strong> - Unique identifier for the specific request</li>
 *   <li><strong>Request Path</strong> - The endpoint that generated the error</li>
 *   <li><strong>HTTP Method</strong> - The HTTP method used</li>
 *   <li><strong>Timestamp</strong> - When the error occurred</li>
 * </ul>
 * 
 * <h3>Validation Error Handling</h3>
 * <ul>
 *   <li><strong>Field-Level Errors</strong> - Detailed validation messages per field</li>
 *   <li><strong>Constraint Violations</strong> - Bean validation constraint details</li>
 *   <li><strong>Internationalization</strong> - Localized error messages</li>
 *   <li><strong>Error Codes</strong> - Machine-readable error identification</li>
 * </ul>
 * 
 * <h3>OpenAPI Integration</h3>
 * <ul>
 *   <li><strong>Documented Responses</strong> - All error responses documented in OpenAPI spec</li>
 *   <li><strong>Schema Definitions</strong> - ProblemDetail schema included</li>
 *   <li><strong>Status Code Mapping</strong> - Proper HTTP status codes for each error type</li>
 * </ul>
 * 
 * <h3>Logging Strategy</h3>
 * <ul>
 *   <li><strong>Error Classification</strong> - Different log levels based on error severity</li>
 *   <li><strong>Structured Logging</strong> - JSON formatted logs with context</li>
 *   <li><strong>Correlation Tracking</strong> - Request correlation IDs in all log entries</li>
 *   <li><strong>Security Events</strong> - Special handling for security-related errors</li>
 * </ul>
 * 
 * <h3>Error Response Examples</h3>
 * <pre>{@code
 * // Validation Error Response
 * {
 *   "type": "https://problems.iqscaffold.com/validation-error",
 *   "title": "Request validation failed",
 *   "status": 400,
 *   "detail": "One or more fields contain invalid values",
 *   "instance": "/api/v1/users",
 *   "correlationId": "abc123",
 *   "requestId": "req-456",
 *   "fields": [
 *     {
 *       "field": "email",
 *       "message": "must be a valid email address",
 *       "rejectedValue": "invalid-email"
 *     }
 *   ]
 * }
 * 
 * // Authentication Error Response
 * {
 *   "type": "https://problems.iqscaffold.com/authentication-error",
 *   "title": "Authentication failed",
 *   "status": 401,
 *   "detail": "Invalid credentials provided",
 *   "instance": "/api/v1/auth/login",
 *   "correlationId": "def789"
 * }
 * }</pre>
 * 
 * <h3>Exception Mapping</h3>
 * <ul>
 *   <li><strong>400 Bad Request</strong> - Validation errors, malformed requests</li>
 *   <li><strong>401 Unauthorized</strong> - Authentication failures</li>
 *   <li><strong>403 Forbidden</strong> - Authorization failures</li>
 *   <li><strong>404 Not Found</strong> - Resource not found</li>
 *   <li><strong>409 Conflict</strong> - Business rule violations</li>
 *   <li><strong>429 Too Many Requests</strong> - Rate limiting</li>
 *   <li><strong>500 Internal Server Error</strong> - System errors</li>
 * </ul>
 * 
 * @author IQ Scaffold Team
 * @version 1.0
 * @since 1.0
 * @see ProblemDetail
 * @see RFC9457
 * @see RestControllerAdvice
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Create a standardized ProblemDetail response with comprehensive context information.
   * 
   * <p>This helper method constructs RFC 9457 compliant Problem Details responses with
   * consistent structure and automatic context enrichment. It ensures all error responses
   * follow the same format and include necessary debugging information.
   * 
   * <h4>Automatic Context Addition:</h4>
   * <ul>
   *   <li><strong>Request Path</strong> - The endpoint that generated the error</li>
   *   <li><strong>HTTP Method</strong> - The HTTP method used in the request</li>
   *   <li><strong>Correlation ID</strong> - For distributed tracing and debugging</li>
   *   <li><strong>Request ID</strong> - Unique identifier for this specific request</li>
   *   <li><strong>Timestamp</strong> - When the error occurred (implicit in response)</li>
   * </ul>
   * 
   * <h4>Problem Detail Structure:</h4>
   * <ul>
   *   <li><strong>type</strong> - URI identifying the problem category</li>
   *   <li><strong>title</strong> - Human-readable problem summary</li>
   *   <li><strong>status</strong> - HTTP status code</li>
   *   <li><strong>detail</strong> - Detailed problem description</li>
   *   <li><strong>instance</strong> - URI of the specific problem occurrence</li>
   * </ul>
   * 
   * <h4>Context Properties:</h4>
   * <ul>
   *   <li><strong>path</strong> - Request URI path for debugging</li>
   *   <li><strong>method</strong> - HTTP method for context</li>
   *   <li><strong>correlationId</strong> - Distributed tracing identifier</li>
   *   <li><strong>requestId</strong> - Unique request identifier</li>
   * </ul>
   * 
   * @param type URI identifying the problem type (e.g., "https://problems.iqscaffold.com/validation-error")
   * @param title Human-readable summary of the problem type
   * @param status HTTP status code for the response
   * @param detail Detailed explanation of this specific problem occurrence
   * @param request The HTTP request that caused the error (for context extraction)
   * @return Fully constructed ProblemDetail with all context information
   * 
   * @see ProblemDetail
   * @see HttpServletRequest
   * @see MDC
   */
  private ProblemDetail problem(String type, String title, HttpStatus status, String detail, HttpServletRequest request) {
    var pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(java.net.URI.create(type));
    pd.setTitle(title);
    pd.setInstance(java.net.URI.create(request.getRequestURI()));
    pd.setProperty("path", request.getRequestURI());
    pd.setProperty("method", request.getMethod());
    pd.setProperty("correlationId", MDC.get(UserServiceConstants.MDC.CORRELATION_ID));
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
    var pd = problem("https://problems.iqscaffold.com/validation-error",
        "Request validation failed",
        HttpStatus.BAD_REQUEST,
        "One or more fields contain invalid values",
        request);
    pd.setProperty("code", "VALIDATION_ERROR");
    pd.setProperty("fields", fieldErrors);
    logger.warn("Validation error: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
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
    var pd = problem("https://problems.iqscaffold.com/validation-error",
        "Constraint validation failed",
        HttpStatus.BAD_REQUEST,
        ex.getMessage(),
        request);
    pd.setProperty("code", "VALIDATION_ERROR");
    pd.setProperty("fields", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(AuthenticationException.class)
  @ApiResponse(
      responseCode = "401",
      description = "Authentication failed - invalid credentials or token",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    var errorCode = determineAuthErrorCode(ex.getMessage());
    var pd = problem("https://problems.iqscaffold.com/authentication-error",
        "Authentication failed",
        HttpStatus.UNAUTHORIZED,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    logger.warn("Authentication failed: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<ProblemDetail> handleAccountLockedException(
      AccountLockedException ex, HttpServletRequest request) {
    var pd = problem("https://problems.iqscaffold.com/account-locked",
        "Account temporarily locked",
        HttpStatus.LOCKED,
        ex.getMessage(),
        request);
    pd.setProperty("code", "AUTH_ACCOUNT_LOCKED");
    return ResponseEntity.status(HttpStatus.LOCKED).body(pd);
  }

  @ExceptionHandler(EmailVerificationRequiredException.class)
  @ApiResponse(
      responseCode = "401",
      description = "Email verification required - user must verify email before login",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleEmailVerificationRequiredException(
      EmailVerificationRequiredException ex, HttpServletRequest request) {
    var pd = problem("https://problems.iqscaffold.com/email-verification-required",
        "Email verification required",
        HttpStatus.UNAUTHORIZED,
        "Please check your email and click the verification link to activate your account",
        request);
    pd.setProperty("code", "EMAIL_VERIFICATION_REQUIRED");
    pd.setProperty("actions", new Actions(
        "/api/v1/auth/email/resend",
        "/api/v1/auth/email/status"
    ));
    logger.warn("Email verification required: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(UserRegistrationService.UserRegistrationException.class)
  public ResponseEntity<ProblemDetail> handleUserRegistrationException(
      UserRegistrationService.UserRegistrationException ex, HttpServletRequest request) {
    var errorCode = determineRegistrationErrorCode(ex.getMessage());
    var status = errorCode.equals("USER_ALREADY_EXISTS") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    var pd = problem("https://problems.iqscaffold.com/user-registration",
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
    var pd = problem("https://problems.iqscaffold.com/access-denied",
        "Insufficient permissions for this operation",
        HttpStatus.FORBIDDEN,
        ex.getMessage(),
        request);
    pd.setProperty("code", "AUTH_INSUFFICIENT_PERMISSIONS");
    logger.warn("Access denied: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(com.iqscaffold.userservice.usermanagement.UserManagementService.UserManagementException.class)
  public ResponseEntity<ProblemDetail> handleUserManagementException(
      com.iqscaffold.userservice.usermanagement.UserManagementService.UserManagementException ex,
      HttpServletRequest request) {
    var errorCode = determineUserManagementErrorCode(ex.getMessage());
    var status = errorCode.equals("USER_ALREADY_EXISTS") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    var pd = problem("https://problems.iqscaffold.com/user-management",
        "User management operation failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(EmailVerificationException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Email verification failed - invalid token, rate limit, or already verified",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleEmailVerificationException(
      EmailVerificationException ex, HttpServletRequest request) {
    var errorCode = determineEmailVerificationErrorCode(ex.getMessage());
    var status = determineEmailVerificationStatus(errorCode);
    var pd = problem("https://problems.iqscaffold.com/email-verification",
        "Email verification failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    logger.warn("Email verification failed: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(TenantManagementException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Tenant management operation failed",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleTenantManagementException(
      TenantManagementException ex, HttpServletRequest request) {
    var errorCode = determineTenantManagementErrorCode(ex);
    var status = determineTenantManagementStatus(ex);
    var pd = problem("https://problems.iqscaffold.com/tenant-management",
        "Tenant management failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);
    logger.warn("Tenant management failed: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(TenantContextException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Tenant context operation failed",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleTenantContextException(
      TenantContextException ex, HttpServletRequest request) {
    var pd = problem("https://problems.iqscaffold.com/tenant-context",
        "Tenant context error",
        HttpStatus.BAD_REQUEST,
        ex.getMessage(),
        request);
    pd.setProperty("code", "TENANT_CONTEXT_INVALID");
    logger.warn("Tenant context error: {} - {}", MDC.get(UserServiceConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(TenantContextMismatchException.class)
  @ApiResponse(
      responseCode = "403",
      description = "Tenant context mismatch - security violation",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleTenantContextMismatchException(
      TenantContextMismatchException ex, HttpServletRequest request) {
    var pd = problem("https://problems.iqscaffold.com/tenant-context-mismatch",
        "Tenant context mismatch",
        HttpStatus.FORBIDDEN,
        ex.getMessage(),
        request);
    pd.setProperty("code", "TENANT_CONTEXT_MISMATCH");
    pd.setProperty("currentTenant", ex.getCurrentTenantId());
    pd.setProperty("entityTenant", ex.getEntityTenantId());
    logger.error("Tenant context mismatch: {} - current: {}, entity: {}",
        MDC.get(UserServiceConstants.MDC.CORRELATION_ID),
        ex.getCurrentTenantId(),
        ex.getEntityTenantId());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex, HttpServletRequest request) {
    var pd = problem("https://problems.iqscaffold.com/internal-error",
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
    return switch (message.toLowerCase(java.util.Locale.ROOT)) {
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
    return switch (message.toLowerCase(java.util.Locale.ROOT)) {
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
    return switch (message.toLowerCase(java.util.Locale.ROOT)) {
      case String msg when msg.contains("username already exists") || msg.contains("email already exists") -> "USER_ALREADY_EXISTS";
      case String msg when msg.contains("user not found") -> "USER_NOT_FOUND";
      case String msg when msg.contains("cannot delete your own account") -> "USER_SELF_DELETE_FORBIDDEN";
      case String msg when msg.contains("unknown authorities") -> "USER_INVALID_AUTHORITIES";
      default -> "USER_MANAGEMENT_FAILED";
    };
  }

  /**
   * Determine email verification error code using switch expression.
   */
  private String determineEmailVerificationErrorCode(String message) {
    return switch (message.toLowerCase(java.util.Locale.ROOT)) {
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
   * Determine tenant management error code using switch expression.
   */
  private String determineTenantManagementErrorCode(TenantManagementException ex) {
    return switch (ex) {
      case TenantManagementException.TenantAlreadyExistsException e -> "TENANT_ALREADY_EXISTS";
      case TenantManagementException.TenantNotFoundException e -> "TENANT_NOT_FOUND";
      case TenantManagementException.DomainAlreadyExistsException e -> "TENANT_DOMAIN_ALREADY_EXISTS";
      case TenantManagementException.TenantHasUsersException e -> "TENANT_HAS_USERS";
      case TenantManagementException.SchemaProvisioningException e -> "TENANT_SCHEMA_PROVISIONING_FAILED";
      default -> "TENANT_MANAGEMENT_FAILED";
    };
  }

  /**
   * Determine HTTP status for tenant management errors.
   */
  private HttpStatus determineTenantManagementStatus(TenantManagementException ex) {
    return switch (ex) {
      case TenantManagementException.TenantNotFoundException e -> HttpStatus.NOT_FOUND;
      case TenantManagementException.TenantAlreadyExistsException e -> HttpStatus.CONFLICT;
      case TenantManagementException.DomainAlreadyExistsException e -> HttpStatus.CONFLICT;
      case TenantManagementException.TenantHasUsersException e -> HttpStatus.CONFLICT;
      case TenantManagementException.SchemaProvisioningException e -> HttpStatus.INTERNAL_SERVER_ERROR;
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
