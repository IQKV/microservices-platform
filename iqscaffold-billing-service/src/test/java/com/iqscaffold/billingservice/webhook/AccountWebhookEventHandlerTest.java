package com.iqscaffold.billingservice.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.MerchantStripeConfig;
import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.infrastructure.messaging.EventPublisher;
import com.iqscaffold.billingservice.infrastructure.messaging.MerchantCapabilitiesUpdatedEvent;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountWebhookEventHandlerTest {

  @Mock
  private MerchantStripeConfigRepository merchantConfigRepository;

  @Mock
  private EventPublisher eventPublisher;

  private AccountWebhookEventHandler handler;

  @BeforeEach
  void setUp() {
    handler = new AccountWebhookEventHandler(merchantConfigRepository, eventPublisher);
  }

  @Test
  void supports_shouldReturnTrueForAccountEvents() {
    // Given
    WebhookEvent event = createAccountEvent();

    // When
    final boolean supports = handler.supports(event);

    // Then
    assertTrue(supports);
  }

  @Test
  void supports_shouldReturnFalseForNonAccountEvents() {
    // Given
    WebhookEvent event = createPaymentEvent();

    // When
    final boolean supports = handler.supports(event);

    // Then
    assertFalse(supports);
  }

  @Test
  void handleEvent_shouldUpdateMerchantCapabilities() {
    // Given
    com.stripe.model.Account stripeAccount = new com.stripe.model.Account();
    stripeAccount.setId("acct_123");
    stripeAccount.setChargesEnabled(true);
    stripeAccount.setPayoutsEnabled(true);

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.ACCOUNT_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(),
        "acct_123",
        WebhookEvent.ResourceType.ACCOUNT,
        Map.of(),
        stripeAccount
    );

    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId("tenant-1");
    config.setOrganizationId(100L);
    config.setStripeAccountId("acct_123");

    when(merchantConfigRepository.findByStripeAccountId("acct_123"))
        .thenReturn(Optional.of(config));

    // When
    handler.handleEvent(event);

    // Then
    verify(merchantConfigRepository).save(config);
    assertTrue(config.isChargesEnabled());
    assertTrue(config.isPayoutsEnabled());
    verify(eventPublisher).publishMerchantCapabilitiesUpdated(any(MerchantCapabilitiesUpdatedEvent.class));
  }

  @Test
  void handleEvent_shouldNotPublishEventWhenOrganizationIdIsNull() {
    // Given
    com.stripe.model.Account stripeAccount = new com.stripe.model.Account();
    stripeAccount.setId("acct_123");
    stripeAccount.setChargesEnabled(true);
    stripeAccount.setPayoutsEnabled(false);

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.ACCOUNT_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(),
        "acct_123",
        WebhookEvent.ResourceType.ACCOUNT,
        Map.of(),
        stripeAccount
    );

    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId("tenant-1");
    config.setOrganizationId(null); // No organization ID

    when(merchantConfigRepository.findByStripeAccountId("acct_123"))
        .thenReturn(Optional.of(config));

    // When
    handler.handleEvent(event);

    // Then
    verify(merchantConfigRepository).save(config);
    verify(eventPublisher, never()).publishMerchantCapabilitiesUpdated(any());
  }

  @Test
  void handleEvent_shouldHandleUnknownAccount() {
    // Given
    com.stripe.model.Account stripeAccount = new com.stripe.model.Account();
    stripeAccount.setId("acct_unknown");

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.ACCOUNT_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(),
        "acct_unknown",
        WebhookEvent.ResourceType.ACCOUNT,
        Map.of(),
        stripeAccount
    );

    when(merchantConfigRepository.findByStripeAccountId("acct_unknown"))
        .thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then - should not throw exception
    verify(merchantConfigRepository, never()).save(any());
    verify(eventPublisher, never()).publishMerchantCapabilitiesUpdated(any());
  }

  @Test
  void handleEvent_shouldThrowExceptionForUnsupportedEvent() {
    // Given
    WebhookEvent event = createPaymentEvent();

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        handler.handleEvent(event)
    );
  }

  private WebhookEvent createAccountEvent() {
    return new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.ACCOUNT_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(),
        "acct_123",
        WebhookEvent.ResourceType.ACCOUNT,
        Map.of(),
        new Object()
    );
  }

  private WebhookEvent createPaymentEvent() {
    return new WebhookEvent(
        "evt_456",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );
  }
}
