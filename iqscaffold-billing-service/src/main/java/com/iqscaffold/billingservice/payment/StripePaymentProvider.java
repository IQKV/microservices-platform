package com.iqscaffold.billingservice.payment;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Optional;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.stereotype.Service;

/**
 * Stripe implementation of the {@link PaymentProviderAdapter}.
 * <p>
 * This class handles low-level interactions with the Stripe API, including:
 * <ul>
 * <li>Creating Payment Intents with support for Automatic Payment Methods.</li>
 * <li>Managing Stripe Customers (upserting based on email to avoid
 * duplicates).</li>
 * <li>Handling Stripe Connect flows (creating accounts, account links).</li>
 * <li>Processing Refunds.</li>
 * </ul>
 * <p>
 * It uses {@link StripeCustomerRepository} to maintain a mapping between local
 * payments and Stripe Customer IDs.
 */
@Service
public class StripePaymentProvider implements PaymentProviderAdapter {

  private final IqScaffoldProperties iqScaffoldProperties;
  private final StripeCustomerRepository stripeCustomerRepository;
  private final com.iqscaffold.billingservice.security.SecurityContextHelper securityHelper; // To get tenant if needed,
                                                                                             // though we can pass it
                                                                                             // down.

  public StripePaymentProvider(IqScaffoldProperties iqScaffoldProperties, StripeCustomerRepository stripeCustomerRepository) {
    this.iqScaffoldProperties = iqScaffoldProperties;
    this.stripeCustomerRepository = stripeCustomerRepository;
    this.securityHelper = null; // We'll just rely on passed args or context if we inject it properly.
    // Actually, let's keep it simple and just use the repo.
  }

  @PostConstruct
  public void init() {
    Stripe.apiKey = iqScaffoldProperties.billing().payment().stripe().apiKey();
  }

  /**
   * Creates a Stripe PaymentIntent.
   * <p>
   * If {@code customerEmail} is provided, this method ensures a corresponding
   * Stripe Customer exists
   * (creating one or updating the existing one) and attaches it to the Intent.
   * This enables
   * "Save my card" functionality and better tracking in the Stripe Dashboard.
   *
   * @param amount               The amount in major units (e.g., Dollars).
   *                             Converted to cents internally.
   * @param currency             The 3-letter currency code (e.g., "usd").
   * @param description          Description to appear on the statement/dashboard.
   * @param customerEmail        Email of the payer (optional).
   * @param customerName         Name of the payer (optional).
   * @param metadata             Additional key-value pairs to attach to the
   *                             Stripe object.
   * @param applicationFeeAmount Fee to capture for the platform (if using
   *                             Connect).
   * @param connectedAccountId   The target merchant account ID (if using
   *                             Connect).
   * @return The ID of the created PaymentIntent (e.g., "pi_123...").
   */
  @Override
  public ProviderPaymentIntent createPaymentIntent(
      BigDecimal amount,
      String currency,
      String description,
      String customerEmail,
      String customerName,
      java.util.Map<String, String> metadata,
      BigDecimal applicationFeeAmount,
      Optional<String> connectedAccountId,
      String idempotencyKey) {
    try {
      // 1. Upsert Customer if email provided
      String customerId = null;
      if (customerEmail != null) {
        customerId = upsertCustomer(customerEmail, customerName, connectedAccountId);
      }

      PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
          .setAmount(toMinorUnits(amount, currency))
          .setCurrency(currency)
          .setDescription(description)
          .setAutomaticPaymentMethods(
              PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build());

      if (customerId != null) {
        paramsBuilder.setCustomer(customerId);
      }

      if (metadata != null) {
        paramsBuilder.putAllMetadata(metadata);
      }

      // Add Tenant ID to metadata for webhook resolution
      String tenantId = com.iqscaffold.billingservice.security.SecurityContextHelper.getCurrentTenantId();
      if (tenantId != null) {
        paramsBuilder.putMetadata("tenant_id", tenantId);
      }

      if (applicationFeeAmount != null && applicationFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
        paramsBuilder.setApplicationFeeAmount(toMinorUnits(applicationFeeAmount, currency));
      }

      RequestOptions options = RequestOptions.builder()
          .setIdempotencyKey(idempotencyKey)
          .build();

      if (connectedAccountId.isPresent()) {
        paramsBuilder.setTransferData(
            PaymentIntentCreateParams.TransferData.builder()
                .setDestination(connectedAccountId.get())
                .build());
      }

      if (connectedAccountId.isPresent()) {
        options = options.toBuilder().setStripeAccount(connectedAccountId.get()).build();
      }

      PaymentIntent intent = PaymentIntent.create(paramsBuilder.build(), options);
      return new ProviderPaymentIntent(intent.getId(), intent.getClientSecret());
    } catch (StripeException e) {
      throw handleStripeException(e);
    }
  }

  /**
   * Ensures a Stripe Customer exists for the given email and account.
   * <p>
   * <ul>
   * <li>Checks the local {@link StripeCustomerRepository} first.</li>
   * <li>If found, updates the name in Stripe if it changed.</li>
   * <li>If not found, creates a new Customer in Stripe and persists the mapping
   * locally.</li>
   * </ul>
   * This logic mimics the `Hi.Events` reference implementation for customer
   * consistency.
   */
  private String upsertCustomer(String email, String name, Optional<String> connectedAccountId) throws StripeException {
    String accountId = connectedAccountId.orElse(null); // Null for platform
    // Note: Tenant ID is needed for local persistence.
    // In a real app we'd pass it or fetch from context.
    // Assuming context is available via SecurityContextHelper static call.
    // Note: Tenant ID is implicitly handled by the schema context

    var existing = stripeCustomerRepository.findByEmailAndStripeAccountId(email, accountId);

    RequestOptions options = accountId != null
        ? RequestOptions.builder().setStripeAccount(accountId).build()
        : null;

    if (existing.isPresent()) {
      StripeCustomer localParams = existing.get();
      // Update name if changed
      if (name != null && !name.equals(localParams.getName())) {
        com.stripe.model.Customer customer = com.stripe.model.Customer.retrieve(localParams.getStripeCustomerId(),
            options);
        customer.update(
            com.stripe.param.CustomerUpdateParams.builder().setName(name).build(),
            options);
        localParams.setName(name);
        stripeCustomerRepository.save(localParams);
      }
      return localParams.getStripeCustomerId();
    } else {
      // Create new
      var params = com.stripe.param.CustomerCreateParams.builder()
          .setEmail(email)
          .setName(name)
          .build();

      var stripeCustomer = com.stripe.model.Customer.create(params, options);

      StripeCustomer newRecord = new StripeCustomer();
      newRecord.setEmail(email);
      newRecord.setName(name);
      newRecord.setStripeCustomerId(stripeCustomer.getId());
      newRecord.setStripeAccountId(accountId);
      stripeCustomerRepository.save(newRecord);

      return stripeCustomer.getId();
    }
  }

  @Override
  public void refundPayment(String paymentIntentId, Optional<BigDecimal> amount, String currency,
      Optional<String> connectedAccountId) {
    try {
      RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
          .setPaymentIntent(paymentIntentId);

      amount.ifPresent(a -> paramsBuilder.setAmount(toMinorUnits(a, currency)));

      RequestOptions options = connectedAccountId
          .map(id -> RequestOptions.builder().setStripeAccount(id).build())
          .orElse(null);

      Refund.create(paramsBuilder.build(), options);

    } catch (StripeException e) {
      throw handleStripeException(e);
    }
  }

  @Override
  public String createConnectAccount() {
    try {
      AccountCreateParams params = AccountCreateParams.builder()
          .setType(AccountCreateParams.Type.EXPRESS)
          .build();

      Account account = Account.create(params);
      return account.getId();
    } catch (StripeException e) {
      throw handleStripeException(e);
    }
  }

  @Override
  public String createAccountLink(String accountId, String refreshUrl, String returnUrl) {
    try {
      AccountLinkCreateParams params = AccountLinkCreateParams.builder()
          .setAccount(accountId)
          .setRefreshUrl(refreshUrl)
          .setReturnUrl(returnUrl)
          .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
          .build();

      AccountLink link = AccountLink.create(params);
      return link.getUrl();
    } catch (StripeException e) {
      throw handleStripeException(e);
    }
  }

  /**
   * Converts a decimal amount (e.g., 10.50) to the minor unit integer required by
   * Stripe (e.g., 1050 cents).
   * <p>
   * Note: Currently assumes 2 decimal places for all currencies.
   * TODO: Enhance to use {@link java.util.Currency} for currency-specific scaling
   * (e.g., JPY has 0 decimals).
   */
  private Long toMinorUnits(BigDecimal amount, String currencyCode) {
    java.util.Currency currency = java.util.Currency.getInstance(currencyCode.toUpperCase());
    int fractionDigits = currency.getDefaultFractionDigits();
    return amount.movePointRight(fractionDigits).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
  }

  private RuntimeException handleStripeException(StripeException e) {
    return switch (e) {
      case com.stripe.exception.CardException ce -> new PaymentException("Card declined: " + ce.getMessage(), ce);
      case com.stripe.exception.InvalidRequestException ire ->
        new PaymentException("Invalid request: " + ire.getMessage(), ire);
      case com.stripe.exception.AuthenticationException ae -> new PaymentException("Authentication failed", ae);
      case com.stripe.exception.ApiConnectionException ace -> new PaymentException("Stripe connection failed", ace);
      case com.stripe.exception.StripeException se -> new PaymentException("Stripe error: " + se.getMessage(), se);
    };
  }
}
