package com.iqscaffold.billingservice.payment;

import java.util.UUID;

/**
 * Interface for managing the refund process for payments.
 */
public interface RefundService {
    void processRefund(UUID paymentId);
}
