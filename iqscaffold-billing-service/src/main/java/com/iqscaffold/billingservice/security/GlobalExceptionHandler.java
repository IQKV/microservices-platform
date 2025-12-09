package com.iqscaffold.billingservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;

import com.iqscaffold.billingservice.shared.exception.BillingException;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import com.iqscaffold.billingservice.shared.exception.UsageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Billing Service.
 *
 * <p>Provides centralized exception handling across all REST controllers.
 * Translates domain exceptions into appropriate HTTP responses using
 * RFC 7807 Problem Details specification.
 *
 * <p>Problem Detail Response Format (RFC 7807):
 * <pre>
 * {
 *   "type": "https://api.iqscaffold.com/errors/quota-exceeded",
 *   "title": "Quota Exceeded",
 *   "status": 429,
 *   "detail": "Quota exceeded for API_CALLS: used 1000 of 500",
 *   "instance": "/api/v1/billing/usage/check-quota",
 *   "errorCode": "BILLING_009",
 *   "metricType": "API_CALLS",
 *   "limit": 500,
 *   "used": 1000,
 *   "upgradeUrl": "/api/v1/billing/portal/upgrade"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String ERROR_TYPE_BASE_URI = "https://api.iqscaffold.com/errors/";

  /**
   * Handles quota exceeded exceptions with specific error details.
   */
  @ExceptionHandler(UsageException.QuotaExceededException.class)
  public ResponseEntity<ProblemDetail> handleQuotaExceeded(
      UsageException.QuotaExceededException ex,
      HttpServletRequest request
  ) {
    logger.warn("Quota exceeded: metricType={}, limit={}, used={}",
        ex.getMetricType(), ex.getLimit(), ex.getUsed());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.TOO_MANY_REQUESTS,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "quota-exceeded"));
    problemDetail.setTitle("Quota Exceeded");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("metricType", ex.getMetricType());
    problemDetail.setProperty("limit", ex.getLimit());
    problemDetail.setProperty("used", ex.getUsed());
    problemDetail.setProperty("upgradeUrl", "/api/v1/billing/portal/upgrade");

    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
  }

  /**
   * Handles feature not available exceptions with upgrade information.
   */
  @ExceptionHandler(SubscriptionException.FeatureNotAvailableException.class)
  public ResponseEntity<ProblemDetail> handleFeatureNotAvailable(
      SubscriptionException.FeatureNotAvailableException ex,
      HttpServletRequest request
  ) {
    logger.warn("Feature not available: feature={}, currentPlan={}",
        ex.getFeatureName(), ex.getCurrentPlan());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "feature-not-available"));
    problemDetail.setTitle("Feature Not Available");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("featureName", ex.getFeatureName());
    problemDetail.setProperty("currentPlan", ex.getCurrentPlan());
    problemDetail.setProperty("upgradeUrl", "/api/v1/billing/portal/upgrade");
    problemDetail.setProperty("availableInPlans", "PRO, ENTERPRISE");

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }

  /**
   * Handles payment failed exceptions with retry information.
   */
  @ExceptionHandler(PaymentException.PaymentFailedException.class)
  public ResponseEntity<ProblemDetail> handlePaymentFailed(
      PaymentException.PaymentFailedException ex,
      HttpServletRequest request
  ) {
    logger.error("Payment failed: paymentId={}, reason={}",
        ex.getPaymentId(), ex.getReason());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYMENT_REQUIRED,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "payment-failed"));
    problemDetail.setTitle("Payment Failed");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("paymentId", ex.getPaymentId());
    problemDetail.setProperty("reason", ex.getReason());
    problemDetail.setProperty("retryUrl", "/api/v1/billing/payments/" + ex.getPaymentId() + "/retry");
    problemDetail.setProperty("updatePaymentMethodUrl", "/api/v1/billing/payment-methods");

    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problemDetail);
  }

  /**
   * Handles subscription not found exceptions.
   */
  @ExceptionHandler(SubscriptionException.SubscriptionNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleSubscriptionNotFound(
      SubscriptionException.SubscriptionNotFoundException ex,
      HttpServletRequest request
  ) {
    logger.warn("Subscription not found: subscriptionId={}", ex.getSubscriptionId());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "subscription-not-found"));
    problemDetail.setTitle("Subscription Not Found");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("subscriptionId", ex.getSubscriptionId());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /**
   * Handles subscription already exists exceptions.
   */
  @ExceptionHandler(SubscriptionException.SubscriptionAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleSubscriptionAlreadyExists(
      SubscriptionException.SubscriptionAlreadyExistsException ex,
      HttpServletRequest request
  ) {
    logger.warn("Subscription already exists: tenantId={}", ex.getTenantId());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "subscription-already-exists"));
    problemDetail.setTitle("Subscription Already Exists");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("tenantId", ex.getTenantId());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * Handles invalid subscription state exceptions.
   */
  @ExceptionHandler(SubscriptionException.InvalidSubscriptionStateException.class)
  public ResponseEntity<ProblemDetail> handleInvalidSubscriptionState(
      SubscriptionException.InvalidSubscriptionStateException ex,
      HttpServletRequest request
  ) {
    logger.warn("Invalid subscription state transition: from={}, to={}",
        ex.getCurrentState(), ex.getTargetState());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "invalid-subscription-state"));
    problemDetail.setTitle("Invalid Subscription State");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("currentState", ex.getCurrentState());
    problemDetail.setProperty("targetState", ex.getTargetState());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  /**
   * Handles plan not found exceptions.
   */
  @ExceptionHandler(PlanException.PlanNotFoundException.class)
  public ResponseEntity<ProblemDetail> handlePlanNotFound(
      PlanException.PlanNotFoundException ex,
      HttpServletRequest request
  ) {
    logger.warn("Plan not found: planId={}", ex.getPlanId());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "plan-not-found"));
    problemDetail.setTitle("Plan Not Found");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("planId", ex.getPlanId());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /**
   * Handles invalid plan transition exceptions.
   */
  @ExceptionHandler(PlanException.InvalidPlanTransitionException.class)
  public ResponseEntity<ProblemDetail> handleInvalidPlanTransition(
      PlanException.InvalidPlanTransitionException ex,
      HttpServletRequest request
  ) {
    logger.warn("Invalid plan transition: from={}, to={}",
        ex.getFromPlan(), ex.getToPlan());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "invalid-plan-transition"));
    problemDetail.setTitle("Invalid Plan Transition");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("fromPlan", ex.getFromPlan());
    problemDetail.setProperty("toPlan", ex.getToPlan());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  /**
   * Handles payment method not found exceptions.
   */
  @ExceptionHandler(PaymentException.PaymentMethodNotFoundException.class)
  public ResponseEntity<ProblemDetail> handlePaymentMethodNotFound(
      PaymentException.PaymentMethodNotFoundException ex,
      HttpServletRequest request
  ) {
    logger.warn("Payment method not found: paymentMethodId={}", ex.getPaymentMethodId());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "payment-method-not-found"));
    problemDetail.setTitle("Payment Method Not Found");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("paymentMethodId", ex.getPaymentMethodId());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /**
   * Handles invalid payment method exceptions.
   */
  @ExceptionHandler(PaymentException.InvalidPaymentMethodException.class)
  public ResponseEntity<ProblemDetail> handleInvalidPaymentMethod(
      PaymentException.InvalidPaymentMethodException ex,
      HttpServletRequest request
  ) {
    logger.warn("Invalid payment method: paymentMethodId={}, reason={}",
        ex.getPaymentMethodId(), ex.getReason());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "invalid-payment-method"));
    problemDetail.setTitle("Invalid Payment Method");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("paymentMethodId", ex.getPaymentMethodId());
    problemDetail.setProperty("reason", ex.getReason());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  /**
   * Handles invoice not found exceptions.
   */
  @ExceptionHandler(InvoiceException.InvoiceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleInvoiceNotFound(
      InvoiceException.InvoiceNotFoundException ex,
      HttpServletRequest request
  ) {
    logger.warn("Invoice not found: invoiceId={}", ex.getInvoiceId());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "invoice-not-found"));
    problemDetail.setTitle("Invoice Not Found");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("invoiceId", ex.getInvoiceId());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /**
   * Handles invoice already paid exceptions.
   */
  @ExceptionHandler(InvoiceException.InvoiceAlreadyPaidException.class)
  public ResponseEntity<ProblemDetail> handleInvoiceAlreadyPaid(
      InvoiceException.InvoiceAlreadyPaidException ex,
      HttpServletRequest request
  ) {
    logger.warn("Invoice already paid: invoiceId={}", ex.getInvoiceId());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "invoice-already-paid"));
    problemDetail.setTitle("Invoice Already Paid");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("invoiceId", ex.getInvoiceId());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * Handles usage limit exceeded exceptions.
   */
  @ExceptionHandler(UsageException.UsageLimitExceededException.class)
  public ResponseEntity<ProblemDetail> handleUsageLimitExceeded(
      UsageException.UsageLimitExceededException ex,
      HttpServletRequest request
  ) {
    logger.warn("Usage limit exceeded: metricType={}, limit={}",
        ex.getMetricType(), ex.getLimit());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.TOO_MANY_REQUESTS,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "usage-limit-exceeded"));
    problemDetail.setTitle("Usage Limit Exceeded");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("metricType", ex.getMetricType());
    problemDetail.setProperty("limit", ex.getLimit());

    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
  }

  /**
   * Handles payment required exceptions.
   */
  @ExceptionHandler(SubscriptionException.PaymentRequiredException.class)
  public ResponseEntity<ProblemDetail> handlePaymentRequired(
      SubscriptionException.PaymentRequiredException ex,
      HttpServletRequest request
  ) {
    logger.warn("Payment required: operation={}, reason={}",
        ex.getOperation(), ex.getReason());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYMENT_REQUIRED,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "payment-required"));
    problemDetail.setTitle("Payment Required");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode());
    problemDetail.setProperty("operation", ex.getOperation());
    problemDetail.setProperty("reason", ex.getReason());
    problemDetail.setProperty("paymentUrl", "/api/v1/billing/payments");

    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problemDetail);
  }

  /**
   * Handles generic billing exceptions.
   */
  @ExceptionHandler(BillingException.class)
  public ResponseEntity<ProblemDetail> handleBillingException(
      BillingException ex,
      HttpServletRequest request
  ) {
    logger.error("Billing exception: errorCode={}, message={}",
        ex.getErrorCode(), ex.getMessage(), ex);

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "billing-error"));
    problemDetail.setTitle("Billing Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", ex.getErrorCode() != null ? ex.getErrorCode() : "BILLING_000");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }

  /**
   * Handles validation exceptions from Bean Validation.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    logger.warn("Validation failed: {}", ex.getMessage());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed for request"
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "validation-error"));
    problemDetail.setTitle("Validation Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", "VALIDATION_ERROR");

    for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
      problemDetail.setProperty(error.getField(), error.getDefaultMessage());
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  /**
   * Handles constraint violation exceptions.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request
  ) {
    logger.warn("Constraint violation: {}", ex.getMessage());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Constraint violation in request"
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "constraint-violation"));
    problemDetail.setTitle("Constraint Violation");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", "CONSTRAINT_VIOLATION");

    ex.getConstraintViolations().forEach(violation ->
        problemDetail.setProperty(violation.getPropertyPath().toString(), violation.getMessage())
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  /**
   * Handles authentication exceptions.
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationException ex,
      HttpServletRequest request
  ) {
    logger.warn("Authentication failed: {}", ex.getMessage());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        "Authentication failed: " + ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "authentication-failed"));
    problemDetail.setTitle("Authentication Failed");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", "AUTHENTICATION_FAILED");

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
  }

  /**
   * Handles access denied exceptions.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      AccessDeniedException ex,
      HttpServletRequest request
  ) {
    logger.warn("Access denied: {}", ex.getMessage());

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "Access denied: " + ex.getMessage()
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "access-denied"));
    problemDetail.setTitle("Access Denied");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", "ACCESS_DENIED");

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }

  /**
   * Handles all other unexpected exceptions.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex,
      HttpServletRequest request
  ) {
    logger.error("Unexpected error occurred", ex);

    var problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please contact support if the problem persists."
    );
    problemDetail.setType(URI.create(ERROR_TYPE_BASE_URI + "internal-error"));
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("errorCode", "INTERNAL_ERROR");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }
}
