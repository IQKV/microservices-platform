package com.iqscaffold.billingservice.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Invoice aggregate root.
 * Tests business logic, invariants, and state transitions.
 */
@DisplayName("Invoice Aggregate Tests")
class InvoiceTest {

  private UUID tenantId;
  private Subscription subscription;
  private LocalDateTime periodStart;
  private LocalDateTime periodEnd;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    var userId = UUID.randomUUID();

    var plan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Monthly",
        "Pro plan",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("29.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        0,
        true
    );

    subscription = Subscription.createActive(tenantId, userId, plan);
    periodStart = LocalDateTime.now().minusDays(30);
    periodEnd = LocalDateTime.now();
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create draft invoice with valid parameters")
    void shouldCreateDraftInvoiceWithValidParameters() {
      // When
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // Then
      assertNotNull(invoice);
      assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
      assertEquals(subscription, invoice.getSubscription());
      assertEquals(tenantId, invoice.getTenantId());
      assertEquals("INV-2024-001", invoice.getInvoiceNumber());
      assertEquals("USD", invoice.getCurrency());
      assertEquals(periodStart, invoice.getPeriodStart());
      assertEquals(periodEnd, invoice.getPeriodEnd());
      assertNotNull(invoice.getDueDate());
      assertEquals(BigDecimal.ZERO.setScale(2), invoice.getSubtotal());
      assertEquals(BigDecimal.ZERO.setScale(2), invoice.getTax());
      assertEquals(BigDecimal.ZERO.setScale(2), invoice.getTotal());
      assertEquals(0, invoice.getLineItemCount());
    }

    @Test
    @DisplayName("Should throw exception when subscription is null")
    void shouldThrowExceptionWhenSubscriptionIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Invoice.createDraft(
              null,
              tenantId,
              "INV-2024-001",
              "USD",
              periodStart,
              periodEnd,
              7
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void shouldThrowExceptionWhenTenantIdIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Invoice.createDraft(
              subscription,
              null,
              "INV-2024-001",
              "USD",
              periodStart,
              periodEnd,
              7
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when invoice number is blank")
    void shouldThrowExceptionWhenInvoiceNumberIsBlank() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Invoice.createDraft(
              subscription,
              tenantId,
              "   ",
              "USD",
              periodStart,
              periodEnd,
              7
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when period end is before period start")
    void shouldThrowExceptionWhenPeriodEndIsBeforePeriodStart() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Invoice.createDraft(
              subscription,
              tenantId,
              "INV-2024-001",
              "USD",
              periodEnd,
              periodStart,
              7
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when due days is negative")
    void shouldThrowExceptionWhenDueDaysIsNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Invoice.createDraft(
              subscription,
              tenantId,
              "INV-2024-001",
              "USD",
              periodStart,
              periodEnd,
              -1
          )
      );
    }
  }

  @Nested
  @DisplayName("Line Item Management Tests")
  class LineItemManagementTests {

    @Test
    @DisplayName("Should add line item to draft invoice")
    void shouldAddLineItemToDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      var lineItem = InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      );

      // When
      invoice.addLineItem(lineItem);

      // Then
      assertEquals(1, invoice.getLineItemCount());
      assertEquals(new BigDecimal("29.99"), invoice.getSubtotal());
      assertEquals(new BigDecimal("29.99"), invoice.getTotal());
    }

    @Test
    @DisplayName("Should add multiple line items and calculate totals")
    void shouldAddMultipleLineItemsAndCalculateTotals() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      var lineItem1 = InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      );

      var lineItem2 = InvoiceLineItem.usageCharge(
          "Overage charges",
          1L,
          new BigDecimal("10.00")
      );

      // When
      invoice.addLineItem(lineItem1);
      invoice.addLineItem(lineItem2);

      // Then
      assertEquals(2, invoice.getLineItemCount());
      assertEquals(new BigDecimal("39.99"), invoice.getSubtotal());
      assertEquals(new BigDecimal("39.99"), invoice.getTotal());
    }

    @Test
    @DisplayName("Should throw exception when adding line item to non-draft invoice")
    void shouldThrowExceptionWhenAddingLineItemToNonDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      var lineItem = InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      );

      invoice.addLineItem(lineItem);
      invoice.finalize();

      var newLineItem = InvoiceLineItem.usageCharge(
          "Additional charge",
          1L,
          new BigDecimal("10.00")
      );

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> invoice.addLineItem(newLineItem)
      );
    }

    @Test
    @DisplayName("Should remove line item from draft invoice")
    void shouldRemoveLineItemFromDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      var lineItem = InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      );

      invoice.addLineItem(lineItem);

      // When
      invoice.removeLineItem(0);

      // Then
      assertEquals(0, invoice.getLineItemCount());
      assertEquals(BigDecimal.ZERO.setScale(2), invoice.getSubtotal());
      assertEquals(BigDecimal.ZERO.setScale(2), invoice.getTotal());
    }

    @Test
    @DisplayName("Should clear all line items from draft invoice")
    void shouldClearAllLineItemsFromDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Item 1",
          new BigDecimal("10.00")
      ));

      invoice.addLineItem(InvoiceLineItem.usageCharge(
          "Item 2",
          1L,
          new BigDecimal("20.00")
      ));

      // When
      invoice.clearLineItems();

      // Then
      assertEquals(0, invoice.getLineItemCount());
      assertEquals(BigDecimal.ZERO.setScale(2), invoice.getSubtotal());
    }
  }

  @Nested
  @DisplayName("Tax Management Tests")
  class TaxManagementTests {

    @Test
    @DisplayName("Should set tax on draft invoice")
    void shouldSetTaxOnDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("100.00")
      ));

      // When
      invoice.setTax(new BigDecimal("10.00"));

      // Then
      assertEquals(new BigDecimal("10.00"), invoice.getTax());
      assertEquals(new BigDecimal("110.00"), invoice.getTotal());
    }

    @Test
    @DisplayName("Should throw exception when setting negative tax")
    void shouldThrowExceptionWhenSettingNegativeTax() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> invoice.setTax(new BigDecimal("-5.00"))
      );
    }

    @Test
    @DisplayName("Should throw exception when setting tax on non-draft invoice")
    void shouldThrowExceptionWhenSettingTaxOnNonDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("100.00")
      ));

      invoice.finalize();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> invoice.setTax(new BigDecimal("10.00"))
      );
    }
  }

  @Nested
  @DisplayName("Status Transition Tests")
  class StatusTransitionTests {

    @Test
    @DisplayName("Should finalize draft invoice")
    void shouldFinalizeDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      // When
      invoice.finalize();

      // Then
      assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when finalizing invoice with no line items")
    void shouldThrowExceptionWhenFinalizingInvoiceWithNoLineItems() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When & Then
      assertThrows(
          IllegalStateException.class,
          invoice::finalize
      );
    }

    @Test
    @DisplayName("Should mark open invoice as paid")
    void shouldMarkOpenInvoiceAsPaid() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      invoice.finalize();

      var paymentDate = LocalDateTime.now();

      // When
      invoice.markAsPaid(paymentDate);

      // Then
      assertEquals(InvoiceStatus.PAID, invoice.getStatus());
      assertEquals(paymentDate, invoice.getPaidAt());
    }

    @Test
    @DisplayName("Should throw exception when marking non-open invoice as paid")
    void shouldThrowExceptionWhenMarkingNonOpenInvoiceAsPaid() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> invoice.markAsPaid(LocalDateTime.now())
      );
    }

    @Test
    @DisplayName("Should void draft invoice")
    void shouldVoidDraftInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When
      invoice.voidInvoice("Customer request");

      // Then
      assertEquals(InvoiceStatus.VOID, invoice.getStatus());
    }

    @Test
    @DisplayName("Should void open invoice")
    void shouldVoidOpenInvoice() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      invoice.finalize();

      // When
      invoice.voidInvoice("Billing error");

      // Then
      assertEquals(InvoiceStatus.VOID, invoice.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when voiding with blank reason")
    void shouldThrowExceptionWhenVoidingWithBlankReason() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> invoice.voidInvoice("   ")
      );
    }

    @Test
    @DisplayName("Should mark open invoice as uncollectible")
    void shouldMarkOpenInvoiceAsUncollectible() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      invoice.finalize();

      // When
      invoice.markAsUncollectible("Payment retries exhausted");

      // Then
      assertEquals(InvoiceStatus.UNCOLLECTIBLE, invoice.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when marking non-open invoice as uncollectible")
    void shouldThrowExceptionWhenMarkingNonOpenInvoiceAsUncollectible() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> invoice.markAsUncollectible("Reason")
      );
    }
  }

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("Should check if invoice is overdue")
    void shouldCheckIfInvoiceIsOverdue() throws InterruptedException {
      // Given
      var pastPeriodStart = LocalDateTime.now().minusDays(30);
      var pastPeriodEnd = LocalDateTime.now();
      
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          pastPeriodStart,
          pastPeriodEnd,
          0  // Due immediately (now)
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      invoice.finalize();

      // Wait a tiny bit to ensure the due date is in the past
      Thread.sleep(10);

      // Then
      // Invoice should be overdue since due date was set to now and we waited
      assertTrue(invoice.isOverdue());
    }

    @Test
    @DisplayName("Should check if invoice can be modified")
    void shouldCheckIfInvoiceCanBeModified() {
      // Given
      var draftInvoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      var openInvoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-002",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      openInvoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      openInvoice.finalize();

      // Then
      assertTrue(draftInvoice.canModify());
      assertFalse(openInvoice.canModify());
    }

    @Test
    @DisplayName("Should get days until due")
    void shouldGetDaysUntilDue() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      invoice.finalize();

      // When
      var daysUntilDue = invoice.getDaysUntilDue();

      // Then
      assertTrue(daysUntilDue >= 0);
    }

    @Test
    @DisplayName("Should return unmodifiable list of line items")
    void shouldReturnUnmodifiableListOfLineItems() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Subscription fee",
          new BigDecimal("29.99")
      ));

      // When
      var lineItems = invoice.getLineItems();

      // Then
      assertThrows(
          UnsupportedOperationException.class,
          () -> lineItems.add(InvoiceLineItem.usageCharge(
              "New item",
              1L,
              new BigDecimal("10.00")
          ))
      );
    }
  }

  @Nested
  @DisplayName("Metadata Tests")
  class MetadataTests {

    @Test
    @DisplayName("Should add metadata")
    void shouldAddMetadata() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      // When
      invoice.addMetadata("key1", "value1");
      invoice.addMetadata("key2", 123);

      // Then
      var metadata = invoice.getMetadata();
      assertEquals("value1", metadata.get("key1"));
      assertEquals(123, metadata.get("key2"));
    }

    @Test
    @DisplayName("Should remove metadata")
    void shouldRemoveMetadata() {
      // Given
      var invoice = Invoice.createDraft(
          subscription,
          tenantId,
          "INV-2024-001",
          "USD",
          periodStart,
          periodEnd,
          7
      );

      invoice.addMetadata("key1", "value1");

      // When
      invoice.removeMetadata("key1");

      // Then
      var metadata = invoice.getMetadata();
      assertFalse(metadata.containsKey("key1"));
    }
  }
}
