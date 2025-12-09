package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stripe payment provider implementation with anti-corruption layer.
 *
 * <p>This final class implements the PaymentProviderAdapter interface for Stripe,
 * providing a clean abstraction over the Stripe Java SDK. It translates between
 * domain models and Stripe-specific API models, isolating the domain from Stripe
 * API changes and implementation details.
 *
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Anti-Corruption Layer:</strong> Protects domain model from Stripe API changes</li>
 *   <li><strong>Translation Layer:</strong> Converts between domain and Stripe models</li>
 *   <li><strong>Error Handling:</strong> Translates Stripe exceptions to domain exceptions</li>
 *   <li><strong>Idempotency:</strong> Leverages Stripe's idempotency key support</li>
 *   <li><strong>Pattern Matching:</strong> Uses Java 21 pattern matching for type-safe error handling</li>
 * </ul>
 *
 * <h2>Stripe API Integration</h2>
 * <p>This adapter integrates with the following Stripe APIs:
 * <ul>
 *   <li>Customer API - for customer management</li>
 *   <li>PaymentMethod API - for payment method tokenization and storage</li>
 *   <li>PaymentIntent API - for payment processing with 3D Secure support</li>
 *   <li>Refund API - for refund processing</li>
 *   <li>Webhook API - for webhook signature verification</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>API key stored in configuration (never hardcoded)</li>
 *   <li>Webhook signature verification prevents malicious webhooks</li>
 *   <li>Payment method tokens used instead of raw card data (PCI compliance)</li>
 *   <li>All monetary amounts converted to cents for Stripe API</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create customer
 * String customerId = stripeProvider.createCustomer(
 *     "tenant-123",
 *     "customer@example.com",
 *     "John Doe"
 * );
 *
 * // Add payment method
 * PaymentMethodDetails paymentMethod = stripeProvider.createPaymentMethod(
 *     customerId,
 *     "pm_card_visa" // Token from Stripe.js
 * );
 *
 * // Process payment
 * PaymentResult result = stripeProvider.processPayment(
 *     paymentMethod.providerPaymentMethodId(),
 *     new BigDecimal("99.99"),
 *     "USD",
 *     "idempotency-key-123"
 * );
 *
 * if (result.success()) {
 *     log.info("Payment successful: {}", result.providerPaymentId());
 * }
 * }</pre>
 *
 * @see PaymentProviderAdapter
 * @see PaymentResult
 * @see PaymentMethodDetails
 */
@Component
@Slf4j
public final class StripePaymentProvider implements PaymentProviderAdapter {

  private final String apiKey;
  private final String webhookSecret;

  /**
   * Constructs a new StripePaymentProvider with configuration.
   *
   * @param properties the billing configuration properties
   */
  public StripePaymentProvider(BillingProperties properties) {
    this.apiKey = properties.payment().stripe().apiKey();
    this.webhookSecret = properties.payment().stripe().webhookSecret();

    // Initialize Stripe API key
    if (apiKey != null && !apiKey.isBlank()) {
      Stripe.apiKey = apiKey;
      log.info("Stripe payment provider initialized");
    } else {
      log.warn("Stripe API key not configured - Stripe provider will not be functional");
    }
  }

  @Override
  public String createCustomer(String tenantId, String email, String name) {
    log.debug("Creating Stripe customer for tenant: {}, email: {}", tenantId, email);

    try {
      var paramsBuilder = CustomerCreateParams.builder()
          .setEmail(email)
          .putMetadata("tenant_id", tenantId);

      if (name != null && !name.isBlank()) {
        paramsBuilder.setName(name);
      }

      var params = paramsBuilder.build();
      var customer = Customer.create(params);

      log.info("Created Stripe customer: {} for tenant: {}", customer.getId(), tenantId);
      return customer.getId();

    } catch (final StripeException e) {
      log.error("Failed to create Stripe customer for tenant: {}", tenantId, e);
      throw new PaymentException("Failed to create customer in Stripe: " + e.getMessage(), e);
    }
  }

  @Override
  public void updateCustomer(String customerId, CustomerUpdateRequest request) {
    log.debug("Updating Stripe customer: {}", customerId);

    try {
      var paramsBuilder = CustomerUpdateParams.builder();

      if (request.hasEmail()) {
        paramsBuilder.setEmail(request.email());
      }

      if (request.hasName()) {
        paramsBuilder.setName(request.name());
      }

      if (request.hasPhone()) {
        paramsBuilder.setPhone(request.phone());
      }

      if (request.hasAddress()) {
        // Stripe expects structured address, but we'll store in description for simplicity
        paramsBuilder.setDescription("Address: " + request.address());
      }

      var params = paramsBuilder.build();
      var customer = Customer.retrieve(customerId);
      customer.update(params);

      log.info("Updated Stripe customer: {}", customerId);

    } catch (final StripeException e) {
      log.error("Failed to update Stripe customer: {}", customerId, e);
      throw new PaymentException("Failed to update customer in Stripe: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentMethodDetails createPaymentMethod(String customerId, String token) {
    log.debug("Creating payment method for Stripe customer: {}", customerId);

    try {
      // Retrieve the payment method from the token
      var paymentMethod = PaymentMethod.retrieve(token);

      // Attach payment method to customer
      var attachParams = PaymentMethodAttachParams.builder()
          .setCustomer(customerId)
          .build();

      paymentMethod.attach(attachParams);

      log.info("Created and attached payment method: {} to customer: {}",
          paymentMethod.getId(), customerId);

      return translatePaymentMethod(paymentMethod);

    } catch (final StripeException e) {
      log.error("Failed to create payment method for customer: {}", customerId, e);
      throw new PaymentException("Failed to create payment method in Stripe: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentMethodDetails getPaymentMethod(String paymentMethodId) {
    log.debug("Retrieving Stripe payment method: {}", paymentMethodId);

    try {
      var paymentMethod = PaymentMethod.retrieve(paymentMethodId);
      return translatePaymentMethod(paymentMethod);

    } catch (final StripeException e) {
      log.error("Failed to retrieve payment method: {}", paymentMethodId, e);

      // Use pattern matching for instanceof (Java 21 feature)
      if (e instanceof com.stripe.exception.InvalidRequestException invalidRequest) {
        throw new PaymentException.PaymentMethodNotFoundException(paymentMethodId, e);
      }

      throw new PaymentException("Failed to retrieve payment method from Stripe: " + e.getMessage(), e);
    }
  }

  @Override
  public void deletePaymentMethod(String paymentMethodId) {
    log.debug("Deleting Stripe payment method: {}", paymentMethodId);

    try {
      var paymentMethod = PaymentMethod.retrieve(paymentMethodId);
      paymentMethod.detach();

      log.info("Deleted Stripe payment method: {}", paymentMethodId);

    } catch (final StripeException e) {
      log.error("Failed to delete payment method: {}", paymentMethodId, e);
      throw new PaymentException("Failed to delete payment method from Stripe: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult processPayment(
      String paymentMethodId,
      BigDecimal amount,
      String currency,
      String idempotencyKey
  ) {
    log.debug("Processing Stripe payment: amount={}, currency={}, paymentMethod={}",
        amount, currency, paymentMethodId);

    try {
      // Convert amount to cents (Stripe uses smallest currency unit)
      long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

      // Create payment intent with idempotency key
      var params = PaymentIntentCreateParams.builder()
          .setAmount(amountInCents)
          .setCurrency(currency.toLowerCase())
          .setPaymentMethod(paymentMethodId)
          .setConfirm(true)
          .setAutomaticPaymentMethods(
              PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                  .setEnabled(true)
                  .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                  .build()
          )
          .build();

      var requestOptions = com.stripe.net.RequestOptions.builder()
          .setIdempotencyKey(idempotencyKey)
          .build();

      var paymentIntent = PaymentIntent.create(params, requestOptions);

      // Check payment status using pattern matching
      return switch (paymentIntent.getStatus()) {
        case "succeeded" -> {
          log.info("Stripe payment succeeded: {}", paymentIntent.getId());
          yield PaymentResult.success(
              paymentIntent.getId(),
              amount,
              currency,
              convertTimestamp(paymentIntent.getCreated())
          );
        }
        case "requires_action", "requires_payment_method" -> {
          log.warn("Stripe payment requires action: {}", paymentIntent.getId());
          yield PaymentResult.failure(
              amount,
              currency,
              "Payment requires additional authentication"
          );
        }
        case "canceled" -> {
          log.warn("Stripe payment canceled: {}", paymentIntent.getId());
          yield PaymentResult.failure(
              amount,
              currency,
              "Payment was canceled"
          );
        }
        default -> {
          log.error("Unexpected Stripe payment status: {}", paymentIntent.getStatus());
          yield PaymentResult.failure(
              amount,
              currency,
              "Payment failed with status: " + paymentIntent.getStatus()
          );
        }
      };

    } catch (final StripeException e) {
      log.error("Stripe payment processing failed", e);

      // Extract failure reason from Stripe exception
      String failureReason = extractFailureReason(e);

      return PaymentResult.failure(amount, currency, failureReason);
    }
  }

  @Override
  public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
    log.debug("Processing Stripe refund: paymentId={}, amount={}", paymentId, amount);

    try {
      // Convert amount to cents
      long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

      var params = RefundCreateParams.builder()
          .setPaymentIntent(paymentId)
          .setAmount(amountInCents)
          .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
          .putMetadata("reason", reason)
          .build();

      var refund = Refund.create(params);

      // Check refund status
      return switch (refund.getStatus()) {
        case "succeeded" -> {
          log.info("Stripe refund succeeded: {}", refund.getId());
          yield PaymentResult.success(
              refund.getId(),
              amount,
              refund.getCurrency().toUpperCase(),
              convertTimestamp(refund.getCreated())
          );
        }
        case "pending" -> {
          log.info("Stripe refund pending: {}", refund.getId());
          yield PaymentResult.success(
              refund.getId(),
              amount,
              refund.getCurrency().toUpperCase(),
              convertTimestamp(refund.getCreated())
          );
        }
        case "failed" -> {
          log.error("Stripe refund failed: {}", refund.getId());
          yield PaymentResult.failure(
              amount,
              refund.getCurrency().toUpperCase(),
              "Refund failed: " + refund.getFailureReason()
          );
        }
        case "canceled" -> {
          log.warn("Stripe refund canceled: {}", refund.getId());
          yield PaymentResult.failure(
              amount,
              refund.getCurrency().toUpperCase(),
              "Refund was canceled"
          );
        }
        default -> {
          log.error("Unexpected Stripe refund status: {}", refund.getStatus());
          yield PaymentResult.failure(
              amount,
              refund.getCurrency().toUpperCase(),
              "Refund failed with status: " + refund.getStatus()
          );
        }
      };

    } catch (final StripeException e) {
      log.error("Stripe refund processing failed", e);

      String failureReason = extractFailureReason(e);

      // Try to extract currency from payment intent
      String currency = "USD"; // Default fallback
      try {
        var paymentIntent = PaymentIntent.retrieve(paymentId);
        currency = paymentIntent.getCurrency().toUpperCase();
      } catch (final StripeException ex) {
        log.warn("Could not retrieve currency from payment intent", ex);
      }

      return PaymentResult.failure(amount, currency, failureReason);
    }
  }

  @Override
  public boolean verifyWebhookSignature(String payload, String signature) {
    log.debug("Verifying Stripe webhook signature");

    if (webhookSecret == null || webhookSecret.isBlank()) {
      log.error("Stripe webhook secret not configured");
      return false;
    }

    try {
      // Verify webhook signature using Stripe SDK
      Webhook.constructEvent(payload, signature, webhookSecret);

      log.debug("Stripe webhook signature verified successfully");
      return true;

    } catch (final SignatureVerificationException e) {
      log.error("Stripe webhook signature verification failed", e);
      return false;
    }
  }

  @Override
  public String getProviderName() {
    return "stripe";
  }

  /**
   * Translates Stripe PaymentMethod to domain PaymentMethodDetails.
   *
   * <p>This method extracts relevant information from Stripe's PaymentMethod
   * object and converts it to our domain model, isolating the domain from
   * Stripe's API structure.
   *
   * @param paymentMethod the Stripe PaymentMethod object
   * @return domain PaymentMethodDetails
   */
  private PaymentMethodDetails translatePaymentMethod(PaymentMethod paymentMethod) {
    String type = paymentMethod.getType().toUpperCase();
    String last4 = null;
    String brand = null;
    Integer expiryMonth = null;
    Integer expiryYear = null;

    // Extract details based on payment method type using pattern matching
    if (paymentMethod.getCard() != null) {
      var card = paymentMethod.getCard();
      last4 = card.getLast4();
      brand = card.getBrand();
      expiryMonth = Math.toIntExact(card.getExpMonth());
      expiryYear = Math.toIntExact(card.getExpYear());
    } else if (paymentMethod.getUsBankAccount() != null) {
      var bankAccount = paymentMethod.getUsBankAccount();
      last4 = bankAccount.getLast4();
      brand = bankAccount.getBankName();
      type = "BANK_ACCOUNT";
    }

    return new PaymentMethodDetails(
        paymentMethod.getId(),
        type,
        last4,
        brand,
        expiryMonth,
        expiryYear
    );
  }

  /**
   * Extracts a user-friendly failure reason from Stripe exception.
   *
   * @param e the Stripe exception
   * @return a user-friendly failure reason
   */
  private String extractFailureReason(StripeException e) {
    // Use pattern matching for instanceof (Java 21 feature)
    if (e instanceof com.stripe.exception.CardException cardEx) {
      return "Card declined: " + cardEx.getDeclineCode();
    } else if (e instanceof com.stripe.exception.InvalidRequestException invalidEx) {
      return "Invalid request: " + invalidEx.getMessage();
    } else if (e instanceof com.stripe.exception.AuthenticationException) {
      return "Authentication failed: Invalid API key";
    } else if (e instanceof com.stripe.exception.ApiConnectionException) {
      return "Connection error: Unable to connect to Stripe";
    } else if (e instanceof com.stripe.exception.RateLimitException) {
      return "Rate limit exceeded: Too many requests";
    } else {
      return "Payment processing error: " + e.getMessage();
    }
  }

  /**
   * Converts Unix timestamp to LocalDateTime.
   *
   * @param timestamp Unix timestamp in seconds
   * @return LocalDateTime in system default timezone
   */
  private LocalDateTime convertTimestamp(Long timestamp) {
    if (timestamp == null) {
      return LocalDateTime.now();
    }

    return LocalDateTime.ofInstant(
        Instant.ofEpochSecond(timestamp),
        ZoneId.systemDefault()
    );
  }
}
