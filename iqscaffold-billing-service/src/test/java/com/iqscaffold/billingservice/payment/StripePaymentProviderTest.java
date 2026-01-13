package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.dto.GatewayConfigDtos;
import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter.ProviderPaymentIntent;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripePaymentProviderTest {

  @Mock
  private IqScaffoldProperties iqScaffoldProperties;

  @Mock
  private StripeCustomerRepository stripeCustomerRepository;

  @Mock
  private com.iqscaffold.billingservice.admin.PaymentGatewayConfigService gatewayConfigService;

  private StripePaymentProvider stripePaymentProvider;

  @BeforeEach
  void setUp() {
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);

    when(iqScaffoldProperties.billing()).thenReturn(billing);
    when(billing.payment()).thenReturn(payment);
    when(payment.stripe()).thenReturn(stripe);
    when(stripe.apiKey()).thenReturn("sk_test_123");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    stripePaymentProvider = new StripePaymentProvider(iqScaffoldProperties, stripeCustomerRepository, gatewayConfigService);
    stripePaymentProvider.init();
  }

  @Test
  void init_shouldSetStripeApiKey() {
    // When
    stripePaymentProvider.init();

    // Then
    assertNotNull(Stripe.apiKey);
  }

  @Test
  void createPaymentIntent_shouldCreateIntentWithoutCustomer() throws StripeException {
    // Given
    BigDecimal amount = new BigDecimal("100.00");
    String currency = "usd";
    String description = "Test payment";
    Map<String, String> metadata = new HashMap<>();
    metadata.put("order_id", "123");

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_123");
    when(mockIntent.getClientSecret()).thenReturn("pi_123_secret");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, description, null, null, metadata, null, Optional.empty(), "idempotency_key_123"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_123", result.id());
      assertEquals("pi_123_secret", result.clientSecret());
    }
  }

  @Test
  void createPaymentIntent_shouldCreateIntentWithNewCustomer() throws StripeException {
    // Given
    BigDecimal amount = new BigDecimal("50.00");
    String currency = "usd";
    String customerEmail = "test@example.com";
    String customerName = "Test User";

    when(stripeCustomerRepository.findByEmailAndStripeAccountId(customerEmail, null))
        .thenReturn(Optional.empty());

    Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getId()).thenReturn("cus_123");

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_456");
    when(mockIntent.getClientSecret()).thenReturn("pi_456_secret");

    StripeCustomer savedCustomer = new StripeCustomer();
    savedCustomer.setStripeCustomerId("cus_123");
    when(stripeCustomerRepository.save(any(StripeCustomer.class))).thenReturn(savedCustomer);

    try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
         MockedStatic<PaymentIntent> intentStatic = mockStatic(PaymentIntent.class)) {

      customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class), any()))
          .thenReturn(mockCustomer);
      intentStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test", customerEmail, customerName, null, null, Optional.empty(), "key_456"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_456", result.id());
      verify(stripeCustomerRepository).save(any(StripeCustomer.class));
    }
  }

  @Test
  void createPaymentIntent_shouldUseExistingCustomer() throws StripeException {
    // Given
    String customerEmail = "existing@example.com";
    StripeCustomer existingCustomer = new StripeCustomer();
    existingCustomer.setStripeCustomerId("cus_existing");
    existingCustomer.setEmail(customerEmail);
    existingCustomer.setName("Existing User");

    when(stripeCustomerRepository.findByEmailAndStripeAccountId(customerEmail, null))
        .thenReturn(Optional.of(existingCustomer));

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_789");
    when(mockIntent.getClientSecret()).thenReturn("pi_789_secret");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          new BigDecimal("75.00"), "usd", "Test", customerEmail, "Existing User",
          null, null, Optional.empty(), "key_789"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_789", result.id());
    }
  }

  @Test
  void createPaymentIntent_shouldHandleCardException() throws StripeException {
    // Given
    CardException cardException = mock(CardException.class);
    when(cardException.getMessage()).thenReturn("Card declined");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenThrow(cardException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createPaymentIntent(
              new BigDecimal("100"), "usd", "Test", null, null, null, null, Optional.empty(), "key"
          )
      );
    }
  }

  @Test
  void createPaymentIntent_shouldHandleInvalidRequestException() throws StripeException {
    // Given
    InvalidRequestException invalidException = mock(InvalidRequestException.class);
    when(invalidException.getMessage()).thenReturn("Invalid request");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenThrow(invalidException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createPaymentIntent(
              new BigDecimal("100"), "usd", "Test", null, null, null, null, Optional.empty(), "key"
          )
      );
    }
  }

  @Test
  void createPaymentIntent_shouldHandleAuthenticationException() throws StripeException {
    // Given
    AuthenticationException authException = mock(AuthenticationException.class);

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenThrow(authException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createPaymentIntent(
              new BigDecimal("100"), "usd", "Test", null, null, null, null, Optional.empty(), "key"
          )
      );
    }
  }

  @Test
  void createPaymentIntent_shouldHandleApiConnectionException() throws StripeException {
    // Given
    ApiConnectionException connectionException = mock(ApiConnectionException.class);

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenThrow(connectionException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createPaymentIntent(
              new BigDecimal("100"), "usd", "Test", null, null, null, null, Optional.empty(), "key"
          )
      );
    }
  }

  @Test
  void refundPayment_shouldRefundFullAmount() throws StripeException {
    // Given
    String paymentIntentId = "pi_123";
    String currency = "usd";

    Refund mockRefund = mock(Refund.class);

    try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
      mockedStatic.when(() -> Refund.create(any(RefundCreateParams.class), any()))
          .thenReturn(mockRefund);

      // When
      stripePaymentProvider.refundPayment(paymentIntentId, Optional.empty(), currency, Optional.empty());

      // Then
      mockedStatic.verify(() -> Refund.create(any(RefundCreateParams.class), any()));
    }
  }

  @Test
  void refundPayment_shouldRefundPartialAmount() throws StripeException {
    // Given
    String paymentIntentId = "pi_123";
    BigDecimal refundAmount = new BigDecimal("25.00");
    String currency = "usd";

    Refund mockRefund = mock(Refund.class);

    try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
      mockedStatic.when(() -> Refund.create(any(RefundCreateParams.class), any()))
          .thenReturn(mockRefund);

      // When
      stripePaymentProvider.refundPayment(paymentIntentId, Optional.of(refundAmount), currency, Optional.empty());

      // Then
      mockedStatic.verify(() -> Refund.create(any(RefundCreateParams.class), any()));
    }
  }

  @Test
  void refundPayment_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);
    when(stripeException.getMessage()).thenReturn("Refund failed");

    try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
      mockedStatic.when(() -> Refund.create(any(RefundCreateParams.class), any()))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.refundPayment("pi_123", Optional.empty(), "usd", Optional.empty())
      );
    }
  }

  @Test
  void createConnectAccount_shouldCreateExpressAccount() throws StripeException {
    // Given
    Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn("acct_123");

    try (MockedStatic<Account> mockedStatic = mockStatic(Account.class)) {
      mockedStatic.when(() -> Account.create(any(AccountCreateParams.class)))
          .thenReturn(mockAccount);

      // When
      String accountId = stripePaymentProvider.createConnectAccount();

      // Then
      assertEquals("acct_123", accountId);
    }
  }

  @Test
  void createConnectAccount_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<Account> mockedStatic = mockStatic(Account.class)) {
      mockedStatic.when(() -> Account.create(any(AccountCreateParams.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createConnectAccount()
      );
    }
  }

  @Test
  void createAccountLink_shouldCreateOnboardingLink() throws StripeException {
    // Given
    String accountId = "acct_123";
    String refreshUrl = "https://example.com/refresh";
    String returnUrl = "https://example.com/return";

    AccountLink mockLink = mock(AccountLink.class);
    when(mockLink.getUrl()).thenReturn("https://connect.stripe.com/setup/123");

    try (MockedStatic<AccountLink> mockedStatic = mockStatic(AccountLink.class)) {
      mockedStatic.when(() -> AccountLink.create(any(AccountLinkCreateParams.class)))
          .thenReturn(mockLink);

      // When
      String linkUrl = stripePaymentProvider.createAccountLink(accountId, refreshUrl, returnUrl);

      // Then
      assertEquals("https://connect.stripe.com/setup/123", linkUrl);
    }
  }

  @Test
  void createAccountLink_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<AccountLink> mockedStatic = mockStatic(AccountLink.class)) {
      mockedStatic.when(() -> AccountLink.create(any(AccountLinkCreateParams.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createAccountLink("acct_123", "refresh", "return")
      );
    }
  }

  @Test
  void createPaymentIntent_shouldHandleConnectedAccount() throws StripeException {
    // Given
    BigDecimal amount = new BigDecimal("100.00");
    String currency = "usd";
    String connectedAccountId = "acct_connected";

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_connected");
    when(mockIntent.getClientSecret()).thenReturn("pi_connected_secret");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test", null, null, null, null, Optional.of(connectedAccountId), "key_connected"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_connected", result.id());
    }
  }

  @Test
  void createPaymentIntent_shouldHandleApplicationFee() throws StripeException {
    // Given
    BigDecimal amount = new BigDecimal("100.00");
    BigDecimal applicationFee = new BigDecimal("10.00");
    String currency = "usd";

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_with_fee");
    when(mockIntent.getClientSecret()).thenReturn("pi_with_fee_secret");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test", null, null, null, applicationFee, Optional.empty(), "key_fee"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_with_fee", result.id());
    }
  }

  @Test
  void createPaymentIntent_shouldHandleZeroApplicationFee() throws StripeException {
    // Given
    BigDecimal amount = new BigDecimal("100.00");
    BigDecimal applicationFee = BigDecimal.ZERO;
    String currency = "usd";

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_zero_fee");
    when(mockIntent.getClientSecret()).thenReturn("pi_zero_fee_secret");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test", null, null, null, applicationFee, Optional.empty(), "key_zero"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_zero_fee", result.id());
    }
  }

  @Test
  void createPaymentIntent_shouldUpdateExistingCustomerName() throws StripeException {
    // Given
    String customerEmail = "existing@example.com";
    String newName = "Updated Name";
    StripeCustomer existingCustomer = new StripeCustomer();
    existingCustomer.setStripeCustomerId("cus_existing");
    existingCustomer.setEmail(customerEmail);
    existingCustomer.setName("Old Name");

    when(stripeCustomerRepository.findByEmailAndStripeAccountId(customerEmail, null))
        .thenReturn(Optional.of(existingCustomer));

    Customer mockCustomer = mock(Customer.class);
    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_updated");
    when(mockIntent.getClientSecret()).thenReturn("pi_updated_secret");

    try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
         MockedStatic<PaymentIntent> intentStatic = mockStatic(PaymentIntent.class)) {

      customerStatic.when(() -> Customer.retrieve(eq("cus_existing"), any()))
          .thenReturn(mockCustomer);
      intentStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          new BigDecimal("50.00"), "usd", "Test", customerEmail, newName,
          null, null, Optional.empty(), "key_update"
      );

      // Then
      assertNotNull(result);
      verify(stripeCustomerRepository).save(existingCustomer);
    }
  }

  @Test
  void refundPayment_shouldHandleConnectedAccount() throws StripeException {
    // Given
    String paymentIntentId = "pi_123";
    String currency = "usd";
    String connectedAccountId = "acct_connected";

    Refund mockRefund = mock(Refund.class);

    try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
      mockedStatic.when(() -> Refund.create(any(RefundCreateParams.class), any()))
          .thenReturn(mockRefund);

      // When
      stripePaymentProvider.refundPayment(paymentIntentId, Optional.empty(), currency, Optional.of(connectedAccountId));

      // Then
      mockedStatic.verify(() -> Refund.create(any(RefundCreateParams.class), any()));
    }
  }

  @Test
  void createPaymentIntent_shouldHandleJPYCurrency() throws StripeException {
    // Given - JPY has 0 decimal places
    BigDecimal amount = new BigDecimal("1000");
    String currency = "jpy";

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_jpy");
    when(mockIntent.getClientSecret()).thenReturn("pi_jpy_secret");

    try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
      mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test JPY", null, null, null, null, Optional.empty(), "key_jpy"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_jpy", result.id());
    }
  }

  @Test
  void verifyAndParseWebhook_shouldParsePaymentIntentSucceeded() {
    // Given
    String payload = "{\"id\":\"evt_123\",\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "t=123,v1=sig";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_123");
    when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
    
    com.stripe.model.StripeObject mockObject = mock(com.stripe.model.PaymentIntent.class);
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    when(deserializer.getObject()).thenReturn(Optional.of(mockObject));
    
    com.stripe.model.PaymentIntent pi = mock(com.stripe.model.PaymentIntent.class);
    when(pi.getId()).thenReturn("pi_123");
    when(pi.getMetadata()).thenReturn(Map.of("tenant_id", "tenant-123"));
    when(deserializer.getObject()).thenReturn(Optional.of(pi));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals("evt_123", result.eventId());
      assertEquals(WebhookEvent.EventType.PAYMENT_SUCCEEDED, result.eventType());
      assertEquals(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE, result.provider());
    }
  }

  @Test
  void verifyAndParseWebhook_shouldHandleInvalidSignature() {
    // Given
    String payload = "invalid";
    String sigHeader = "invalid";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenThrow(new com.stripe.exception.SignatureVerificationException("Invalid", "sig"));

      // When & Then
      assertThrows(IllegalArgumentException.class, () ->
          stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader)
      );
    }
  }

  @Test
  void verifyAndParseWebhook_shouldParseChargeRefunded() {
    // Given
    String payload = "{\"id\":\"evt_456\",\"type\":\"charge.refunded\"}";
    String sigHeader = "t=456,v1=sig456";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_456");
    when(mockEvent.getType()).thenReturn("charge.refunded");
    
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    
    com.stripe.model.Charge charge = mock(com.stripe.model.Charge.class);
    when(charge.getPaymentIntent()).thenReturn("pi_456");
    when(charge.getRefunded()).thenReturn(true);
    when(charge.getMetadata()).thenReturn(Map.of());
    when(deserializer.getObject()).thenReturn(Optional.of(charge));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals(WebhookEvent.EventType.PAYMENT_REFUNDED, result.eventType());
      assertEquals("pi_456", result.resourceId());
    }
  }

  @Test
  void verifyAndParseWebhook_shouldParsePayoutPaid() {
    // Given
    String payload = "{\"id\":\"evt_789\",\"type\":\"payout.paid\"}";
    String sigHeader = "t=789,v1=sig789";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_789");
    when(mockEvent.getType()).thenReturn("payout.paid");
    
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    
    com.stripe.model.Payout payout = mock(com.stripe.model.Payout.class);
    when(payout.getId()).thenReturn("po_123");
    when(payout.getAmount()).thenReturn(10000L);
    when(payout.getCurrency()).thenReturn("usd");
    when(payout.getArrivalDate()).thenReturn(1234567890L);
    when(deserializer.getObject()).thenReturn(Optional.of(payout));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals(WebhookEvent.EventType.PAYOUT_PAID, result.eventType());
      assertEquals("po_123", result.resourceId());
    }
  }

  @Test
  void verifyAndParseWebhook_shouldParseAccountUpdated() {
    // Given
    String payload = "{\"id\":\"evt_account\",\"type\":\"account.updated\"}";
    String sigHeader = "t=999,v1=sig999";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_account");
    when(mockEvent.getType()).thenReturn("account.updated");
    
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    
    com.stripe.model.Account account = mock(com.stripe.model.Account.class);
    when(account.getId()).thenReturn("acct_123");
    when(account.getChargesEnabled()).thenReturn(true);
    when(account.getPayoutsEnabled()).thenReturn(true);
    when(deserializer.getObject()).thenReturn(Optional.of(account));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals(WebhookEvent.EventType.ACCOUNT_UPDATED, result.eventType());
      assertEquals("acct_123", result.resourceId());
    }
  }

  @Test
  void createPaymentIntent_shouldUseTenantSpecificApiKey() throws Exception {
    // Given
    String tenantId = "tenant-123";
    BigDecimal amount = new BigDecimal("100.00");
    String currency = "usd";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.apiKey()).thenReturn("sk_test_global");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(true);
    
    GatewayConfigDtos.StripeGatewayConfigData tenantConfig = 
        new GatewayConfigDtos.StripeGatewayConfigData("sk_test_tenant", "whsec_tenant", null, null);
    
    when(gatewayConfigService.getDecryptedGatewayConfig(
        eq(tenantId),
        eq(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE),
        eq(GatewayConfigDtos.StripeGatewayConfigData.class)
    )).thenReturn(tenantConfig);

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_tenant");
    when(mockIntent.getClientSecret()).thenReturn("pi_tenant_secret");

    try (MockedStatic<SecurityContextHelper> contextMock = mockStatic(SecurityContextHelper.class);
         MockedStatic<PaymentIntent> intentMock = mockStatic(PaymentIntent.class)) {
      
      contextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      intentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test", null, null, null, null, Optional.empty(), "key_tenant"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_tenant", result.id());
      verify(gatewayConfigService).getDecryptedGatewayConfig(
          eq(tenantId),
          eq(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE),
          eq(GatewayConfigDtos.StripeGatewayConfigData.class)
      );
    }
  }

  @Test
  void createPaymentIntent_shouldFallbackToGlobalKeyOnTenantConfigError() throws Exception {
    // Given
    String tenantId = "tenant-123";
    BigDecimal amount = new BigDecimal("100.00");
    String currency = "usd";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    when(iqScaffoldProperties.billing()).thenReturn(billing);
    when(billing.payment()).thenReturn(payment);
    when(payment.stripe()).thenReturn(stripe);
    when(stripe.apiKey()).thenReturn("sk_test_global");
    when(billing.security()).thenReturn(security);
    when(security.encryption()).thenReturn(encryption);
    when(encryption.useTenantSpecificConfig()).thenReturn(true);
    
    when(gatewayConfigService.getDecryptedGatewayConfig(
        eq(tenantId),
        eq(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE),
        eq(GatewayConfigDtos.StripeGatewayConfigData.class)
    )).thenThrow(new RuntimeException("Config error"));

    PaymentIntent mockIntent = mock(PaymentIntent.class);
    when(mockIntent.getId()).thenReturn("pi_fallback");
    when(mockIntent.getClientSecret()).thenReturn("pi_fallback_secret");

    try (MockedStatic<SecurityContextHelper> contextMock = mockStatic(SecurityContextHelper.class);
         MockedStatic<PaymentIntent> intentMock = mockStatic(PaymentIntent.class)) {
      
      contextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      intentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockIntent);

      // When
      ProviderPaymentIntent result = stripePaymentProvider.createPaymentIntent(
          amount, currency, "Test", null, null, null, null, Optional.empty(), "key_fallback"
      );

      // Then
      assertNotNull(result);
      assertEquals("pi_fallback", result.id());
    }
  }

  @Test
  void verifyAndParseWebhook_shouldUseTenantSpecificWebhookSecret() {
    // Given
    String tenantId = "tenant-123";
    String payload = "{\"id\":\"evt_tenant\",\"type\":\"payment_intent.succeeded\"}";
    String sigHeader = "t=123,v1=sig";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_global");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(true);
    
    GatewayConfigDtos.StripeGatewayConfigData tenantConfig = 
        new GatewayConfigDtos.StripeGatewayConfigData("sk_test_tenant", "whsec_tenant", null, null);
    
    when(gatewayConfigService.getDecryptedGatewayConfig(
        eq(tenantId),
        eq(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE),
        eq(GatewayConfigDtos.StripeGatewayConfigData.class)
    )).thenReturn(tenantConfig);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_tenant");
    when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
    
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    
    com.stripe.model.PaymentIntent pi = mock(com.stripe.model.PaymentIntent.class);
    when(pi.getId()).thenReturn("pi_tenant");
    when(pi.getMetadata()).thenReturn(Map.of());
    when(deserializer.getObject()).thenReturn(Optional.of(pi));

    try (MockedStatic<SecurityContextHelper> contextMock = mockStatic(SecurityContextHelper.class);
         MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      
      contextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_tenant"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals("evt_tenant", result.eventId());
    }
  }

  // ==================== Subscription Management Tests ====================

  @Test
  void createProduct_shouldCreateStripeProduct() throws StripeException {
    // Given
    String name = "Test Product";
    String description = "Test Description";
    Map<String, String> metadata = Map.of("key", "value");

    com.stripe.model.Product mockProduct = mock(com.stripe.model.Product.class);
    when(mockProduct.getId()).thenReturn("prod_123");

    try (MockedStatic<com.stripe.model.Product> productStatic = mockStatic(com.stripe.model.Product.class)) {
      productStatic.when(() -> com.stripe.model.Product.create(any(com.stripe.param.ProductCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockProduct);

      // When
      String productId = stripePaymentProvider.createProduct(name, description, metadata);

      // Then
      assertEquals("prod_123", productId);
    }
  }

  @Test
  void createProduct_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Product> productStatic = mockStatic(com.stripe.model.Product.class)) {
      productStatic.when(() -> com.stripe.model.Product.create(any(com.stripe.param.ProductCreateParams.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createProduct("Test", "Description", null)
      );
    }
  }

  @Test
  void createPrice_shouldCreateStripePrice() throws StripeException {
    // Given
    String productId = "prod_123";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "usd";
    String interval = "month";
    Integer intervalCount = 1;
    Map<String, String> metadata = Map.of("plan", "basic");

    com.stripe.model.Price mockPrice = mock(com.stripe.model.Price.class);
    when(mockPrice.getId()).thenReturn("price_123");

    try (MockedStatic<com.stripe.model.Price> priceStatic = mockStatic(com.stripe.model.Price.class)) {
      priceStatic.when(() -> com.stripe.model.Price.create(any(com.stripe.param.PriceCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockPrice);

      // When
      String priceId = stripePaymentProvider.createPrice(productId, amount, currency, interval, intervalCount, metadata);

      // Then
      assertEquals("price_123", priceId);
    }
  }

  @Test
  void createPrice_shouldHandleInvalidInterval() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        stripePaymentProvider.createPrice("prod_123", new BigDecimal("10"), "usd", "invalid", 1, null)
    );
  }

  @Test
  void createPrice_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Price> priceStatic = mockStatic(com.stripe.model.Price.class)) {
      priceStatic.when(() -> com.stripe.model.Price.create(any(com.stripe.param.PriceCreateParams.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createPrice("prod_123", new BigDecimal("10"), "usd", "month", 1, null)
      );
    }
  }

  @Test
  void createSubscription_shouldCreateStripeSubscription() throws StripeException {
    // Given
    String customerId = "cus_123";
    String priceId = "price_123";
    Integer trialPeriodDays = 7;
    Map<String, String> metadata = Map.of("plan", "premium");
    String idempotencyKey = "sub_key_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);
    when(mockSubscription.getId()).thenReturn("sub_123");

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.create(any(com.stripe.param.SubscriptionCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      String subscriptionId = stripePaymentProvider.createSubscription(customerId, priceId, trialPeriodDays, metadata, idempotencyKey);

      // Then
      assertEquals("sub_123", subscriptionId);
    }
  }

  @Test
  void createSubscription_shouldHandleNullTrialPeriod() throws StripeException {
    // Given
    String customerId = "cus_123";
    String priceId = "price_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);
    when(mockSubscription.getId()).thenReturn("sub_no_trial");

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.create(any(com.stripe.param.SubscriptionCreateParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      String subscriptionId = stripePaymentProvider.createSubscription(customerId, priceId, null, null, null);

      // Then
      assertEquals("sub_no_trial", subscriptionId);
    }
  }

  @Test
  void createSubscription_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.create(any(com.stripe.param.SubscriptionCreateParams.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.createSubscription("cus_123", "price_123", null, null, null)
      );
    }
  }

  @Test
  void updateSubscription_shouldUpdateStripeSubscription() throws StripeException {
    // Given
    String subscriptionId = "sub_123";
    String newPriceId = "price_new";
    Map<String, String> metadata = Map.of("updated", "true");

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);
    when(mockSubscription.getId()).thenReturn(subscriptionId);

    com.stripe.model.SubscriptionItemCollection itemCollection = mock(com.stripe.model.SubscriptionItemCollection.class);
    com.stripe.model.SubscriptionItem item = mock(com.stripe.model.SubscriptionItem.class);
    when(item.getId()).thenReturn("si_123");
    when(itemCollection.getData()).thenReturn(java.util.List.of(item));
    when(mockSubscription.getItems()).thenReturn(itemCollection);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);
      when(mockSubscription.update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      String result = stripePaymentProvider.updateSubscription(subscriptionId, newPriceId, metadata);

      // Then
      assertEquals(subscriptionId, result);
    }
  }

  @Test
  void updateSubscription_shouldHandleNoItems() throws StripeException {
    // Given
    String subscriptionId = "sub_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);
    com.stripe.model.SubscriptionItemCollection itemCollection = mock(com.stripe.model.SubscriptionItemCollection.class);
    when(itemCollection.getData()).thenReturn(java.util.List.of());
    when(mockSubscription.getItems()).thenReturn(itemCollection);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.updateSubscription(subscriptionId, "price_new", null)
      );
    }
  }

  @Test
  void updateSubscription_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(any(String.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.updateSubscription("sub_123", "price_new", null)
      );
    }
  }

  @Test
  void cancelSubscription_shouldCancelAtPeriodEnd() throws StripeException {
    // Given
    String subscriptionId = "sub_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);
      when(mockSubscription.update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      stripePaymentProvider.cancelSubscription(subscriptionId, true);

      // Then
      verify(mockSubscription).update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class));
    }
  }

  @Test
  void cancelSubscription_shouldCancelImmediately() throws StripeException {
    // Given
    String subscriptionId = "sub_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);
      when(mockSubscription.cancel(any(com.stripe.param.SubscriptionCancelParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      stripePaymentProvider.cancelSubscription(subscriptionId, false);

      // Then
      verify(mockSubscription).cancel(any(com.stripe.param.SubscriptionCancelParams.class), any(RequestOptions.class));
    }
  }

  @Test
  void cancelSubscription_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(any(String.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.cancelSubscription("sub_123", false)
      );
    }
  }

  @Test
  void pauseSubscription_shouldPauseStripeSubscription() throws StripeException {
    // Given
    String subscriptionId = "sub_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);
      when(mockSubscription.update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      stripePaymentProvider.pauseSubscription(subscriptionId);

      // Then
      verify(mockSubscription).update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class));
    }
  }

  @Test
  void pauseSubscription_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(any(String.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.pauseSubscription("sub_123")
      );
    }
  }

  @Test
  void resumeSubscription_shouldResumeStripeSubscription() throws StripeException {
    // Given
    String subscriptionId = "sub_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);
      when(mockSubscription.update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      stripePaymentProvider.resumeSubscription(subscriptionId);

      // Then
      verify(mockSubscription).update(any(com.stripe.param.SubscriptionUpdateParams.class), any(RequestOptions.class));
    }
  }

  @Test
  void resumeSubscription_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(any(String.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.resumeSubscription("sub_123")
      );
    }
  }

  @Test
  void getSubscription_shouldRetrieveStripeSubscription() throws StripeException {
    // Given
    String subscriptionId = "sub_123";

    com.stripe.model.Subscription mockSubscription = mock(com.stripe.model.Subscription.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(eq(subscriptionId), any(RequestOptions.class)))
          .thenReturn(mockSubscription);

      // When
      Object result = stripePaymentProvider.getSubscription(subscriptionId);

      // Then
      assertEquals(mockSubscription, result);
    }
  }

  @Test
  void getSubscription_shouldHandleStripeException() throws StripeException {
    // Given
    StripeException stripeException = mock(StripeException.class);

    try (MockedStatic<com.stripe.model.Subscription> subscriptionStatic = mockStatic(com.stripe.model.Subscription.class)) {
      subscriptionStatic.when(() -> com.stripe.model.Subscription.retrieve(any(String.class), any(RequestOptions.class)))
          .thenThrow(stripeException);

      // When & Then
      assertThrows(PaymentException.class, () ->
          stripePaymentProvider.getSubscription("sub_123")
      );
    }
  }

  @Test
  void getProviderType_shouldReturnStripe() {
    // When
    com.iqscaffold.billingservice.shared.PaymentGatewayProvider result = stripePaymentProvider.getProviderType();

    // Then
    assertEquals(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE, result);
  }

  @Test
  void verifyAndParseWebhook_shouldParseSubscriptionCreated() {
    // Given
    String payload = "{\"id\":\"evt_sub_created\",\"type\":\"customer.subscription.created\"}";
    String sigHeader = "t=123,v1=sig";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_sub_created");
    when(mockEvent.getType()).thenReturn("customer.subscription.created");
    
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    
    com.stripe.model.Subscription subscription = mock(com.stripe.model.Subscription.class);
    when(subscription.getId()).thenReturn("sub_123");
    when(subscription.getStatus()).thenReturn("active");
    when(subscription.getCustomer()).thenReturn("cus_123");
    when(subscription.getMetadata()).thenReturn(Map.of("tenant_id", "tenant-123"));
    when(deserializer.getObject()).thenReturn(Optional.of(subscription));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals(WebhookEvent.EventType.SUBSCRIPTION_CREATED, result.eventType());
      assertEquals("sub_123", result.resourceId());
    }
  }

  @Test
  void verifyAndParseWebhook_shouldParseInvoicePaid() {
    // Given
    String payload = "{\"id\":\"evt_invoice_paid\",\"type\":\"invoice.paid\"}";
    String sigHeader = "t=123,v1=sig";
    
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);
    
    lenient().when(iqScaffoldProperties.billing()).thenReturn(billing);
    lenient().when(billing.payment()).thenReturn(payment);
    lenient().when(payment.stripe()).thenReturn(stripe);
    lenient().when(stripe.webhookSecret()).thenReturn("whsec_test");
    lenient().when(billing.security()).thenReturn(security);
    lenient().when(security.encryption()).thenReturn(encryption);
    lenient().when(encryption.useTenantSpecificConfig()).thenReturn(false);

    com.stripe.model.Event mockEvent = mock(com.stripe.model.Event.class);
    when(mockEvent.getId()).thenReturn("evt_invoice_paid");
    when(mockEvent.getType()).thenReturn("invoice.paid");
    
    com.stripe.model.EventDataObjectDeserializer deserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
    when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
    
    com.stripe.model.Invoice invoice = mock(com.stripe.model.Invoice.class);
    when(invoice.getId()).thenReturn("in_123");
    when(invoice.getStatus()).thenReturn("paid");
    when(invoice.getCustomer()).thenReturn("cus_123");
    when(invoice.getCurrency()).thenReturn("usd");
    when(deserializer.getObject()).thenReturn(Optional.of(invoice));

    try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
      webhookStatic.when(() -> Webhook.constructEvent(payload, sigHeader, "whsec_test"))
          .thenReturn(mockEvent);

      // When
      WebhookEvent result = stripePaymentProvider.verifyAndParseWebhook(payload, sigHeader);

      // Then
      assertNotNull(result);
      assertEquals(WebhookEvent.EventType.INVOICE_PAID, result.eventType());
      assertEquals("in_123", result.resourceId());
    }
  }
}
