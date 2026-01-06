package com.iqscaffold.billingservice.payment;

import java.util.UUID;

/**
 * Interface for managing the refund process for payments.
 * 
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public interface RefundService {

    /**
     * Processes a full refund for a specific payment.
     *
     * @param paymentId The internal UUID of the payment to refund
     */
    void processRefund(UUID paymentId);
}