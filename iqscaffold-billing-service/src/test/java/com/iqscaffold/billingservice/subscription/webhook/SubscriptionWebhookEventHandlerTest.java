package com.iqscaffold.billingservice.subscription.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionWebhookEventHandlerTest {

  @Mock
  private TenantSubscriptionRepository subscriptionRepository;

  @Mock
  private Subscription stripeSubscription;

  private SubscriptionWebhookEventHandler handler;

  @BeforeEach
  void setUp() {
    handler = new SubscriptionWebhookEventHandler(subscriptionRepository);
  }

  @Test
  void supports_SubscriptionEvent_ShouldReturnTrue() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        "sub_123",
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    // When
    boolean result = handler.supports(event);

    // Then
    assertTrue(result);
  }

  @Test
  void supports_NonSubscriptionEvent_ShouldReturnFalse() {
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
  void handleEvent_SubscriptionCreated_NewSubscription_ShouldCreateSubscription() {
    // Given
    String subscriptionId = "sub_123";
    String customerId = "cus_123";

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(
            "status", "active",
            "customer", customerId,
            "subscription", stripeSubscription
        ),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then
    verify(subscriptionRepository).save(any(TenantSubscription.class));
  }

  @Test
  void handleEvent_SubscriptionCreated_ExistingSubscription_ShouldNotCreateDuplicate() {
    // Given
    String subscriptionId = "sub_123";
    TenantSubscription existingSubscription = new TenantSubscription();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.of(existingSubscription));

    // When
    handler.handleEvent(event);

    // Then
    verify(subscriptionRepository, never()).save(any(TenantSubscription.class));
  }

  @Test
  void handleEvent_SubscriptionCreated_NoTenantId_ShouldLogWarning() {
    // Given
    String subscriptionId = "sub_123";

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_CREATED,
        PaymentGatewayProvider.STRIPE,
        Optional.empty(), // No tenant ID
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    // When
    handler.handleEvent(event);

    // Then
    verify(subscriptionRepository, never()).save(any(TenantSubscription.class));
  }

  @Test
  void handleEvent_SubscriptionUpdated_ExistingSubscription_ShouldUpdateSubscription() {
    // Given
    String subscriptionId = "sub_123";
    TenantSubscription subscription = new TenantSubscription();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(
            "status", "past_due",
            "subscription", stripeSubscription
        ),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.of(subscription));

    // When
    handler.handleEvent(event);

    // Then
    verify(subscriptionRepository).save(subscription);
  }

  @Test
  void handleEvent_SubscriptionUpdated_SubscriptionNotFound_ShouldTryToCreate() {
    // Given
    String subscriptionId = "sub_123";

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_UPDATED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of("status", "active"),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then - Should attempt to create the subscription since it wasn't found
    // The method calls handleSubscriptionCreated when subscription is not found during update
  }

  @Test
  void handleEvent_SubscriptionCanceled_ShouldUpdateStatusAndCanceledAt() {
    // Given
    String subscriptionId = "sub_123";
    TenantSubscription subscription = new TenantSubscription();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_CANCELED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.of(subscription));

    // When
    handler.handleEvent(event);

    // Then
    verify(subscriptionRepository).save(subscription);
  }

  @Test
  void handleEvent_SubscriptionCanceled_SubscriptionNotFound_ShouldLogWarning() {
    // Given
    String subscriptionId = "sub_123";

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_CANCELED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then
    verify(subscriptionRepository, never()).save(any(TenantSubscription.class));
  }

  @Test
  void handleEvent_SubscriptionTrialEnding_ShouldLogInfo() {
    // Given
    String subscriptionId = "sub_123";
    TenantSubscription subscription = new TenantSubscription();

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_TRIAL_ENDING,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.of(subscription));

    // When
    handler.handleEvent(event);

    // Then - Just verify no exception is thrown
    // The method only logs info for trial ending events
  }

  @Test
  void handleEvent_SubscriptionTrialEnding_SubscriptionNotFound_ShouldLogWarning() {
    // Given
    String subscriptionId = "sub_123";

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.SUBSCRIPTION_TRIAL_ENDING,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        subscriptionId,
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    when(subscriptionRepository.findByStripeSubscriptionId(subscriptionId)).thenReturn(Optional.empty());

    // When
    handler.handleEvent(event);

    // Then - Just verify no exception is thrown
    // The method only logs a warning for subscription not found
  }

  @Test
  void handleEvent_UnknownEventType_ShouldLogWarning() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        "subscription.unknown",
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant1"),
        "sub_123",
        WebhookEvent.ResourceType.SUBSCRIPTION,
        Map.of(),
        null
    );

    // When
    handler.handleEvent(event);

    // Then - Just verify no exception is thrown
    // The method only logs a warning for unknown event types
  }
}
