package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.config.BillingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting and providing payment provider implementations.
 *
 * <p>This factory uses the configured payment provider from application properties
 * to return the appropriate PaymentProviderAdapter implementation. It supports
 * multiple payment providers (Stripe, PayPal, Manual) and provides fallback logic
 * when the configured provider is unavailable.
 *
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Provider Selection:</strong> Centralizes provider selection logic based on configuration</li>
 *   <li><strong>Switch Expression:</strong> Uses Java 21 switch expressions for clean provider selection</li>
 *   <li><strong>Fallback Logic:</strong> Automatically falls back to manual processing when provider unavailable</li>
 *   <li><strong>Configuration-Driven:</strong> Provider selection controlled via BillingProperties</li>
 *   <li><strong>Dependency Injection:</strong> All providers injected via constructor for testability</li>
 * </ul>
 *
 * <h2>Provider Selection Logic</h2>
 * <p>The factory selects providers in the following order:
 * <ol>
 *   <li>Use configured provider (from iqscaffold.billing.payment.provider)</li>
 *   <li>If configured provider is unavailable, fall back to manual provider</li>
 *   <li>Log warnings when fallback occurs</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * <p>Provider selection is configured in application.yml:
 * <pre>{@code
 * iqscaffold:
 *   billing:
 *     payment:
 *       provider: stripe  # Options: stripe, paypal, manual
 *       stripe:
 *         api-key: ${STRIPE_API_KEY}
 *         webhook-secret: ${STRIPE_WEBHOOK_SECRET}
 *       paypal:
 *         client-id: ${PAYPAL_CLIENT_ID}
 *         client-secret: ${PAYPAL_CLIENT_SECRET}
 * }</pre>
 *
 * <h2>Fallback Behavior</h2>
 * <p>When a provider is configured but unavailable (e.g., missing credentials),
 * the factory automatically falls back to the manual provider to prevent service
 * disruption. This allows the system to continue operating with manual payment
 * recording while provider issues are resolved.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Service
 * public class PaymentService {
 *     private final PaymentProviderFactory providerFactory;
 *
 *     public PaymentResult processPayment(PaymentRequest request) {
 *         // Get the configured provider
 *         PaymentProviderAdapter provider = providerFactory.getProvider();
 *
 *         // Process payment using the selected provider
 *         return provider.processPayment(
 *             request.paymentMethodId(),
 *             request.amount(),
 *             request.currency(),
 *             request.idempotencyKey()
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This factory is thread-safe. Provider instances are injected once during
 * construction and never modified. The getProvider() method can be called
 * concurrently from multiple threads without synchronization.
 *
 * @see PaymentProviderAdapter
 * @see StripePaymentProvider
 * @see PayPalPaymentProvider
 * @see ManualPaymentProvider
 * @see BillingProperties
 */
@Component
@Slf4j
public class PaymentProviderFactory {

  private final BillingProperties properties;
  private final StripePaymentProvider stripeProvider;
  private final PayPalPaymentProvider paypalProvider;
  private final ManualPaymentProvider manualProvider;

  /**
   * Constructs a new PaymentProviderFactory with all available providers.
   *
   * @param properties     the billing configuration properties
   * @param stripeProvider the Stripe payment provider implementation
   * @param paypalProvider the PayPal payment provider implementation
   * @param manualProvider the manual payment provider implementation
   */
  public PaymentProviderFactory(
      BillingProperties properties,
      StripePaymentProvider stripeProvider,
      PayPalPaymentProvider paypalProvider,
      ManualPaymentProvider manualProvider
  ) {
    this.properties = properties;
    this.stripeProvider = stripeProvider;
    this.paypalProvider = paypalProvider;
    this.manualProvider = manualProvider;

    log.info("PaymentProviderFactory initialized with configured provider: {}",
        properties.payment().provider());
  }

  /**
   * Returns the configured payment provider implementation.
   *
   * <p>This method selects the payment provider based on the configuration
   * property {@code iqscaffold.billing.payment.provider}. If the configured
   * provider is unavailable (e.g., missing credentials), it automatically
   * falls back to the manual provider.
   *
   * <p><strong>Provider Selection:</strong>
   * <ul>
   *   <li>"stripe" → {@link StripePaymentProvider}</li>
   *   <li>"paypal" → {@link PayPalPaymentProvider}</li>
   *   <li>"manual" → {@link ManualPaymentProvider}</li>
   *   <li>Unknown → {@link ManualPaymentProvider} (with warning)</li>
   * </ul>
   *
   * <p><strong>Fallback Logic:</strong>
   * If the configured provider is unavailable (e.g., Stripe configured but
   * API key missing), the factory logs a warning and returns the manual
   * provider to prevent service disruption.
   *
   * @return the selected payment provider implementation
   * @throws IllegalStateException if no provider is available (should never happen)
   */
  public PaymentProviderAdapter getProvider() {
    String configuredProvider = properties.payment().provider();

    log.debug("Selecting payment provider: {}", configuredProvider);

    // Use switch expression for provider selection (Java 21 feature)
    PaymentProviderAdapter provider = switch (configuredProvider.toLowerCase()) {
      case "stripe" -> {
        if (isStripeAvailable()) {
          log.debug("Using Stripe payment provider");
          yield stripeProvider;
        } else {
          log.warn("Stripe provider configured but unavailable (missing credentials), " +
                   "falling back to manual provider");
          yield manualProvider;
        }
      }
      case "paypal" -> {
        if (isPayPalAvailable()) {
          log.debug("Using PayPal payment provider");
          yield paypalProvider;
        } else {
          log.warn("PayPal provider configured but unavailable (missing credentials), " +
                   "falling back to manual provider");
          yield manualProvider;
        }
      }
      case "manual" -> {
        log.debug("Using manual payment provider");
        yield manualProvider;
      }
      default -> {
        log.warn("Unknown payment provider configured: {}, falling back to manual provider",
            configuredProvider);
        yield manualProvider;
      }
    };

    log.info("Selected payment provider: {}", provider.getProviderName());
    return provider;
  }

  /**
   * Returns a specific payment provider by name.
   *
   * <p>This method allows explicit provider selection, bypassing the
   * configuration. Useful for testing or scenarios where multiple providers
   * need to be used simultaneously.
   *
   * @param providerName the provider name ("stripe", "paypal", or "manual")
   * @return the requested payment provider implementation
   * @throws IllegalArgumentException if provider name is unknown
   */
  public PaymentProviderAdapter getProvider(String providerName) {
    log.debug("Explicitly selecting payment provider: {}", providerName);

    return switch (providerName.toLowerCase()) {
      case "stripe" -> stripeProvider;
      case "paypal" -> paypalProvider;
      case "manual" -> manualProvider;
      default -> throw new IllegalArgumentException(
          "Unknown payment provider: " + providerName +
          ". Valid options: stripe, paypal, manual"
      );
    };
  }

  /**
   * Checks if Stripe provider is available and properly configured.
   *
   * <p>Stripe is considered available if the API key is configured.
   * The webhook secret is optional for payment processing but required
   * for webhook verification.
   *
   * @return true if Stripe is available, false otherwise
   */
  private boolean isStripeAvailable() {
    String apiKey = properties.payment().stripe().apiKey();
    boolean available = apiKey != null && !apiKey.isBlank();

    if (!available) {
      log.debug("Stripe provider unavailable: API key not configured");
    }

    return available;
  }

  /**
   * Checks if PayPal provider is available and properly configured.
   *
   * <p>PayPal is considered available if both client ID and client secret
   * are configured. Both credentials are required for OAuth2 authentication.
   *
   * @return true if PayPal is available, false otherwise
   */
  private boolean isPayPalAvailable() {
    String clientId = properties.payment().paypal().clientId();
    String clientSecret = properties.payment().paypal().clientSecret();

    boolean available = clientId != null && !clientId.isBlank() &&
                        clientSecret != null && !clientSecret.isBlank();

    if (!available) {
      log.debug("PayPal provider unavailable: credentials not configured");
    }

    return available;
  }

  /**
   * Returns the name of the currently configured provider.
   *
   * <p>This method returns the provider name from configuration, which may
   * differ from the actual provider being used if fallback has occurred.
   * Use {@code getProvider().getProviderName()} to get the actual provider name.
   *
   * @return the configured provider name
   */
  public String getConfiguredProviderName() {
    return properties.payment().provider();
  }

  /**
   * Checks if the configured provider is available.
   *
   * <p>This method verifies that the configured provider has all required
   * credentials and is ready to process payments.
   *
   * @return true if the configured provider is available, false if fallback will occur
   */
  public boolean isConfiguredProviderAvailable() {
    return switch (properties.payment().provider().toLowerCase()) {
      case "stripe" -> isStripeAvailable();
      case "paypal" -> isPayPalAvailable();
      case "manual" -> true; // Manual provider is always available
      default -> false;
    };
  }
}
