package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.shared.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manual payment provider implementation for offline payment recording.
 * 
 * <p>This final class implements the PaymentProviderAdapter interface for manual
 * payment processing, supporting enterprise contracts, wire transfers, checks,
 * and other offline payment methods. Unlike automated providers (Stripe, PayPal),
 * this adapter records payments that have been processed outside the system.
 * 
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Enterprise Support:</strong> Handles large enterprise contracts with custom payment terms</li>
 *   <li><strong>Offline Payments:</strong> Records wire transfers, checks, ACH, and manual card processing</li>
 *   <li><strong>Audit Trail:</strong> Maintains complete records of manual payment entries</li>
 *   <li><strong>No External Integration:</strong> No API calls to external providers</li>
 *   <li><strong>Idempotency:</strong> Prevents duplicate payment recording</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Enterprise contracts with net-30/net-60 payment terms</li>
 *   <li>Wire transfers and ACH payments</li>
 *   <li>Check payments</li>
 *   <li>Manual credit card processing (via phone or in-person)</li>
 *   <li>Purchase orders and invoicing workflows</li>
 *   <li>Government contracts requiring specific payment methods</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>Manual payments require admin authorization</li>
 *   <li>All manual payments are logged for audit purposes</li>
 *   <li>Payment method tokens are internal identifiers (not real tokens)</li>
 *   <li>No sensitive payment data is stored</li>
 * </ul>
 * 
 * <h2>Limitations</h2>
 * <ul>
 *   <li>No automated payment processing</li>
 *   <li>No webhook support (payments recorded manually)</li>
 *   <li>Refunds must be processed manually outside the system</li>
 *   <li>No real-time payment validation</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create customer record
 * String customerId = manualProvider.createCustomer(
 *     "tenant-123",
 *     "enterprise@example.com",
 *     "Acme Corporation"
 * );
 * 
 * // Record payment method (e.g., wire transfer details)
 * PaymentMethodDetails paymentMethod = manualProvider.createPaymentMethod(
 *     customerId,
 *     "WIRE_TRANSFER_ACCOUNT_12345"
 * );
 * 
 * // Record a manual payment (e.g., wire transfer received)
 * PaymentResult result = manualProvider.processPayment(
 *     paymentMethod.providerPaymentMethodId(),
 *     new BigDecimal("50000.00"),
 *     "USD",
 *     "manual-payment-" + UUID.randomUUID()
 * );
 * 
 * if (result.success()) {
 *     log.info("Manual payment recorded: {}", result.providerPaymentId());
 * }
 * }</pre>
 * 
 * @see PaymentProviderAdapter
 * @see PaymentResult
 * @see PaymentMethodDetails
 */
@Component
@Slf4j
public final class ManualPaymentProvider implements PaymentProviderAdapter {
    
    // In-memory storage for demonstration (in production, use database)
    private final Map<String, CustomerRecord> customers = new ConcurrentHashMap<>();
    private final Map<String, PaymentMethodRecord> paymentMethods = new ConcurrentHashMap<>();
    private final Map<String, String> processedPayments = new ConcurrentHashMap<>();
    
    /**
     * Customer record for manual payment tracking.
     */
    private record CustomerRecord(
        String customerId,
        String tenantId,
        String email,
        String name
    ) {}
    
    /**
     * Payment method record for manual payment tracking.
     */
    private record PaymentMethodRecord(
        String paymentMethodId,
        String customerId,
        String type,
        String details
    ) {}
    
    @Override
    public String createCustomer(String tenantId, String email, String name) {
        log.debug("Creating manual payment customer for tenant: {}, email: {}", tenantId, email);
        
        // Generate internal customer ID
        String customerId = "manual_cust_" + UUID.randomUUID().toString().replace("-", "");
        
        // Store customer record
        customers.put(customerId, new CustomerRecord(customerId, tenantId, email, name));
        
        log.info("Created manual payment customer: {} for tenant: {}", customerId, tenantId);
        return customerId;
    }
    
    @Override
    public void updateCustomer(String customerId, CustomerUpdateRequest request) {
        log.debug("Updating manual payment customer: {}", customerId);
        
        CustomerRecord existing = customers.get(customerId);
        if (existing == null) {
            throw new PaymentException("Customer not found: " + customerId);
        }
        
        // Update customer record with new information
        String updatedEmail = request.hasEmail() ? request.email() : existing.email();
        String updatedName = request.hasName() ? request.name() : existing.name();
        
        customers.put(customerId, new CustomerRecord(
            existing.customerId(),
            existing.tenantId(),
            updatedEmail,
            updatedName
        ));
        
        log.info("Updated manual payment customer: {}", customerId);
    }
    
    @Override
    public PaymentMethodDetails createPaymentMethod(String customerId, String token) {
        log.debug("Creating manual payment method for customer: {}", customerId);
        
        // Verify customer exists
        if (!customers.containsKey(customerId)) {
            throw new PaymentException("Customer not found: " + customerId);
        }
        
        // Generate internal payment method ID
        String paymentMethodId = "manual_pm_" + UUID.randomUUID().toString().replace("-", "");
        
        // Determine payment method type from token
        String type = determinePaymentMethodType(token);
        
        // Store payment method record
        paymentMethods.put(paymentMethodId, new PaymentMethodRecord(
            paymentMethodId,
            customerId,
            type,
            token
        ));
        
        log.info("Created manual payment method: {} for customer: {}", paymentMethodId, customerId);
        
        // Return payment method details
        return new PaymentMethodDetails(
            paymentMethodId,
            type,
            extractLast4(token),
            extractBrand(token, type),
            null, // Manual payments don't have expiration
            null
        );
    }
    
    @Override
    public PaymentMethodDetails getPaymentMethod(String paymentMethodId) {
        log.debug("Retrieving manual payment method: {}", paymentMethodId);
        
        PaymentMethodRecord record = paymentMethods.get(paymentMethodId);
        if (record == null) {
            throw new PaymentException.PaymentMethodNotFoundException(paymentMethodId);
        }
        
        return new PaymentMethodDetails(
            record.paymentMethodId(),
            record.type(),
            extractLast4(record.details()),
            extractBrand(record.details(), record.type()),
            null,
            null
        );
    }
    
    @Override
    public void deletePaymentMethod(String paymentMethodId) {
        log.debug("Deleting manual payment method: {}", paymentMethodId);
        
        if (!paymentMethods.containsKey(paymentMethodId)) {
            throw new PaymentException.PaymentMethodNotFoundException(paymentMethodId);
        }
        
        paymentMethods.remove(paymentMethodId);
        
        log.info("Deleted manual payment method: {}", paymentMethodId);
    }
    
    @Override
    public PaymentResult processPayment(
        String paymentMethodId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
    ) {
        log.debug("Recording manual payment: amount={}, currency={}, paymentMethod={}", 
            amount, currency, paymentMethodId);
        
        // Check idempotency - prevent duplicate payment recording
        if (processedPayments.containsKey(idempotencyKey)) {
            String existingPaymentId = processedPayments.get(idempotencyKey);
            log.info("Duplicate manual payment request detected, returning existing payment: {}", 
                existingPaymentId);
            
            return PaymentResult.success(
                existingPaymentId,
                amount,
                currency,
                LocalDateTime.now()
            );
        }
        
        // Verify payment method exists
        if (!paymentMethods.containsKey(paymentMethodId)) {
            log.error("Payment method not found: {}", paymentMethodId);
            return PaymentResult.failure(
                amount,
                currency,
                "Payment method not found: " + paymentMethodId
            );
        }
        
        // Generate payment ID
        String paymentId = "manual_pay_" + UUID.randomUUID().toString().replace("-", "");
        
        // Record payment as processed
        processedPayments.put(idempotencyKey, paymentId);
        
        log.info("Manual payment recorded successfully: {} for amount: {} {}", 
            paymentId, amount, currency);
        
        // Return success result
        return PaymentResult.success(
            paymentId,
            amount,
            currency,
            LocalDateTime.now()
        );
    }
    
    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        log.debug("Recording manual refund: paymentId={}, amount={}", paymentId, amount);
        
        // Verify payment exists
        if (!processedPayments.containsValue(paymentId)) {
            log.error("Payment not found for refund: {}", paymentId);
            return PaymentResult.failure(
                amount,
                "USD", // Default currency for manual refunds
                "Payment not found: " + paymentId
            );
        }
        
        // Generate refund ID
        String refundId = "manual_refund_" + UUID.randomUUID().toString().replace("-", "");
        
        log.info("Manual refund recorded successfully: {} for payment: {}, amount: {}, reason: {}", 
            refundId, paymentId, amount, reason);
        
        // Return success result
        return PaymentResult.success(
            refundId,
            amount,
            "USD", // Default currency for manual refunds
            LocalDateTime.now()
        );
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        log.debug("Manual payment provider does not support webhooks");
        
        // Manual payments don't have webhooks
        // Always return false as webhooks are not applicable
        return false;
    }
    
    @Override
    public String getProviderName() {
        return "manual";
    }
    
    /**
     * Determines payment method type from token string.
     * 
     * <p>Analyzes the token to identify the payment method type:
     * <ul>
     *   <li>WIRE_TRANSFER - for wire transfer details</li>
     *   <li>CHECK - for check numbers</li>
     *   <li>ACH - for ACH account details</li>
     *   <li>PURCHASE_ORDER - for PO numbers</li>
     *   <li>MANUAL_CARD - for manually processed cards</li>
     *   <li>OTHER - for unrecognized types</li>
     * </ul>
     * 
     * @param token the payment method token/identifier
     * @return the payment method type
     */
    private String determinePaymentMethodType(String token) {
        if (token == null || token.isBlank()) {
            return "OTHER";
        }
        
        String upperToken = token.toUpperCase();
        
        if (upperToken.contains("WIRE") || upperToken.contains("TRANSFER")) {
            return "WIRE_TRANSFER";
        } else if (upperToken.contains("CHECK") || upperToken.contains("CHEQUE")) {
            return "CHECK";
        } else if (upperToken.contains("ACH")) {
            return "ACH";
        } else if (upperToken.contains("PO") || upperToken.contains("PURCHASE_ORDER")) {
            return "PURCHASE_ORDER";
        } else if (upperToken.contains("CARD")) {
            return "MANUAL_CARD";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * Extracts last 4 characters from token for display purposes.
     * 
     * @param token the payment method token
     * @return the last 4 characters, or null if token is too short
     */
    private String extractLast4(String token) {
        if (token == null || token.length() < 4) {
            return null;
        }
        
        return token.substring(token.length() - 4);
    }
    
    /**
     * Extracts brand/description from token based on type.
     * 
     * @param token the payment method token
     * @param type the payment method type
     * @return a user-friendly brand/description
     */
    private String extractBrand(String token, String type) {
        return switch (type) {
            case "WIRE_TRANSFER" -> "Wire Transfer";
            case "CHECK" -> "Check";
            case "ACH" -> "ACH Transfer";
            case "PURCHASE_ORDER" -> "Purchase Order";
            case "MANUAL_CARD" -> "Manual Card";
            default -> "Manual Payment";
        };
    }
}
