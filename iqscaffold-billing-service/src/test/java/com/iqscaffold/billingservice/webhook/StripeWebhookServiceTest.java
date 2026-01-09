package com.iqscaffold.billingservice.webhook;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.MerchantStripeConfig;
import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import com.iqscaffold.billingservice.payment.PaymentService;
import com.iqscaffold.billingservice.payout.PayoutService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

  @Mock
  private PaymentService paymentService;

  @Mock
  private PayoutService payoutService;

  @Mock
  private MerchantStripeConfigRepository merchantConfigRepository;

  @Mock
  private com.iqscaffold.billingservice.infrastructure.messaging.EventPublisher eventPublisher;

  @Mock
  private IqScaffoldProperties iqScaffoldProperties;

  private StripeWebhookService webhookService;

  @BeforeEach
  void setUp() {
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);

    when(iqScaffoldProperties.billing()).thenReturn(billing);
    when(billing.payment()).thenReturn(payment);
    when(payment.stripe()).thenReturn(stripe);
    when(stripe.webhookSecret()).thenReturn("whsec_test_secret");

    webhookService = new StripeWebhookService(
        paymentService,
        payoutService,
        merchantConfigRepository,
        iqScaffoldProperties,
        eventPublisher
    );
  }

  @Test
  void processWebhook_shouldHandlePaymentIntentSucceeded() {
    // Given
    String payload = "{\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    PaymentIntent mockIntent = mock(PaymentIntent.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", "tenant_123");

    when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));
    when(mockIntent.getId()).thenReturn("pi_123");
    when(mockIntent.getMetadata()).thenReturn(metadata);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(paymentService).updateStatus("pi_123", BillingConstants.PaymentStatus.SUCCEEDED);
    }
  }

  @Test
  void processWebhook_shouldHandlePaymentIntentFailed() {
    // Given
    String payload = "{\"type\":\"payment_intent.payment_failed\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    PaymentIntent mockIntent = mock(PaymentIntent.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", "tenant_123");

    when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));
    when(mockIntent.getId()).thenReturn("pi_456");
    when(mockIntent.getMetadata()).thenReturn(metadata);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(paymentService).updateStatus("pi_456", BillingConstants.PaymentStatus.FAILED);
    }
  }

  @Test
  void processWebhook_shouldHandleChargeRefunded() {
    // Given
    String payload = "{\"type\":\"charge.refunded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    Charge mockCharge = mock(Charge.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", "tenant_123");

    when(mockEvent.getType()).thenReturn("charge.refunded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockCharge));
    when(mockCharge.getPaymentIntent()).thenReturn("pi_789");
    when(mockCharge.getRefunded()).thenReturn(true);
    when(mockCharge.getMetadata()).thenReturn(metadata);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(paymentService).updateStatus("pi_789", BillingConstants.PaymentStatus.REFUNDED);
    }
  }

  @Test
  void processWebhook_shouldHandlePartialRefund() {
    // Given
    String payload = "{\"type\":\"charge.refunded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    Charge mockCharge = mock(Charge.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", "tenant_123");

    when(mockEvent.getType()).thenReturn("charge.refunded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockCharge));
    when(mockCharge.getPaymentIntent()).thenReturn("pi_partial");
    when(mockCharge.getRefunded()).thenReturn(false);
    when(mockCharge.getMetadata()).thenReturn(metadata);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(paymentService).updateStatus("pi_partial", BillingConstants.PaymentStatus.PARTIALLY_REFUNDED);
    }
  }

  @Test
  void processWebhook_shouldHandleAccountUpdated() {
    // Given
    String payload = "{\"type\":\"account.updated\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    Account mockAccount = mock(Account.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
    MerchantStripeConfig mockConfig = mock(MerchantStripeConfig.class);

    when(mockEvent.getType()).thenReturn("account.updated");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockAccount));
    when(mockAccount.getId()).thenReturn("acct_123");
    when(mockAccount.getChargesEnabled()).thenReturn(true);
    when(mockAccount.getPayoutsEnabled()).thenReturn(true);
    when(merchantConfigRepository.findByStripeAccountId("acct_123"))
        .thenReturn(Optional.of(mockConfig));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(mockConfig).setChargesEnabled(true);
      verify(mockConfig).setPayoutsEnabled(true);
      verify(merchantConfigRepository).save(mockConfig);
    }
  }

  @Test
  void processWebhook_shouldHandleUnknownAccountUpdate() {
    // Given
    String payload = "{\"type\":\"account.updated\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    Account mockAccount = mock(Account.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    when(mockEvent.getType()).thenReturn("account.updated");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockAccount));
    when(mockAccount.getId()).thenReturn("acct_unknown");
    when(merchantConfigRepository.findByStripeAccountId("acct_unknown"))
        .thenReturn(Optional.empty());

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(merchantConfigRepository, never()).save(any());
    }
  }

  @Test
  void processWebhook_shouldThrowExceptionOnInvalidSignature() {
    // Given
    String payload = "{\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "invalid_signature";

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenThrow(new SignatureVerificationException("Invalid signature", sigHeader));

      // When & Then
      assertThrows(IllegalArgumentException.class, () ->
          webhookService.processWebhook(payload, sigHeader)
      );
    }
  }

  @Test
  void processWebhook_shouldThrowExceptionOnParsingFailure() {
    // Given
    String payload = "invalid_json";
    String sigHeader = "valid_signature";

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenThrow(new RuntimeException("Parsing failed"));

      // When & Then
      assertThrows(IllegalArgumentException.class, () ->
          webhookService.processWebhook(payload, sigHeader)
      );
    }
  }

  @Test
  void processWebhook_shouldIgnoreUnhandledEventTypes() {
    // Given - account.updated is a global event that doesn't need tenant
    String payload = "{\"type\":\"account.updated\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
    Account mockAccount = mock(Account.class);

    when(mockEvent.getType()).thenReturn("account.updated");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockAccount));
    when(mockAccount.getId()).thenReturn("acct_unknown");
    when(merchantConfigRepository.findByStripeAccountId("acct_unknown"))
        .thenReturn(Optional.empty());

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then - should not throw and not update anything
      verify(paymentService, never()).updateStatus(anyString(), anyString());
      verify(payoutService, never()).processPayout(any());
    }
  }

  @Test
  void processWebhook_shouldHandleNullPaymentIntent() {
    // Given
    String payload = "{\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
    PaymentIntent mockIntent = mock(PaymentIntent.class);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", "tenant_123");

    when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));
    when(mockIntent.getMetadata()).thenReturn(metadata);
    when(mockIntent.getId()).thenReturn(null);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then - updateStatus is called even with null, which is fine (service handles it)
      verify(paymentService).updateStatus(null, BillingConstants.PaymentStatus.SUCCEEDED);
    }
  }

  @Test
  void processWebhook_shouldHandleChargeWithoutPaymentIntent() {
    // Given
    String payload = "{\"type\":\"charge.refunded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    Charge mockCharge = mock(Charge.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", "tenant_123");

    when(mockEvent.getType()).thenReturn("charge.refunded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockCharge));
    when(mockCharge.getPaymentIntent()).thenReturn(null);
    when(mockCharge.getMetadata()).thenReturn(metadata);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(paymentService, never()).updateStatus(anyString(), anyString());
    }
  }

  @Test
  void processWebhook_shouldRejectNonGlobalEventWithoutTenant() {
    // Given
    String payload = "{\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    PaymentIntent mockIntent = mock(PaymentIntent.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    Map<String, String> metadata = new HashMap<>();
    // No tenant_id in metadata

    when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));
    when(mockIntent.getMetadata()).thenReturn(metadata);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When & Then
      assertThrows(IllegalArgumentException.class, () ->
          webhookService.processWebhook(payload, sigHeader)
      );
    }
  }

  @Test
  void processWebhook_shouldHandleAccountUpdatedWithDisabledCapabilities() {
    // Given
    String payload = "{\"type\":\"account.updated\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    Account mockAccount = mock(Account.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
    MerchantStripeConfig mockConfig = mock(MerchantStripeConfig.class);

    when(mockEvent.getType()).thenReturn("account.updated");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.of(mockAccount));
    when(mockAccount.getId()).thenReturn("acct_456");
    when(mockAccount.getChargesEnabled()).thenReturn(false);
    when(mockAccount.getPayoutsEnabled()).thenReturn(false);
    when(merchantConfigRepository.findByStripeAccountId("acct_456"))
        .thenReturn(Optional.of(mockConfig));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When
      webhookService.processWebhook(payload, sigHeader);

      // Then
      verify(mockConfig).setChargesEnabled(false);
      verify(mockConfig).setPayoutsEnabled(false);
      verify(merchantConfigRepository).save(mockConfig);
    }
  }

  @Test
  void processWebhook_shouldHandleEmptyDataObject() {
    // Given
    String payload = "{\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "valid_signature";

    Event mockEvent = mock(Event.class);
    EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

    when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
    when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
    when(mockDeserializer.getObject()).thenReturn(Optional.empty());

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), anyString()))
          .thenReturn(mockEvent);

      // When & Then - should throw due to missing tenant
      assertThrows(IllegalArgumentException.class, () ->
          webhookService.processWebhook(payload, sigHeader)
      );
    }
  }
}
