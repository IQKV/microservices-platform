package com.iqscaffold.billingservice.subscription;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for subscription management endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "Subscription lifecycle operations")
public class SubscriptionRestResource {

  private final SubscriptionService subscriptionService;

  // REST endpoints will be added in subsequent tasks
}
