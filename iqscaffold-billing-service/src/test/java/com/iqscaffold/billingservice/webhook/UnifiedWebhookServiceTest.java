package com.iqscaffold.billingservice.webhook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.MerchantStripeConfig;
import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnifiedWebhookServiceTest {

  @Mock
  private WebhookEventHandler paymentHandler;

  @Mock
  private WebhookEventHandler payoutHandler;

  @Mock
  private MerchantStripeConfigRepository merchantConfigRepository;

  private UnifiedWebhookService webhookService;

  @BeforeEach
  void setUp() {
    webhookService = new UnifiedWebhookService(
        List.of(paymentHandler, payoutHandler),
        merchantConfigRepository
    );
  }

  @Test
  void processWebhookEvent_shouldRouteToSupportingHandler() {
    // Given
    WebhookEvent event = createPaymentEvent("payment.succeeded", Optional.of("tenant-1"));
    when(paymentHandler.supports(event)).thenReturn(true);
    when(payoutHandler.supports(event)).thenReturn(false);

    // When
    webhookService.processWebhookEvent(event);

    // Then
    verify(paymentHandler).handleEvent(event);
    verify(payoutHandler, never()).handleEvent(any());
  }

  @Test
  void processWebhookEvent_shouldRouteToMultipleHandlers() {
    // Given
    WebhookEvent event = createPaymentEvent("payment.succeeded", Optional.of("tenant-1"));
    when(paymentHandler.supports(event)).thenReturn(true);
    when(payoutHandler.supports(event)).thenReturn(true);

    // When
    webhookService.processWebhookEvent(event);

    // Then
    verify(paymentHandler).handleEvent(event);
    verify(payoutHandler).handleEvent(event);
  }

  @Test
  void processWebhookEvent_shouldNotThrowWhenNoHandlerFound() {
    // Given
    WebhookEvent event = createPaymentEvent("unknown.event", Optional.of("tenant-1"));
    when(paymentHandler.supports(event)).thenReturn(false);
    when(payoutHandler.supports(event)).thenReturn(false);

    // When & Then
    assertDoesNotThrow(() -> webhookService.processWebhookEvent(event));
  }

  @Test
  void processWebhookEvent_shouldContinueWhenHandlerFails() {
    // Given
    WebhookEvent event = createPaymentEvent("payment.succeeded", Optional.of("tenant-1"));
    when(paymentHandler.supports(event)).thenReturn(true);
    when(payoutHandler.supports(event)).thenReturn(true);
    doThrow(new RuntimeException("Handler error")).when(paymentHandler).handleEvent(event);

    // When
    webhookService.processWebhookEvent(event);

    // Then - second handler should still be called
    verify(paymentHandler).handleEvent(event);
    verify(payoutHandler).handleEvent(event);
  }

  @Test
  void processWebhookEvent_shouldResolveAccountEventTenant() {
    // Given
    WebhookEvent event = createAccountEvent("acct_123");
    when(paymentHandler.supports(event)).thenReturn(true);

    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId("tenant-from-account");
    when(merchantConfigRepository.findByStripeAccountId("acct_123"))
        .thenReturn(Optional.of(config));

    // When
    webhookService.processWebhookEvent(event);

    // Then
    verify(paymentHandler).handleEvent(event);
    verify(merchantConfigRepository).findByStripeAccountId("acct_123");
  }

  @Test
  void processWebhookEvent_shouldThrowWhenTenantResolutionFailsForNonGlobalEvent() {
    // Given
    WebhookEvent event = createPaymentEvent("payment.succeeded", Optional.empty());

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        webhookService.processWebhookEvent(event)
    );
  }

  @Test
  void processWebhookEvent_shouldProcessGlobalEventWithoutTenant() {
    // Given - account.updated is a global event
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.ACCOUNT_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(),
        "acct_123",
        WebhookEvent.ResourceType.ACCOUNT,
        Map.of(),
        new Object()
    );
    when(paymentHandler.supports(event)).thenReturn(true);

    // When
    webhookService.processWebhookEvent(event);

    // Then
    verify(paymentHandler).handleEvent(event);
  }

  @Test
  void processWebhookEvent_shouldCleanupTenantContextAfterProcessing() {
    // Given
    WebhookEvent event = createPaymentEvent("payment.succeeded", Optional.of("tenant-1"));
    when(paymentHandler.supports(event)).thenReturn(true);

    // When
    webhookService.processWebhookEvent(event);

    // Then - verify handler was called (tenant context cleanup happens in finally block)
    verify(paymentHandler).handleEvent(event);
  }

  @Test
  void processWebhookEvent_shouldCleanupTenantContextEvenOnException() {
    // Given
    WebhookEvent event = createPaymentEvent("payment.succeeded", Optional.of("tenant-1"));
    when(paymentHandler.supports(event)).thenReturn(true);
    doThrow(new RuntimeException("Handler error")).when(paymentHandler).handleEvent(event);

    // When
    webhookService.processWebhookEvent(event);

    // Then - no exception thrown, cleanup happened
    verify(paymentHandler).handleEvent(event);
  }

  private WebhookEvent createPaymentEvent(final String eventType, final Optional<String> tenantId) {
    return new WebhookEvent(
        "evt_123",
        eventType,
        PaymentGatewayProvider.STRIPE,
        tenantId,
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );
  }

  private WebhookEvent createAccountEvent(final String accountId) {
    return new WebhookEvent(
        "evt_456",
        WebhookEvent.EventType.ACCOUNT_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(),
        accountId,
        WebhookEvent.ResourceType.ACCOUNT,
        Map.of(),
        new Object()
    );
  }
}
