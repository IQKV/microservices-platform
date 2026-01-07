package com.iqscaffold.billingservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BillingEventTest {

  @Test
  void constructor_shouldCreateBillingEventWithAllFields() {
    // Given
    String eventId = "event-123";
    String eventType = "PAYMENT_SUCCESSFUL";
    String paymentId = "pay-123";
    String merchantId = "merchant-123";
    String invoiceId = "inv-123";
    String customerEmail = "customer@example.com";
    Map<String, Object> eventData = new HashMap<>();
    eventData.put("amount", "100.00");
    Instant timestamp = Instant.now();
    String tenantId = "tenant-123";

    // When
    BillingEvent event = new BillingEvent(eventId, eventType, paymentId, merchantId,
        invoiceId, customerEmail, eventData, timestamp, tenantId);

    // Then
    assertEquals(eventId, event.getEventId());
    assertEquals(eventType, event.getEventType());
    assertEquals(paymentId, event.getPaymentId());
    assertEquals(merchantId, event.getMerchantId());
    assertEquals(invoiceId, event.getInvoiceId());
    assertEquals(customerEmail, event.getCustomerEmail());
    assertEquals(eventData, event.getEventData());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void defaultConstructor_shouldCreateEmptyBillingEvent() {
    // When
    BillingEvent event = new BillingEvent();

    // Then
    assertNotNull(event);
  }

  @Test
  void setters_shouldSetAllFields() {
    // Given
    BillingEvent event = new BillingEvent();
    String eventId = "event-456";
    String eventType = "PAYMENT_FAILED";
    String paymentId = "pay-456";
    String merchantId = "merchant-456";
    String invoiceId = "inv-456";
    String customerEmail = "user@example.com";
    Map<String, Object> eventData = new HashMap<>();
    Instant timestamp = Instant.now();
    String tenantId = "tenant-456";

    // When
    event.setEventId(eventId);
    event.setEventType(eventType);
    event.setPaymentId(paymentId);
    event.setMerchantId(merchantId);
    event.setInvoiceId(invoiceId);
    event.setCustomerEmail(customerEmail);
    event.setEventData(eventData);
    event.setTimestamp(timestamp);
    event.setTenantId(tenantId);

    // Then
    assertEquals(eventId, event.getEventId());
    assertEquals(eventType, event.getEventType());
    assertEquals(paymentId, event.getPaymentId());
    assertEquals(merchantId, event.getMerchantId());
    assertEquals(invoiceId, event.getInvoiceId());
    assertEquals(customerEmail, event.getCustomerEmail());
    assertEquals(eventData, event.getEventData());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void paymentSuccessful_shouldCreatePaymentSuccessfulEvent() {
    // Given
    String paymentId = "pay-789";
    String tenantId = "tenant-789";
    String customerEmail = "customer@example.com";

    // When
    BillingEvent event = BillingEvent.paymentSuccessful(paymentId, tenantId, customerEmail);

    // Then
    assertNotNull(event.getEventId());
    assertEquals("PAYMENT_SUCCESSFUL", event.getEventType());
    assertEquals(paymentId, event.getPaymentId());
    assertNull(event.getMerchantId());
    assertNull(event.getInvoiceId());
    assertEquals(customerEmail, event.getCustomerEmail());
    assertNull(event.getEventData());
    assertNotNull(event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void paymentFailed_shouldCreatePaymentFailedEvent() {
    // Given
    String paymentId = "pay-101";
    String tenantId = "tenant-101";
    String customerEmail = "customer@example.com";

    // When
    BillingEvent event = BillingEvent.paymentFailed(paymentId, tenantId, customerEmail);

    // Then
    assertNotNull(event.getEventId());
    assertEquals("PAYMENT_FAILED", event.getEventType());
    assertEquals(paymentId, event.getPaymentId());
    assertNull(event.getMerchantId());
    assertNull(event.getInvoiceId());
    assertEquals(customerEmail, event.getCustomerEmail());
    assertNull(event.getEventData());
    assertNotNull(event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void paymentRefunded_shouldCreatePaymentRefundedEvent() {
    // Given
    String paymentId = "pay-202";
    String tenantId = "tenant-202";
    String customerEmail = "customer@example.com";

    // When
    BillingEvent event = BillingEvent.paymentRefunded(paymentId, tenantId, customerEmail);

    // Then
    assertNotNull(event.getEventId());
    assertEquals("PAYMENT_REFUNDED", event.getEventType());
    assertEquals(paymentId, event.getPaymentId());
    assertNull(event.getMerchantId());
    assertNull(event.getInvoiceId());
    assertEquals(customerEmail, event.getCustomerEmail());
    assertNull(event.getEventData());
    assertNotNull(event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void merchantOnboarding_shouldCreateMerchantOnboardingEvent() {
    // Given
    String merchantId = "merchant-303";
    String tenantId = "tenant-303";
    String merchantEmail = "merchant@example.com";

    // When
    BillingEvent event = BillingEvent.merchantOnboarding(merchantId, tenantId, merchantEmail);

    // Then
    assertNotNull(event.getEventId());
    assertEquals("MERCHANT_ONBOARDING", event.getEventType());
    assertNull(event.getPaymentId());
    assertEquals(merchantId, event.getMerchantId());
    assertNull(event.getInvoiceId());
    assertEquals(merchantEmail, event.getCustomerEmail());
    assertNull(event.getEventData());
    assertNotNull(event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }

  @Test
  void invoiceGenerated_shouldCreateInvoiceGeneratedEvent() {
    // Given
    String invoiceId = "inv-404";
    String tenantId = "tenant-404";
    String customerEmail = "customer@example.com";

    // When
    BillingEvent event = BillingEvent.invoiceGenerated(invoiceId, tenantId, customerEmail);

    // Then
    assertNotNull(event.getEventId());
    assertEquals("INVOICE_GENERATED", event.getEventType());
    assertNull(event.getPaymentId());
    assertNull(event.getMerchantId());
    assertEquals(invoiceId, event.getInvoiceId());
    assertEquals(customerEmail, event.getCustomerEmail());
    assertNull(event.getEventData());
    assertNotNull(event.getTimestamp());
    assertEquals(tenantId, event.getTenantId());
  }
}
