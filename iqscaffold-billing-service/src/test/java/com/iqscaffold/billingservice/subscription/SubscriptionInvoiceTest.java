package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionInvoiceTest {

  private SubscriptionInvoice invoice;
  private TenantSubscription tenantSubscription;

  @BeforeEach
  void setUp() {
    invoice = new SubscriptionInvoice();
    tenantSubscription = new TenantSubscription();
    tenantSubscription.setId(UUID.randomUUID());
  }

  @Test
  void constructor_shouldCreateEmptyInvoice() {
    // When
    SubscriptionInvoice newInvoice = new SubscriptionInvoice();

    // Then
    assertNull(newInvoice.getId());
    assertNull(newInvoice.getTenantSubscription());
    assertNull(newInvoice.getInvoiceNumber());
    assertNull(newInvoice.getAmountDue());
    assertEquals(BigDecimal.ZERO, newInvoice.getAmountPaid()); // Default value in entity
    assertNull(newInvoice.getCurrency());
    assertNull(newInvoice.getStatus());
    assertNull(newInvoice.getDueDate());
    assertNull(newInvoice.getPaidAt());
    assertNull(newInvoice.getStripeInvoiceId());
    assertNull(newInvoice.getHostedInvoiceUrl());
    assertNull(newInvoice.getInvoicePdfUrl());
    assertNull(newInvoice.getMetadata());
    assertNull(newInvoice.getCreatedAt());
    assertNull(newInvoice.getUpdatedAt());
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    String invoiceNumber = "INV-001";
    BigDecimal amountDue = new BigDecimal("99.99");
    BigDecimal amountPaid = new BigDecimal("50.00");
    String currency = "USD";
    InvoiceStatus status = InvoiceStatus.OPEN;
    Instant dueDate = Instant.now().plusSeconds(86400);
    Instant paidAt = Instant.now();
    String stripeInvoiceId = "in_123";
    String hostedInvoiceUrl = "https://invoice.stripe.com/123";
    String invoicePdfUrl = "https://invoice.stripe.com/123.pdf";
    String metadata = "{\"key\":\"value\"}";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    invoice.setId(id);
    invoice.setTenantSubscription(tenantSubscription);
    invoice.setInvoiceNumber(invoiceNumber);
    invoice.setAmountDue(amountDue);
    invoice.setAmountPaid(amountPaid);
    invoice.setCurrency(currency);
    invoice.setStatus(status);
    invoice.setDueDate(dueDate);
    invoice.setPaidAt(paidAt);
    invoice.setStripeInvoiceId(stripeInvoiceId);
    invoice.setHostedInvoiceUrl(hostedInvoiceUrl);
    invoice.setInvoicePdfUrl(invoicePdfUrl);
    invoice.setMetadata(metadata);
    invoice.setCreatedAt(createdAt);
    invoice.setUpdatedAt(updatedAt);

    // Then
    assertEquals(id, invoice.getId());
    assertEquals(tenantSubscription, invoice.getTenantSubscription());
    assertEquals(invoiceNumber, invoice.getInvoiceNumber());
    assertEquals(amountDue, invoice.getAmountDue());
    assertEquals(amountPaid, invoice.getAmountPaid());
    assertEquals(currency, invoice.getCurrency());
    assertEquals(status, invoice.getStatus());
    assertEquals(dueDate, invoice.getDueDate());
    assertEquals(paidAt, invoice.getPaidAt());
    assertEquals(stripeInvoiceId, invoice.getStripeInvoiceId());
    assertEquals(hostedInvoiceUrl, invoice.getHostedInvoiceUrl());
    assertEquals(invoicePdfUrl, invoice.getInvoicePdfUrl());
    assertEquals(metadata, invoice.getMetadata());
    assertEquals(createdAt, invoice.getCreatedAt());
    assertEquals(updatedAt, invoice.getUpdatedAt());
  }

  @Test
  void onCreate_shouldGenerateIdAndTimestamps() {
    // Given
    invoice.setAmountDue(new BigDecimal("99.99"));
    invoice.setAmountPaid(BigDecimal.ZERO);
    invoice.setCurrency("USD");
    invoice.setStatus(InvoiceStatus.OPEN);

    // When
    invoice.onCreate();

    // Then
    assertNotNull(invoice.getId());
    assertNotNull(invoice.getCreatedAt());
    assertNotNull(invoice.getUpdatedAt());
    assertEquals(invoice.getCreatedAt(), invoice.getUpdatedAt());
  }

  @Test
  void onCreate_shouldNotOverrideExistingId() {
    // Given
    UUID existingId = UUID.randomUUID();
    invoice.setId(existingId);

    // When
    invoice.onCreate();

    // Then
    assertEquals(existingId, invoice.getId());
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
    // Given
    invoice.onCreate();
    Instant originalUpdatedAt = invoice.getUpdatedAt();
    Thread.sleep(1); // Ensure time difference

    // When
    invoice.onUpdate();

    // Then
    assertTrue(invoice.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void isPaid_shouldReturnTrueWhenStatusIsPaid() {
    // Given
    invoice.setStatus(InvoiceStatus.PAID);

    // When & Then
    assertTrue(invoice.isPaid());
  }

  @Test
  void isPaid_shouldReturnFalseWhenStatusIsNotPaid() {
    // Given
    invoice.setStatus(InvoiceStatus.OPEN);

    // When & Then
    assertFalse(invoice.isPaid());
  }

  @Test
  void isOpen_shouldReturnTrueWhenStatusIsOpen() {
    // Given
    invoice.setStatus(InvoiceStatus.OPEN);

    // When & Then
    assertTrue(invoice.isOpen());
  }

  @Test
  void isOpen_shouldReturnFalseWhenStatusIsNotOpen() {
    // Given
    invoice.setStatus(InvoiceStatus.PAID);

    // When & Then
    assertFalse(invoice.isOpen());
  }

  @Test
  void isOverdue_shouldReturnTrueWhenOpenAndPastDueDate() {
    // Given
    invoice.setStatus(InvoiceStatus.OPEN);
    invoice.setDueDate(Instant.now().minusSeconds(86400)); // 1 day ago

    // When & Then
    assertTrue(invoice.isOverdue());
  }

  @Test
  void isOverdue_shouldReturnFalseWhenOpenButNotPastDueDate() {
    // Given
    invoice.setStatus(InvoiceStatus.OPEN);
    invoice.setDueDate(Instant.now().plusSeconds(86400)); // 1 day from now

    // When & Then
    assertFalse(invoice.isOverdue());
  }

  @Test
  void isOverdue_shouldReturnFalseWhenNotOpen() {
    // Given
    invoice.setStatus(InvoiceStatus.PAID);
    invoice.setDueDate(Instant.now().minusSeconds(86400)); // 1 day ago

    // When & Then
    assertFalse(invoice.isOverdue());
  }

  @Test
  void isOverdue_shouldReturnFalseWhenDueDateIsNull() {
    // Given
    invoice.setStatus(InvoiceStatus.OPEN);
    invoice.setDueDate(null);

    // When & Then
    assertFalse(invoice.isOverdue());
  }

  @Test
  void getAmountRemaining_shouldCalculateCorrectly() {
    // Given
    invoice.setAmountDue(new BigDecimal("100.00"));
    invoice.setAmountPaid(new BigDecimal("30.00"));

    // When
    BigDecimal remaining = invoice.getAmountRemaining();

    // Then
    assertEquals(new BigDecimal("70.00"), remaining);
  }

  @Test
  void getAmountRemaining_shouldReturnFullAmountWhenNothingPaid() {
    // Given
    invoice.setAmountDue(new BigDecimal("100.00"));
    invoice.setAmountPaid(BigDecimal.ZERO);

    // When
    BigDecimal remaining = invoice.getAmountRemaining();

    // Then
    assertEquals(new BigDecimal("100.00"), remaining);
  }

  @Test
  void getAmountRemaining_shouldReturnZeroWhenFullyPaid() {
    // Given
    invoice.setAmountDue(new BigDecimal("100.00"));
    invoice.setAmountPaid(new BigDecimal("100.00"));

    // When
    BigDecimal remaining = invoice.getAmountRemaining();

    // Then
    assertEquals(new BigDecimal("0.00"), remaining);
  }
}