package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;

/**
 * Anti-corruption layer interface for payment provider integration.
 * 
 * <p>This sealed interface defines the contract for payment provider implementations,
 * isolating the domain model from external payment provider APIs (Stripe, PayPal, etc.).
 * By using a sealed interface, we restrict the permitted implementations to known providers,
 * providing compile-time safety and exhaustive pattern matching support.
 * 
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Anti-Corruption Layer:</strong> Protects domain model from external API changes</li>
 *   <li><strong>Provider Abstraction:</strong> Enables switching between payment providers without domain changes</li>
 *   <li><strong>Sealed Interface:</strong> Restricts implementations to known providers for type safety</li>
 *   <li><strong>Domain Translation:</strong> Translates between domain models and provider-specific APIs</li>
 * </ul>
 * 
 * <h2>Permitted Implementations</h2>
 * <ul>
 *   <li>{@code StripePaymentProvider} - Stripe payment processing</li>
 *   <li>{@code PayPalPaymentProvider} - PayPal payment processing</li>
 *   <li>{@code ManualPaymentProvider} - Manual/offline payment recording</li>
 * </ul>
 * 
 * <h2>Contract Guarantees</h2>
 * <ul>
 *   <li>All payment operations are idempotent when using the same idempotency key</li>
 *   <li>Payment method tokens are provider-specific and opaque to the domain</li>
 *   <li>All monetary amounts use BigDecimal for precision</li>
 *   <li>Provider-specific errors are translated to domain exceptions</li>
 *   <li>Webhook signature verification is provider-specific</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PaymentProviderAdapter provider = paymentProviderFactory.getProvider();
 * 
 * // Create payment method
 * PaymentMethodDetails paymentMethod = provider.createPaymentMethod(
 *     customerId,
 *     tokenFromClient
 * );
 * 
 * // Process payment
 * PaymentResult result = provider.processPayment(
 *     paymentMethod.providerPaymentMethodId(),
 *     amount,
 *     currency,
 *     idempotencyKey
 * );
 * 
 * if (result.success()) {
 *     // Handle successful payment
 * } else {
 *     // Handle payment failure
 * }
 * }</pre>
 * 
 * @see PaymentResult
 * @see PaymentMethodDetails
 * @see CustomerUpdateRequest
 */
public sealed interface PaymentProviderAdapter 
    permits StripePaymentProvider, PayPalPaymentProvider, ManualPaymentProvider {
    
    /**
     * Creates a customer in the payment provider system.
     * 
     * <p>This method creates a customer record in the payment provider's system,
     * which is required before adding payment methods or processing payments.
     * The customer ID returned is provider-specific and should be stored for
     * future operations.
     * 
     * @param tenantId the tenant identifier
     * @param email the customer's email address
     * @param name the customer's full name (optional, may be null)
     * @return the provider-specific customer ID
     * @throws PaymentException if customer creation fails
     */
    String createCustomer(String tenantId, String email, String name);
    
    /**
     * Updates customer information in the payment provider system.
     * 
     * @param customerId the provider-specific customer ID
     * @param request the customer update request containing fields to update
     * @throws PaymentException if customer update fails
     */
    void updateCustomer(String customerId, CustomerUpdateRequest request);
    
    /**
     * Creates a payment method (credit card, bank account, etc.) for a customer.
     * 
     * <p>This method tokenizes and stores a payment method with the payment provider.
     * The token provided should be obtained from the provider's client-side SDK
     * to ensure PCI compliance (never send raw card numbers to the server).
     * 
     * <p><strong>Security Note:</strong> The token parameter should be a provider-generated
     * token from their client-side SDK, not raw payment details.
     * 
     * @param customerId the provider-specific customer ID
     * @param token the payment method token from provider's client SDK
     * @return payment method details including provider ID and display information
     * @throws PaymentException if payment method creation fails
     */
    PaymentMethodDetails createPaymentMethod(String customerId, String token);
    
    /**
     * Retrieves payment method details from the payment provider.
     * 
     * @param paymentMethodId the provider-specific payment method ID
     * @return payment method details
     * @throws PaymentException if payment method not found or retrieval fails
     */
    PaymentMethodDetails getPaymentMethod(String paymentMethodId);
    
    /**
     * Deletes a payment method from the payment provider system.
     * 
     * <p>This permanently removes the payment method from the provider's system.
     * Ensure the payment method is not set as default before deletion.
     * 
     * @param paymentMethodId the provider-specific payment method ID
     * @throws PaymentException if payment method deletion fails
     */
    void deletePaymentMethod(String paymentMethodId);
    
    /**
     * Processes a payment using the specified payment method.
     * 
     * <p>This method charges the payment method for the specified amount.
     * The operation is idempotent when using the same idempotency key,
     * preventing duplicate charges if the request is retried.
     * 
     * <p><strong>Idempotency:</strong> If a payment with the same idempotency key
     * has already been processed, the original result is returned without
     * creating a new charge.
     * 
     * @param paymentMethodId the provider-specific payment method ID
     * @param amount the payment amount (must be positive)
     * @param currency the ISO 4217 currency code (e.g., "USD", "EUR")
     * @param idempotencyKey unique key to ensure idempotent payment processing
     * @return payment result indicating success or failure with details
     * @throws PaymentException if payment processing fails unexpectedly
     */
    PaymentResult processPayment(
        String paymentMethodId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
    );
    
    /**
     * Issues a refund for a previously successful payment.
     * 
     * <p>This method refunds all or part of a payment. Partial refunds are supported
     * by specifying an amount less than the original payment amount. Multiple partial
     * refunds can be issued up to the original payment amount.
     * 
     * @param paymentId the provider-specific payment ID to refund
     * @param amount the refund amount (must not exceed original payment amount)
     * @param reason the reason for the refund (for audit purposes)
     * @return payment result indicating refund success or failure
     * @throws PaymentException if refund processing fails
     */
    PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason);
    
    /**
     * Verifies the authenticity of a webhook event from the payment provider.
     * 
     * <p>Payment providers sign webhook events to ensure they originate from
     * the provider and haven't been tampered with. This method verifies the
     * signature using the provider's webhook secret.
     * 
     * <p><strong>Security:</strong> Always verify webhook signatures before
     * processing webhook events to prevent malicious webhook injection attacks.
     * 
     * @param payload the raw webhook payload (JSON string)
     * @param signature the webhook signature header value
     * @return true if signature is valid, false otherwise
     */
    boolean verifyWebhookSignature(String payload, String signature);
    
    /**
     * Returns the name of the payment provider.
     * 
     * <p>This is used for logging, metrics, and provider selection.
     * 
     * @return the provider name (e.g., "stripe", "paypal", "manual")
     */
    String getProviderName();
}
