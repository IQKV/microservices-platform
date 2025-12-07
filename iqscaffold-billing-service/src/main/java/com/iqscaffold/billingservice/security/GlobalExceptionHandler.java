package com.iqscaffold.billingservice.security;

import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import com.iqscaffold.billingservice.shared.exception.TenantContextException;
import com.iqscaffold.billingservice.shared.exception.UsageException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
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
 * Global exception handler for the Billing Service using Java 21 switch expressions and records.
 * Provides consistent ProblemDetail error responses across all endpoints with OpenAPI documentation.
 *
 * <p>This handler follows RFC 7807 (Problem Details for HTTP APIs) and includes:
 * <ul>
 *   <li>Structured error responses with correlation IDs</li>
 *   <li>Field-level validation error details</li>
 *   <li>Domain-specific error codes</li>
 *   <li>Appropriate HTTP status codes</li>
 *   <li>OpenAPI documentation for error responses</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Helper to create a ProblemDetail with common properties.
   *
   * @param type URI reference that identifies the problem type
   * @param title short, human-readable summary of the problem
   * @param status HTTP status code
   * @param detail human-readable explanation specific to this occurrence
   * @param request HTTP servlet request for context
   * @return configured ProblemDetail instance
   */
  private ProblemDetail problem(String type, String title, HttpStatus status, String detail, HttpServletRequest request) {
    var pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(java.net.URI.create(type));
    pd.setTitle(title);
    pd.setInstance(java.net.URI.create(request.getRequestURI()));
    pd.setProperty("path", request.getRequestURI());
    pd.setProperty("method", request.getMethod());
    pd.setProperty("correlationId", MDC.get(BillingConstants.MDC.CORRELATION_ID));
    pd.setProperty("requestId", generateRequestId());
    return pd;
  }

  /**
   * Handle validation errors from @Valid annotations.
   */
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
    logger.warn("Validation error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  /**
   * Handle constraint violation errors.
   */
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

  /**
   * Handle subscription-related exceptions.
   */
  @ExceptionHandler(SubscriptionException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Subscription operation failed",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleSubscriptionException(
      SubscriptionException ex, HttpServletRequest request) {
    var errorCode = determineSubscriptionErrorCode(ex);
    var status = determineSubscriptionStatus(ex);
    var pd = problem("https://problems.iqscaffold.com/subscription-error",
        "Subscription operation failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);

    // Add specific properties for certain exception types
    if (ex instanceof SubscriptionException.SubscriptionNotFoundException notFoundEx) {
      pd.setProperty("subscriptionId", notFoundEx.getSubscriptionId());
    } else if (ex instanceof SubscriptionException.SubscriptionAlreadyExistsException existsEx) {
      pd.setProperty("tenantId", existsEx.getTenantId());
    } else if (ex instanceof SubscriptionException.InvalidSubscriptionStateException stateEx) {
      pd.setProperty("currentState", stateEx.getCurrentState());
      pd.setProperty("targetState", stateEx.getTargetState());
    }

    logger.warn("Subscription error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  /**
   * Handle payment-related exceptions.
   */
  @ExceptionHandler(PaymentException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Payment operation failed",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handlePaymentException(
      PaymentException ex, HttpServletRequest request) {
    var errorCode = determinePaymentErrorCode(ex);
    var status = determinePaymentStatus(ex);
    var pd = problem("https://problems.iqscaffold.com/payment-error",
        "Payment operation failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);

    // Add specific properties for certain exception types
    if (ex instanceof PaymentException.PaymentFailedException failedEx) {
      pd.setProperty("paymentId", failedEx.getPaymentId());
      pd.setProperty("reason", failedEx.getReason());
    } else if (ex instanceof PaymentException.PaymentMethodNotFoundException notFoundEx) {
      pd.setProperty("paymentMethodId", notFoundEx.getPaymentMethodId());
    } else if (ex instanceof PaymentException.InvalidPaymentMethodException invalidEx) {
      pd.setProperty("paymentMethodId", invalidEx.getPaymentMethodId());
      pd.setProperty("reason", invalidEx.getReason());
    }

    logger.warn("Payment error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  /**
   * Handle invoice-related exceptions.
   */
  @ExceptionHandler(InvoiceException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Invoice operation failed",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleInvoiceException(
      InvoiceException ex, HttpServletRequest request) {
    var errorCode = determineInvoiceErrorCode(ex);
    var status = determineInvoiceStatus(ex);
    var pd = problem("https://problems.iqscaffold.com/invoice-error",
        "Invoice operation failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);

    // Add specific properties for certain exception types
    if (ex instanceof InvoiceException.InvoiceNotFoundException notFoundEx) {
      pd.setProperty("invoiceId", notFoundEx.getInvoiceId());
    } else if (ex instanceof InvoiceException.InvoiceAlreadyPaidException paidEx) {
      pd.setProperty("invoiceId", paidEx.getInvoiceId());
    } else if (ex instanceof InvoiceException.InvalidInvoiceStateException stateEx) {
      pd.setProperty("currentState", stateEx.getCurrentState());
      pd.setProperty("requiredState", stateEx.getRequiredState());
    }

    logger.warn("Invoice error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  /**
   * Handle plan-related exceptions.
   */
  @ExceptionHandler(PlanException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Plan operation failed",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handlePlanException(
      PlanException ex, HttpServletRequest request) {
    var errorCode = determinePlanErrorCode(ex);
    var status = determinePlanStatus(ex);
    var pd = problem("https://problems.iqscaffold.com/plan-error",
        "Plan operation failed",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);

    // Add specific properties for certain exception types
    if (ex instanceof PlanException.PlanNotFoundException notFoundEx) {
      pd.setProperty("planId", notFoundEx.getPlanId());
    } else if (ex instanceof PlanException.InvalidPlanTransitionException transitionEx) {
      pd.setProperty("fromPlan", transitionEx.getFromPlan());
      pd.setProperty("toPlan", transitionEx.getToPlan());
    }

    logger.warn("Plan error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  /**
   * Handle usage and quota-related exceptions.
   */
  @ExceptionHandler(UsageException.class)
  @ApiResponse(
      responseCode = "429",
      description = "Usage quota exceeded",
      content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
      )
  )
  public ResponseEntity<ProblemDetail> handleUsageException(
      UsageException ex, HttpServletRequest request) {
    var errorCode = determineUsageErrorCode(ex);
    var status = determineUsageStatus(ex);
    var pd = problem("https://problems.iqscaffold.com/usage-error",
        "Usage quota exceeded",
        status,
        ex.getMessage(),
        request);
    pd.setProperty("code", errorCode);

    // Add specific properties for quota exceeded exceptions
    if (ex instanceof UsageException.QuotaExceededException quotaEx) {
      pd.setProperty("metricType", quotaEx.getMetricType());
      pd.setProperty("limit", quotaEx.getLimit());
      pd.setProperty("used", quotaEx.getUsed());
      pd.setProperty("actions", new QuotaActions(
          "/api/v1/billing/subscriptions/upgrade",
          "/api/v1/billing/usage/current"
      ));
    } else if (ex instanceof UsageException.UsageLimitExceededException limitEx) {
      pd.setProperty("metricType", limitEx.getMetricType());
      pd.setProperty("limit", limitEx.getLimit());
    }

    logger.warn("Usage error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(status).body(pd);
  }

  /**
   * Handle tenant context exceptions.
   */
  @ExceptionHandler(TenantContextException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Tenant context error",
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
    logger.warn("Tenant context error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  /**
   * Handle access denied exceptions.
   */
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
    logger.warn("Access denied: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  /**
   * Handle all other exceptions.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex, HttpServletRequest request) {
    var pd = problem("https://problems.iqscaffold.com/internal-error",
        "Internal system error",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        request);
    pd.setProperty("code", "SYSTEM_INTERNAL_ERROR");
    logger.error("Unexpected error: {} - {}", MDC.get(BillingConstants.MDC.CORRELATION_ID), ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }

  /**
   * Determine subscription error code using switch expression.
   */
  private String determineSubscriptionErrorCode(SubscriptionException ex) {
    return switch (ex) {
      case SubscriptionException.SubscriptionNotFoundException e -> "SUBSCRIPTION_NOT_FOUND";
      case SubscriptionException.SubscriptionAlreadyExistsException e -> "SUBSCRIPTION_ALREADY_EXISTS";
      case SubscriptionException.InvalidSubscriptionStateException e -> "INVALID_SUBSCRIPTION_STATE";
      case SubscriptionException.TrialNotEligibleException e -> "TRIAL_NOT_ELIGIBLE";
      case SubscriptionException.PlanDoesNotOfferTrialException e -> "PLAN_NO_TRIAL";
      case SubscriptionException.InvalidPaymentMethodException e -> "INVALID_PAYMENT_METHOD";
      case SubscriptionException.PaymentMethodMismatchException e -> "PAYMENT_METHOD_MISMATCH";
      default -> "SUBSCRIPTION_ERROR";
    };
  }

  /**
   * Determine HTTP status for subscription errors.
   */
  private HttpStatus determineSubscriptionStatus(SubscriptionException ex) {
    return switch (ex) {
      case SubscriptionException.SubscriptionNotFoundException e -> HttpStatus.NOT_FOUND;
      case SubscriptionException.SubscriptionAlreadyExistsException e -> HttpStatus.CONFLICT;
      case SubscriptionException.InvalidSubscriptionStateException e -> HttpStatus.CONFLICT;
      default -> HttpStatus.BAD_REQUEST;
    };
  }

  /**
   * Determine payment error code using switch expression.
   */
  private String determinePaymentErrorCode(PaymentException ex) {
    return switch (ex) {
      case PaymentException.PaymentFailedException e -> "PAYMENT_FAILED";
      case PaymentException.PaymentMethodNotFoundException e -> "PAYMENT_METHOD_NOT_FOUND";
      case PaymentException.InvalidPaymentMethodException e -> "INVALID_PAYMENT_METHOD";
      default -> "PAYMENT_ERROR";
    };
  }

  /**
   * Determine HTTP status for payment errors.
   */
  private HttpStatus determinePaymentStatus(PaymentException ex) {
    return switch (ex) {
      case PaymentException.PaymentMethodNotFoundException e -> HttpStatus.NOT_FOUND;
      case PaymentException.PaymentFailedException e -> HttpStatus.PAYMENT_REQUIRED;
      default -> HttpStatus.BAD_REQUEST;
    };
  }

  /**
   * Determine invoice error code using switch expression.
   */
  private String determineInvoiceErrorCode(InvoiceException ex) {
    return switch (ex) {
      case InvoiceException.InvoiceNotFoundException e -> "INVOICE_NOT_FOUND";
      case InvoiceException.InvoiceAlreadyPaidException e -> "INVOICE_ALREADY_PAID";
      case InvoiceException.SubscriptionNotFoundException e -> "SUBSCRIPTION_NOT_FOUND";
      case InvoiceException.InvalidInvoiceStateException e -> "INVALID_INVOICE_STATE";
      default -> "INVOICE_ERROR";
    };
  }

  /**
   * Determine HTTP status for invoice errors.
   */
  private HttpStatus determineInvoiceStatus(InvoiceException ex) {
    return switch (ex) {
      case InvoiceException.InvoiceNotFoundException e -> HttpStatus.NOT_FOUND;
      case InvoiceException.SubscriptionNotFoundException e -> HttpStatus.NOT_FOUND;
      case InvoiceException.InvoiceAlreadyPaidException e -> HttpStatus.CONFLICT;
      case InvoiceException.InvalidInvoiceStateException e -> HttpStatus.CONFLICT;
      default -> HttpStatus.BAD_REQUEST;
    };
  }

  /**
   * Determine plan error code using switch expression.
   */
  private String determinePlanErrorCode(PlanException ex) {
    return switch (ex) {
      case PlanException.PlanNotFoundException e -> "PLAN_NOT_FOUND";
      case PlanException.InvalidPlanTransitionException e -> "INVALID_PLAN_TRANSITION";
      default -> "PLAN_ERROR";
    };
  }

  /**
   * Determine HTTP status for plan errors.
   */
  private HttpStatus determinePlanStatus(PlanException ex) {
    return switch (ex) {
      case PlanException.PlanNotFoundException e -> HttpStatus.NOT_FOUND;
      case PlanException.InvalidPlanTransitionException e -> HttpStatus.CONFLICT;
      default -> HttpStatus.BAD_REQUEST;
    };
  }

  /**
   * Determine usage error code using switch expression.
   */
  private String determineUsageErrorCode(UsageException ex) {
    return switch (ex) {
      case UsageException.QuotaExceededException e -> "QUOTA_EXCEEDED";
      case UsageException.UsageLimitExceededException e -> "USAGE_LIMIT_EXCEEDED";
      default -> "USAGE_ERROR";
    };
  }

  /**
   * Determine HTTP status for usage errors.
   */
  private HttpStatus determineUsageStatus(UsageException ex) {
    return switch (ex) {
      case UsageException.QuotaExceededException e -> HttpStatus.TOO_MANY_REQUESTS;
      case UsageException.UsageLimitExceededException e -> HttpStatus.TOO_MANY_REQUESTS;
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
          example = "planId"
      )
      String field,

      @Schema(
          description = "Validation error code",
          example = "NotBlank"
      )
      String code,

      @Schema(
          description = "Human-readable error message",
          example = "Plan ID must not be blank"
      )
      String message,

      @Schema(
          description = "The value that was rejected",
          example = ""
      )
      Object rejectedValue
  ) {

  }

  /**
   * Actions record for quota exceeded errors.
   */
  @Schema(
      name = "QuotaActions",
      description = "Actionable links for quota resolution"
  )
  public record QuotaActions(
      @Schema(
          description = "Endpoint to upgrade subscription",
          example = "/api/v1/billing/subscriptions/upgrade"
      )
      String upgradeSubscription,

      @Schema(
          description = "Endpoint to check current usage",
          example = "/api/v1/billing/usage/current"
      )
      String checkUsage
  ) {

  }
}
