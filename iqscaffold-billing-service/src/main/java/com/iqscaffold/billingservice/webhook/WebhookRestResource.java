package com.iqscaffold.billingservice.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for webhook endpoints from payment providers.
 *
 * <p>This controller provides webhook endpoints for Stripe and PayPal payment providers.
 * Webhooks are processed asynchronously to ensure fast response times (< 5 seconds)
 * as required by payment providers.
 *
 * <h2>Processing Pattern</h2>
 * <ol>
 *   <li>Validate webhook signature</li>
 *   <li>Store webhook event for idempotency</li>
 *   <li>Publish to RabbitMQ for async processing</li>
 *   <li>Return 200 OK immediately</li>
 * </ol>
 *
 * <h2>Security</h2>
 * <p>All webhook endpoints verify signatures before processing to prevent
 * malicious webhook injection attacks. Invalid signatures return 401 Unauthorized.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment provider webhook endpoints")
public class WebhookRestResource {

  private final WebhookApplicationService webhookApplicationService;

  /**
   * Processes Stripe webhook events.
   *
   * <p><strong>Async Processing:</strong> This endpoint returns 200 OK immediately
   * after validating the signature and queuing the event. Actual processing happens
   * asynchronously via RabbitMQ consumers.
   *
   * <p><strong>Idempotency:</strong> Duplicate webhook events (same event ID) are
   * automatically detected and return success without reprocessing.
   *
   * @param payload   the raw webhook payload (JSON string)
   * @param signature the Stripe signature header (Stripe-Signature)
   * @return webhook event ID for tracking
   */
  @PostMapping("/stripe")
  @Operation(
      summary = "Process Stripe webhook",
      description = """
          Receives and processes webhook events from Stripe payment provider.
          
          ## Processing Flow
          1. Validates webhook signature using Stripe webhook secret
          2. Checks idempotency (returns success if already processed)
          3. Stores webhook event for audit trail
          4. Publishes to RabbitMQ for async processing
          5. Returns 200 OK immediately (< 5 seconds)
          
          ## Supported Event Types
          - payment_intent.succeeded - Payment completed successfully
          - payment_intent.failed - Payment failed
          - charge.refunded - Payment refunded
          - customer.subscription.updated - Subscription updated
          
          ## Security
          Webhook signature is verified using the Stripe webhook secret.
          Invalid signatures return 401 Unauthorized.
          
          ## Idempotency
          Duplicate webhook events (same event ID) are automatically detected
          and return success without reprocessing.
          """,
      tags = {"Webhooks"}
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Webhook received and queued for processing",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = WebhookResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Invalid webhook signature"
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error"
      )
  })
  public ResponseEntity<WebhookResponse> processStripeWebhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String signature
  ) {
    log.info("Received Stripe webhook");

    String eventId = webhookApplicationService.processStripeWebhook(payload, signature);

    return ResponseEntity.ok(new WebhookResponse(eventId, "Webhook received and queued for processing"));
  }

  /**
   * Processes PayPal webhook events.
   *
   * <p><strong>Async Processing:</strong> This endpoint returns 200 OK immediately
   * after validating the signature and queuing the event. Actual processing happens
   * asynchronously via RabbitMQ consumers.
   *
   * @param payload   the raw webhook payload (JSON string)
   * @param signature the PayPal signature header
   * @return webhook event ID for tracking
   */
  @PostMapping("/paypal")
  @Operation(
      summary = "Process PayPal webhook",
      description = """
          Receives and processes webhook events from PayPal payment provider.
          
          ## Processing Flow
          1. Validates webhook signature using PayPal webhook secret
          2. Checks idempotency (returns success if already processed)
          3. Stores webhook event for audit trail
          4. Publishes to RabbitMQ for async processing
          5. Returns 200 OK immediately (< 5 seconds)
          
          ## Security
          Webhook signature is verified using the PayPal webhook secret.
          Invalid signatures return 401 Unauthorized.
          """,
      tags = {"Webhooks"}
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Webhook received and queued for processing",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = WebhookResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Invalid webhook signature"
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error"
      )
  })
  public ResponseEntity<WebhookResponse> processPayPalWebhook(
      @RequestBody String payload,
      @RequestHeader("PayPal-Transmission-Sig") String signature
  ) {
    log.info("Received PayPal webhook");

    String eventId = webhookApplicationService.processPayPalWebhook(payload, signature);

    return ResponseEntity.ok(new WebhookResponse(eventId, "Webhook received and queued for processing"));
  }

  /**
   * Response DTO for webhook endpoints.
   */
  public record WebhookResponse(
      @Schema(description = "Webhook event ID", example = "evt_1234567890")
      String eventId,

      @Schema(description = "Status message", example = "Webhook received and queued for processing")
      String message
  ) {
  }
}
