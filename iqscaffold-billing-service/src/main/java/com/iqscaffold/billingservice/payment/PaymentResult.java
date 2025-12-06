package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable record representing the result of a payment operation.
 * 
 * <p>This record encapsulates the outcome of payment processing, refunds,
 * or other payment operations. It provides a provider-agnostic representation
 * of payment results, isolating the domain from provider-specific response formats.
 * 
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> Java record ensures thread-safe, immutable payment results</li>
 *   <li><strong>Provider Agnostic:</strong> Unified format regardless of payment provider</li>
 *   <li><strong>Complete Information:</strong> Contains all necessary data for domain processing</li>
 *   <li><strong>Error Handling:</strong> Includes failure reason for proper error handling</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PaymentResult result = paymentProvider.processPayment(
 *     paymentMethodId,
 *     new BigDecimal("99.99"),
 *     "USD",
 *     idempotencyKey
 * );
 * 
 * if (result.success()) {
 *     // Update invoice status to PAID
 *     invoice.markAsPaid(result.providerPaymentId(), result.processedAt());
 *     // Send payment confirmation email
 *     emailService.sendPaymentConfirmation(invoice);
 * } else {
 *     // Handle payment failure
 *     log.warn("Payment failed: {}", result.failureReason());
 *     // Schedule retry or notify customer
 *     paymentRetryService.scheduleRetry(invoice, result.failureReason());
 * }
 * }</pre>
 * 
 * @param success true if payment operation succeeded, false otherwise
 * @param providerPaymentId the payment provider's unique payment identifier (null if failed)
 * @param amount the payment amount processed
 * @param currency the ISO 4217 currency code (e.g., "USD", "EUR")
 * @param processedAt the timestamp when the payment was processed
 * @param failureReason the reason for payment failure (null if successful)
 */
public record PaymentResult(
    boolean success,
    String providerPaymentId,
    BigDecimal amount,
    String currency,
    LocalDateTime processedAt,
    String failureReason
) {
    
    /**
     * Creates a successful payment result.
     * 
     * @param providerPaymentId the payment provider's unique payment identifier
     * @param amount the payment amount processed
     * @param currency the ISO 4217 currency code
     * @param processedAt the timestamp when the payment was processed
     * @return a successful PaymentResult
     */
    public static PaymentResult success(
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        LocalDateTime processedAt
    ) {
        return new PaymentResult(
            true,
            providerPaymentId,
            amount,
            currency,
            processedAt,
            null
        );
    }
    
    /**
     * Creates a failed payment result.
     * 
     * @param amount the payment amount that was attempted
     * @param currency the ISO 4217 currency code
     * @param failureReason the reason for payment failure
     * @return a failed PaymentResult
     */
    public static PaymentResult failure(
        BigDecimal amount,
        String currency,
        String failureReason
    ) {
        return new PaymentResult(
            false,
            null,
            amount,
            currency,
            LocalDateTime.now(),
            failureReason
        );
    }
    
    /**
     * Validates the payment result state.
     * 
     * <p>Ensures that:
     * <ul>
     *   <li>Successful payments have a provider payment ID</li>
     *   <li>Failed payments have a failure reason</li>
     *   <li>Amount is not null and not negative</li>
     *   <li>Currency is not null or blank</li>
     *   <li>Processed timestamp is not null</li>
     * </ul>
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public PaymentResult {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount must be non-negative");
        }
        
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be null or blank");
        }
        
        if (processedAt == null) {
            throw new IllegalArgumentException("Processed timestamp must not be null");
        }
        
        if (success && (providerPaymentId == null || providerPaymentId.isBlank())) {
            throw new IllegalArgumentException(
                "Successful payment must have a provider payment ID"
            );
        }
        
        if (!success && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException(
                "Failed payment must have a failure reason"
            );
        }
    }
    
    /**
     * Checks if the payment failed.
     * 
     * @return true if payment failed, false if successful
     */
    public boolean failed() {
        return !success;
    }
}
