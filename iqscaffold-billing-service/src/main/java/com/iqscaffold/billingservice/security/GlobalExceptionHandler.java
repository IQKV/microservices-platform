package com.iqscaffold.billingservice.security;

import com.iqscaffold.billingservice.shared.exception.BillingException;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import com.iqscaffold.billingservice.shared.exception.UsageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
 * Translates domain exceptions into appropriate HTTP responses with
 * consistent error format.
 * 
 * <p>Error Response Format:
 * <pre>
 * {
 *   "errorCode": "BILLING_009",
 *   "message": "Quota exceeded for API_CALLS: used 1000 of 500",
 *   "timestamp": "2024-12-07T10:30:00Z",
 *   "path": "/api/v1/billing/usage/check-quota",
 *   "details": {
 *     "metricType": "API_CALLS",
 *     "limit": 500,
 *     "used": 1000
 *   }
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Error response record for consistent error format.
     */
    public record ErrorResponse(
        String errorCode,
        String message,
        Instant timestamp,
        String path,
        Map<String, Object> details
    ) {
        public ErrorResponse(String errorCode, String message, String path) {
            this(errorCode, message, Instant.now(), path, new HashMap<>());
        }

        public ErrorResponse(String errorCode, String message, String path, Map<String, Object> details) {
            this(errorCode, message, Instant.now(), path, details);
        }
    }

    /**
     * Handles quota exceeded exceptions with specific error details.
     */
    @ExceptionHandler(UsageException.QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(
        UsageException.QuotaExceededException ex,
        HttpServletRequest request
    ) {
        logger.warn("Quota exceeded: metricType={}, limit={}, used={}", 
            ex.getMetricType(), ex.getLimit(), ex.getUsed());

        var details = new HashMap<String, Object>();
        details.put("metricType", ex.getMetricType());
        details.put("limit", ex.getLimit());
        details.put("used", ex.getUsed());
        details.put("upgradeUrl", "/api/v1/billing/portal/upgrade");

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    /**
     * Handles feature not available exceptions with upgrade information.
     */
    @ExceptionHandler(SubscriptionException.FeatureNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleFeatureNotAvailable(
        SubscriptionException.FeatureNotAvailableException ex,
        HttpServletRequest request
    ) {
        logger.warn("Feature not available: feature={}, currentPlan={}", 
            ex.getFeatureName(), ex.getCurrentPlan());

        var details = new HashMap<String, Object>();
        details.put("featureName", ex.getFeatureName());
        details.put("currentPlan", ex.getCurrentPlan());
        details.put("upgradeUrl", "/api/v1/billing/portal/upgrade");
        details.put("availableInPlans", "PRO, ENTERPRISE");

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles payment failed exceptions with retry information.
     */
    @ExceptionHandler(PaymentException.PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(
        PaymentException.PaymentFailedException ex,
        HttpServletRequest request
    ) {
        logger.error("Payment failed: paymentId={}, reason={}", 
            ex.getPaymentId(), ex.getReason());

        var details = new HashMap<String, Object>();
        details.put("paymentId", ex.getPaymentId());
        details.put("reason", ex.getReason());
        details.put("retryUrl", "/api/v1/billing/payments/" + ex.getPaymentId() + "/retry");
        details.put("updatePaymentMethodUrl", "/api/v1/billing/payment-methods");

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
    }

    /**
     * Handles subscription not found exceptions.
     */
    @ExceptionHandler(SubscriptionException.SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(
        SubscriptionException.SubscriptionNotFoundException ex,
        HttpServletRequest request
    ) {
        logger.warn("Subscription not found: subscriptionId={}", ex.getSubscriptionId());

        var details = new HashMap<String, Object>();
        details.put("subscriptionId", ex.getSubscriptionId());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles subscription already exists exceptions.
     */
    @ExceptionHandler(SubscriptionException.SubscriptionAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionAlreadyExists(
        SubscriptionException.SubscriptionAlreadyExistsException ex,
        HttpServletRequest request
    ) {
        logger.warn("Subscription already exists: tenantId={}", ex.getTenantId());

        var details = new HashMap<String, Object>();
        details.put("tenantId", ex.getTenantId());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles invalid subscription state exceptions.
     */
    @ExceptionHandler(SubscriptionException.InvalidSubscriptionStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSubscriptionState(
        SubscriptionException.InvalidSubscriptionStateException ex,
        HttpServletRequest request
    ) {
        logger.warn("Invalid subscription state transition: from={}, to={}", 
            ex.getCurrentState(), ex.getTargetState());

        var details = new HashMap<String, Object>();
        details.put("currentState", ex.getCurrentState());
        details.put("targetState", ex.getTargetState());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles plan not found exceptions.
     */
    @ExceptionHandler(PlanException.PlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlanNotFound(
        PlanException.PlanNotFoundException ex,
        HttpServletRequest request
    ) {
        logger.warn("Plan not found: planId={}", ex.getPlanId());

        var details = new HashMap<String, Object>();
        details.put("planId", ex.getPlanId());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles invalid plan transition exceptions.
     */
    @ExceptionHandler(PlanException.InvalidPlanTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPlanTransition(
        PlanException.InvalidPlanTransitionException ex,
        HttpServletRequest request
    ) {
        logger.warn("Invalid plan transition: from={}, to={}", 
            ex.getFromPlan(), ex.getToPlan());

        var details = new HashMap<String, Object>();
        details.put("fromPlan", ex.getFromPlan());
        details.put("toPlan", ex.getToPlan());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles payment method not found exceptions.
     */
    @ExceptionHandler(PaymentException.PaymentMethodNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentMethodNotFound(
        PaymentException.PaymentMethodNotFoundException ex,
        HttpServletRequest request
    ) {
        logger.warn("Payment method not found: paymentMethodId={}", ex.getPaymentMethodId());

        var details = new HashMap<String, Object>();
        details.put("paymentMethodId", ex.getPaymentMethodId());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles invalid payment method exceptions.
     */
    @ExceptionHandler(PaymentException.InvalidPaymentMethodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentMethod(
        PaymentException.InvalidPaymentMethodException ex,
        HttpServletRequest request
    ) {
        logger.warn("Invalid payment method: paymentMethodId={}, reason={}", 
            ex.getPaymentMethodId(), ex.getReason());

        var details = new HashMap<String, Object>();
        details.put("paymentMethodId", ex.getPaymentMethodId());
        details.put("reason", ex.getReason());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles invoice not found exceptions.
     */
    @ExceptionHandler(InvoiceException.InvoiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceNotFound(
        InvoiceException.InvoiceNotFoundException ex,
        HttpServletRequest request
    ) {
        logger.warn("Invoice not found: invoiceId={}", ex.getInvoiceId());

        var details = new HashMap<String, Object>();
        details.put("invoiceId", ex.getInvoiceId());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles invoice already paid exceptions.
     */
    @ExceptionHandler(InvoiceException.InvoiceAlreadyPaidException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceAlreadyPaid(
        InvoiceException.InvoiceAlreadyPaidException ex,
        HttpServletRequest request
    ) {
        logger.warn("Invoice already paid: invoiceId={}", ex.getInvoiceId());

        var details = new HashMap<String, Object>();
        details.put("invoiceId", ex.getInvoiceId());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles usage limit exceeded exceptions.
     */
    @ExceptionHandler(UsageException.UsageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleUsageLimitExceeded(
        UsageException.UsageLimitExceededException ex,
        HttpServletRequest request
    ) {
        logger.warn("Usage limit exceeded: metricType={}, limit={}", 
            ex.getMetricType(), ex.getLimit());

        var details = new HashMap<String, Object>();
        details.put("metricType", ex.getMetricType());
        details.put("limit", ex.getLimit());

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    /**
     * Handles payment required exceptions.
     */
    @ExceptionHandler(SubscriptionException.PaymentRequiredException.class)
    public ResponseEntity<ErrorResponse> handlePaymentRequired(
        SubscriptionException.PaymentRequiredException ex,
        HttpServletRequest request
    ) {
        logger.warn("Payment required: operation={}, reason={}", 
            ex.getOperation(), ex.getReason());

        var details = new HashMap<String, Object>();
        details.put("operation", ex.getOperation());
        details.put("reason", ex.getReason());
        details.put("paymentUrl", "/api/v1/billing/payments");

        var errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
    }

    /**
     * Handles generic billing exceptions.
     */
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ErrorResponse> handleBillingException(
        BillingException ex,
        HttpServletRequest request
    ) {
        logger.error("Billing exception: errorCode={}, message={}", 
            ex.getErrorCode(), ex.getMessage(), ex);

        var errorResponse = new ErrorResponse(
            ex.getErrorCode() != null ? ex.getErrorCode() : "BILLING_000",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles validation exceptions from Bean Validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        logger.warn("Validation failed: {}", ex.getMessage());

        var details = new HashMap<String, Object>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }

        var errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed for request",
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        logger.warn("Constraint violation: {}", ex.getMessage());

        var details = new HashMap<String, Object>();
        ex.getConstraintViolations().forEach(violation -> 
            details.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        var errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Constraint violation in request",
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException ex,
        HttpServletRequest request
    ) {
        logger.warn("Authentication failed: {}", ex.getMessage());

        var errorResponse = new ErrorResponse(
            "AUTHENTICATION_FAILED",
            "Authentication failed: " + ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        logger.warn("Access denied: {}", ex.getMessage());

        var errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "Access denied: " + ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        logger.error("Unexpected error occurred", ex);

        var errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support if the problem persists.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
