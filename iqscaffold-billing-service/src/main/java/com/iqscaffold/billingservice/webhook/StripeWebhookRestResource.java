package com.iqscaffold.billingservice.webhook;

import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for receiving webhooks from multiple payment gateway providers.
 * <p>
 * This controller provides unified webhook endpoints for all supported payment providers:
 * <ul>
 *   <li>Stripe: /api/v1/billing/webhooks/stripe</li>
 *   <li>PayPal: /api/v1/billing/webhooks/paypal</li>
 *   <li>Square: /api/v1/billing/webhooks/square</li>
 *   <li>Braintree: /api/v1/billing/webhooks/braintree</li>
 * </ul>
 * <p>
 * Processing Flow:
 * <ol>
 *   <li>Receive webhook payload and signature header from provider</li>
 *   <li>Use {@link PaymentProviderFactory} to get the appropriate provider adapter</li>
 *   <li>Delegate signature verification and parsing to the adapter</li>
 *   <li>Pass normalized {@link WebhookEvent} to {@link UnifiedWebhookService}</li>
 * </ol>
 * <p>
 * Security: Each provider adapter performs cryptographic signature verification
 * before parsing the webhook, ensuring the webhook originated from the payment provider.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
@Tag(name = "Webhooks", description = "Incoming webhooks from payment providers")
public class StripeWebhookRestResource {
  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookRestResource.class);

  private final UnifiedWebhookService unifiedWebhookService;
  private final PaymentProviderFactory providerFactory;

  public StripeWebhookRestResource(
      final UnifiedWebhookService unifiedWebhookService,
      final PaymentProviderFactory providerFactory) {
    this.unifiedWebhookService = unifiedWebhookService;
    this.providerFactory = providerFactory;
  }

  /**
   * Unified webhook endpoint for all payment providers.
   * <p>
   * Supports dynamic provider routing based on path variable:
   * - /api/v1/billing/webhooks/stripe (Stripe-Signature header)
   * - /api/v1/billing/webhooks/paypal (PayPal-Transmission-Sig header)
   * - /api/v1/billing/webhooks/square (X-Square-Signature header)
   * - /api/v1/billing/webhooks/braintree (X-Braintree-Signature header)
   */
  @Operation(
      summary = "Handle payment provider webhook",
      description = "Receives and processes asynchronous events from any supported payment provider"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Event processed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid signature or request"),
      @ApiResponse(responseCode = "404", description = "Provider not supported")
  })
  @PostMapping("/{provider}")
  public ResponseEntity<Void> handleWebhook(
      @Parameter(description = "Payment provider name (stripe, paypal, square, braintree)")
      @PathVariable("provider") String providerName,
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
      @RequestHeader(value = "PayPal-Transmission-Sig", required = false) String paypalSignature,
      @RequestHeader(value = "X-Square-Signature", required = false) String squareSignature,
      @RequestHeader(value = "X-Braintree-Signature", required = false) String braintreeSignature
  ) {
    logger.info("Received webhook from provider: {}", providerName);

    // Parse provider name to enum
    PaymentGatewayProvider provider;
    try {
      provider = PaymentGatewayProvider.valueOf(providerName.toUpperCase());
    } catch (final IllegalArgumentException e) {
      logger.error("Unsupported payment provider: {}", providerName);
      return ResponseEntity.notFound().build();
    }

    // Check if provider is supported
    if (!providerFactory.isProviderSupported(provider)) {
      logger.error("Payment provider not configured: {}", provider);
      return ResponseEntity.notFound().build();
    }

    // Get signature header based on provider
    String signatureHeader = getSignatureHeader(provider, stripeSignature, paypalSignature, squareSignature, braintreeSignature);
    if (signatureHeader == null) {
      logger.error("Missing signature header for provider: {}", provider);
      return ResponseEntity.badRequest().build();
    }

    // Get provider adapter and verify/parse webhook
    PaymentProviderAdapter adapter = providerFactory.getProvider(provider);
    WebhookEvent event = adapter.verifyAndParseWebhook(payload, signatureHeader);

    // Process the normalized event
    unifiedWebhookService.processWebhookEvent(event);

    return ResponseEntity.ok().build();
  }

  /**
   * Gets the appropriate signature header based on the payment provider.
   */
  private String getSignatureHeader(
      PaymentGatewayProvider provider,
      String stripeSignature,
      String paypalSignature,
      String squareSignature,
      String braintreeSignature
  ) {
    return switch (provider) {
      case STRIPE -> stripeSignature;
      case PAYPAL -> paypalSignature;
      case SQUARE -> squareSignature;
      case BRAINTREE -> braintreeSignature;
    };
  }
}
