package com.iqscaffold.billingservice.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/webhooks")
@Tag(name = "Webhooks", description = "Incoming webhooks from payment providers")
public class StripeWebhookRestResource {

  private final WebhookService webhookService;

  public StripeWebhookRestResource(WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @Operation(summary = "Handle Stripe events", description = "Receives and processes asynchronous events from Stripe")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Event processed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid signature or request")
  })
  @PostMapping("/stripe")
  public ResponseEntity<Void> handleStripeWebhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String sigHeader
  ) {
    webhookService.processWebhook(payload, sigHeader);
    return ResponseEntity.ok().build();
  }
}
