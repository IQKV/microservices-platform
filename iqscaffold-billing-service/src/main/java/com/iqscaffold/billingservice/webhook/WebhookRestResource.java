package com.iqscaffold.billingservice.webhook;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for webhook endpoints from payment providers.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Payment provider webhook endpoints")
public class WebhookRestResource {

  private final WebhookService webhookService;

  // REST endpoints will be added in subsequent tasks
}
