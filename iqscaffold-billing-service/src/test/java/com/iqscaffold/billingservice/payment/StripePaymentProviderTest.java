package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter.ProviderPaymentIntent;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
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

  private StripePaymentProvider stripePaymentProvider;

  @BeforeEach
  void setUp() {
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Payment payment = mock(IqScaffoldProperties.Billing.Payment.class);
    IqScaffoldProperties.Billing.Payment.Stripe stripe = mock(IqScaffoldProperties.Billing.Payment.Stripe.class);

    when(iqScaffoldProperties.billing()).thenReturn(billing);
    when(billing.payment()).thenReturn(payment);
    when(payment.stripe()).thenReturn(stripe);
    when(stripe.apiKey()).thenReturn("sk_test_123");

    stripePaymentProvider = new StripePaymentProvider(iqScaffoldProperties, stripeCustomerRepository);
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
}
