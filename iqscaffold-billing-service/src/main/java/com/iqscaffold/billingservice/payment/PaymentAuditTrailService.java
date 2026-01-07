package com.iqscaffold.billingservice.payment;

import java.util.UUID;

import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.BillingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class PaymentAuditTrailService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentAuditTrailService.class);

    public void logPaymentAttempt(UUID paymentId, String status) {
        try (var ignored = MDC.putCloseable(BillingConstants.MDC.PAYMENT_ID, paymentId.toString())) {
            String userId = SecurityContextHelper.getCurrentUserId() != null 
                ? SecurityContextHelper.getCurrentUserId().toString() 
                : "system";
            String tenantId = SecurityContextHelper.getCurrentTenantId();

            logger.info("Payment Transition | PaymentID: {} | Status: {} | User: {} | Tenant: {}", 
                paymentId, status, userId, tenantId);
        }
    }
}
