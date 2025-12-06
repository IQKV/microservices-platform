package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;

/**
 * Manual payment provider implementation for offline/enterprise payments.
 * 
 * <p>This class implements the PaymentProviderAdapter interface for manual payment recording.
 * It is used for enterprise contracts, wire transfers, checks, and other offline payment methods
 * that don't go through automated payment processors.
 * 
 * <p><strong>Note:</strong> This is a placeholder implementation. Full implementation will be
 * completed in task 20.
 * 
 * @see PaymentProviderAdapter
 */
public final class ManualPaymentProvider implements PaymentProviderAdapter {
    
    @Override
    public String createCustomer(String tenantId, String email, String name) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public void updateCustomer(String customerId, CustomerUpdateRequest request) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public PaymentMethodDetails createPaymentMethod(String customerId, String token) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public PaymentMethodDetails getPaymentMethod(String paymentMethodId) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public void deletePaymentMethod(String paymentMethodId) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public PaymentResult processPayment(
        String paymentMethodId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
    ) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        throw new UnsupportedOperationException("Manual provider not yet implemented");
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // Manual payments don't have webhooks
        return false;
    }
    
    @Override
    public String getProviderName() {
        return "manual";
    }
}
