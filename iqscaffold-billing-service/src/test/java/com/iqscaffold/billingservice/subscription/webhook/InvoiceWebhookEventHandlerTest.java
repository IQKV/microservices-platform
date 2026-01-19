package com.iqscaffold.billingservice.subscription.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoice;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoiceRepository;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.stripe.model.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceWebhookEventHandlerTest {

  @Mock
  private SubscriptionInvoiceRepository invoiceRepository;

  @Mock
  private TenantSubscriptionRepository subscriptionRepository;

  @Mock
  private StripeInvoiceDataExtractor stripeDataExtractor;

  @Mock
  private Invoice stripeInvoice;

  private InvoiceWebhookEventHandler handler;

  @BeforeEach
  void setUp() {
    handler = new InvoiceWebhookEventHandler(invoiceRepository, subscriptionRepository, stripeDataExtractor);
  }

  @Test
  void supports_InvoiceEvent_ShouldReturnTrue() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        "in_123",
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    // When
    boolean result = handler.supports(event);

    // Then
    assertTrue(result);
  }

  @Test
  void supports_NonInvoiceEvent_ShouldReturnFalse() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        null
    );

    // When
    boolean result = handler.supports(event);

    // Then
    assertFalse(result);
  }

  @Test
  void handleEvent_InvoiceCreated_NewInvoice_ShouldCreateInvoice() {
    // Given
    String invoiceId = "in_123";
    String subscriptionId = "sub_123";
    TenantSubscription tenantSubscription = new TenantSubscription();

    when(stripeInvoice.getId()).thenReturn(invoiceId);
    when(stripeInvoice.getNumber()).thenReturn("INV-001");
    when(stripeInvoice.getAmountDue()).thenReturn(1000L);
    when(stripeInvoice.getAmountPaid()).thenReturn(0L);
    when(stripeInvoice.getCurrency()).thenReturn("usd");
    when(stripeInvoice.getStatus()).thenReturn("open");
    when(stripeInvoice.getHostedInvoiceUrl()).thenReturn("https://invoice.stripe.com/123");
    when(stripeInvoice.getInvoicePdf()).thenReturn("https://invoice.stripe.com/123.pdf");
    when(stripeInvoice.getDueDate()).thenReturn(Instant.now().getEpochSecond());

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of("invoice", stripeInvoice, "subscription", subscriptionId),
        null
    );

    StripeInvoiceDataExtractor.InvoiceData invoiceData = 
        new StripeInvoiceDataExtractor.InvoiceData(invoiceId, subscriptionId);

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.empty());
    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.of(tenantSubscription));
    when(stripeDataExtractor.extractBasicData(event)).thenReturn(invoiceData);
    when(stripeDataExtractor.extractStripeInvoice(event)).thenReturn(stripeInvoice);

    // When
    handler.handleEvent(event);

    // Then
    verify(invoiceRepository).save(any(SubscriptionInvoice.class));
    verify(stripeDataExtractor).populateFromStripeInvoice(any(SubscriptionInvoice.class), any(Invoice.class));
  }

  @Test
  void handleEvent_InvoiceCreated_ExistingInvoice_ShouldNotCreateDuplicate() {
    // Given
    String invoiceId = "in_123";
    SubscriptionInvoice existingInvoice = new SubscriptionInvoice();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.of(existingInvoice));

    // When
    handler.handleEvent(event);

    // Then
    verify(invoiceRepository, never()).save(any(SubscriptionInvoice.class));
  }

  @Test
  void handleEvent_InvoiceFinalized_ShouldUpdateStatus() {
    // Given
    String invoiceId = "in_123";
    SubscriptionInvoice invoice = new SubscriptionInvoice();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_FINALIZED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));

    // When
    handler.handleEvent(event);

    // Then
    verify(invoiceRepository).save(invoice);
  }

  @Test
  void handleEvent_InvoicePaid_ShouldUpdateStatusAndPaidAt() {
    // Given
    String invoiceId = "in_123";
    SubscriptionInvoice invoice = new SubscriptionInvoice();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_PAID,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));

    // When
    handler.handleEvent(event);

    // Then - Should save twice: once for status update, once for paidAt timestamp
    verify(invoiceRepository, times(2)).save(invoice);
  }

  @Test
  void handleEvent_InvoicePaymentFailed_ShouldLogWarning() {
    // Given
    String invoiceId = "in_123";

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_PAYMENT_FAILED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    // When
    handler.handleEvent(event);

    // Then - Just verify no exception is thrown
    // The method only logs a warning for payment failed events
  }

  @Test
  void handleEvent_InvoiceVoided_ShouldUpdateStatus() {
    // Given
    String invoiceId = "in_123";
    SubscriptionInvoice invoice = new SubscriptionInvoice();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_VOIDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));

    // When
    handler.handleEvent(event);

    // Then
    verify(invoiceRepository).save(invoice);
  }

  @Test
  void handleEvent_UnknownEventType_ShouldLogWarning() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        "invoice.unknown",
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        "in_123",
        WebhookEvent.ResourceType.INVOICE,
        Map.of(),
        null
    );

    // When
    handler.handleEvent(event);

    // Then - Just verify no exception is thrown
    // The method only logs a warning for unknown event types
  }

  @Test
  void handleEvent_InvoiceCreated_NoSubscriptionInMetadata_ShouldLogWarning() {
    // Given
    String invoiceId = "in_123";

    when(stripeInvoice.getId()).thenReturn(invoiceId);

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of("invoice", stripeInvoice),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then
    verify(invoiceRepository, never()).save(any(SubscriptionInvoice.class));
  }

  @Test
  void handleEvent_InvoiceCreated_SubscriptionNotFound_ShouldLogWarning() {
    // Given
    String invoiceId = "in_123";
    String subscriptionId = "sub_123";

    when(stripeInvoice.getId()).thenReturn(invoiceId);

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of("invoice", stripeInvoice, "subscription", subscriptionId),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.empty());
    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then
    verify(invoiceRepository, never()).save(any(SubscriptionInvoice.class));
  }

  @Test
  void handleEvent_StatusUpdate_InvoiceNotFound_ShouldTryToCreate() {
    // Given
    String invoiceId = "in_123";

    when(stripeInvoice.getId()).thenReturn(invoiceId);

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.INVOICE_FINALIZED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        invoiceId,
        WebhookEvent.ResourceType.INVOICE,
        Map.of("invoice", stripeInvoice),
        null
    );

    when(invoiceRepository.findByStripeInvoiceId(invoiceId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then - Should attempt to create the invoice since it wasn't found
    // The method calls handleInvoiceCreated when invoice is not found during status update
  }
}
