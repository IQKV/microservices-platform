package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Event for billing-related activities
 */
public class BillingEvent {

  private String eventId;
  private String eventType;
  private String paymentId;
  private String merchantId;
  private String invoiceId;
  private String customerEmail;
  private Map<String, Object> eventData;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  private String tenantId;

  public BillingEvent() {
  }

  public BillingEvent(final String eventId, final String eventType, final String paymentId,
                      final String merchantId, final String invoiceId, final String customerEmail,
                      final Map<String, Object> eventData, final Instant timestamp, final String tenantId) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.paymentId = paymentId;
    this.merchantId = merchantId;
    this.invoiceId = invoiceId;
    this.customerEmail = customerEmail;
    this.eventData = eventData;
    this.timestamp = timestamp;
    this.tenantId = tenantId;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public void setCustomerEmail(String customerEmail) {
    this.customerEmail = customerEmail;
  }

  public Map<String, Object> getEventData() {
    return eventData;
  }

  public void setEventData(Map<String, Object> eventData) {
    this.eventData = eventData;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public static BillingEvent paymentSuccessful(String paymentId, String tenantId, String customerEmail) {
    return new BillingEvent(
        java.util.UUID.randomUUID().toString(),
        "PAYMENT_SUCCESSFUL",
        paymentId,
        null,
        null,
        customerEmail,
        null,
        Instant.now(),
        tenantId
    );
  }

  public static BillingEvent paymentFailed(String paymentId, String tenantId, String customerEmail) {
    return new BillingEvent(
        java.util.UUID.randomUUID().toString(),
        "PAYMENT_FAILED",
        paymentId,
        null,
        null,
        customerEmail,
        null,
        Instant.now(),
        tenantId
    );
  }

  public static BillingEvent paymentRefunded(String paymentId, String tenantId, String customerEmail) {
    return new BillingEvent(
        java.util.UUID.randomUUID().toString(),
        "PAYMENT_REFUNDED",
        paymentId,
        null,
        null,
        customerEmail,
        null,
        Instant.now(),
        tenantId
    );
  }

  public static BillingEvent merchantOnboarding(String merchantId, String tenantId, String merchantEmail) {
    return new BillingEvent(
        java.util.UUID.randomUUID().toString(),
        "MERCHANT_ONBOARDING",
        null,
        merchantId,
        null,
        merchantEmail,
        null,
        Instant.now(),
        tenantId
    );
  }

  public static BillingEvent invoiceGenerated(String invoiceId, String tenantId, String customerEmail) {
    return new BillingEvent(
        java.util.UUID.randomUUID().toString(),
        "INVOICE_GENERATED",
        null,
        null,
        invoiceId,
        customerEmail,
        null,
        Instant.now(),
        tenantId
    );
  }
}
