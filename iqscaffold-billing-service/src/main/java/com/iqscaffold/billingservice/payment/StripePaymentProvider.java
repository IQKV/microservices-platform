package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.net.RequestOptions;
import java.math.BigDecimal;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentProvider implements PaymentProviderAdapter {

  private final BillingProperties billingProperties;

  public StripePaymentProvider(BillingProperties billingProperties) {
    this.billingProperties = billingProperties;
  }

  @PostConstruct
  public void init() {
    Stripe.apiKey = billingProperties.payment().stripe().apiKey();
  }

  @Override
  public String createPaymentIntent(BigDecimal amount, String currency, BigDecimal applicationFeeAmount, Optional<String> connectedAccountId) {
    try {
      PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
          .setAmount(toMinorUnits(amount, currency))
          .setCurrency(currency)
          .setAutomaticPaymentMethods(
              PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
          );

      if (applicationFeeAmount != null && applicationFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
        paramsBuilder.setApplicationFeeAmount(toMinorUnits(applicationFeeAmount, currency));
      }

      RequestOptions options = connectedAccountId
          .map(id -> RequestOptions.builder().setStripeAccount(id).build())
          .orElse(null);

      PaymentIntent intent = PaymentIntent.create(paramsBuilder.build(), options);
      return intent.getId();

    } catch (StripeException e) {
      throw handleStripeException(e);
    }
  }

  @Override
  public void refundPayment(String paymentIntentId, Optional<BigDecimal> amount, Optional<String> connectedAccountId) {
    try {
      RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
          .setPaymentIntent(paymentIntentId);

      amount.ifPresent(a -> paramsBuilder.setAmount(toMinorUnits(a, "USD"))); // TODO: Currency needed for refund? usually inferred from PI

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

  private Long toMinorUnits(BigDecimal amount, String currency) {
    // Simplified logic: assume 2 decimals for now.
    // In production, use Currency.getInstance(currency).getDefaultFractionDigits()
    return amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  private RuntimeException handleStripeException(StripeException e) {
    return switch (e) {
      case com.stripe.exception.CardException ce -> new PaymentException("Card declined: " + ce.getMessage(), ce);
      case com.stripe.exception.InvalidRequestException ire -> new PaymentException("Invalid request: " + ire.getMessage(), ire);
      case com.stripe.exception.AuthenticationException ae -> new PaymentException("Authentication failed", ae);
      case com.stripe.exception.ApiConnectionException ace -> new PaymentException("Stripe connection failed", ace);
      case com.stripe.exception.StripeException se -> new PaymentException("Stripe error: " + se.getMessage(), se);
    };
  }
}
