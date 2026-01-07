package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void onCreate_shouldSetIdAndTimestamps() {
        // Given
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);

        // When
        payment.onCreate();

        // Then
        assertNotNull(payment.getId());
        assertNotNull(payment.getCreatedAt());
        assertNotNull(payment.getUpdatedAt());
        assertEquals(payment.getCreatedAt(), payment.getUpdatedAt());
    }

    @Test
    void onCreate_shouldNotOverrideExistingId() {
        // Given
        UUID existingId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(existingId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);

        // When
        payment.onCreate();

        // Then
        assertEquals(existingId, payment.getId());
    }

    @Test
    void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
        // Given
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);
        payment.onCreate();

        Instant originalUpdatedAt = payment.getUpdatedAt();
        Thread.sleep(10); // Small delay to ensure timestamp difference

        // When
        payment.onUpdate();

        // Then
        assertNotNull(payment.getUpdatedAt());
        assertTrue(payment.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        Payment payment = new Payment();
        UUID id = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.50");
        String currency = "eur";
        String status = BillingConstants.PaymentStatus.SUCCEEDED;
        String paymentIntentId = "pi_123";
        String clientSecret = "secret_123";
        BigDecimal applicationFee = new BigDecimal("15.05");
        String merchantAccountId = "acct_123";
        Instant now = Instant.now();

        // When
        payment.setId(id);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setPaymentIntentId(paymentIntentId);
        payment.setClientSecret(clientSecret);
        payment.setApplicationFeeAmount(applicationFee);
        payment.setMerchantAccountId(merchantAccountId);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        // Then
        assertEquals(id, payment.getId());
        assertEquals(amount, payment.getAmount());
        assertEquals(currency, payment.getCurrency());
        assertEquals(status, payment.getStatus());
        assertEquals(paymentIntentId, payment.getPaymentIntentId());
        assertEquals(clientSecret, payment.getClientSecret());
        assertEquals(applicationFee, payment.getApplicationFeeAmount());
        assertEquals(merchantAccountId, payment.getMerchantAccountId());
        assertEquals(now, payment.getCreatedAt());
        assertEquals(now, payment.getUpdatedAt());
    }

    @Test
    void constructor_shouldCreateEmptyPayment() {
        // When
        Payment payment = new Payment();

        // Then
        assertNotNull(payment);
        assertNull(payment.getId());
        assertNull(payment.getAmount());
        assertNull(payment.getCurrency());
        assertNull(payment.getStatus());
    }

    @Test
    void payment_shouldHandleNullOptionalFields() {
        // Given
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            payment.setPaymentIntentId(null);
            payment.setClientSecret(null);
            payment.setApplicationFeeAmount(null);
            payment.setMerchantAccountId(null);
        });

        assertNull(payment.getPaymentIntentId());
        assertNull(payment.getClientSecret());
        assertNull(payment.getApplicationFeeAmount());
        assertNull(payment.getMerchantAccountId());
    }

    @Test
    void payment_shouldHandleZeroAmount() {
        // Given
        Payment payment = new Payment();
        payment.setAmount(BigDecimal.ZERO);
        payment.setCurrency("usd");
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);

        // When & Then
        assertEquals(BigDecimal.ZERO, payment.getAmount());
    }

    @Test
    void payment_shouldHandleLargeAmount() {
        // Given
        Payment payment = new Payment();
        BigDecimal largeAmount = new BigDecimal("999999999.99");
        payment.setAmount(largeAmount);
        payment.setCurrency("usd");
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);

        // When & Then
        assertEquals(largeAmount, payment.getAmount());
    }
}
