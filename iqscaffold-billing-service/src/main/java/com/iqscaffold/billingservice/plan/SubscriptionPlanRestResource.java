package com.iqscaffold.billingservice.plan;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for subscription plan management endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plans", description = "Subscription plan management operations")
public class SubscriptionPlanRestResource {

  private final SubscriptionPlanService subscriptionPlanService;

  // REST endpoints will be added in subsequent tasks
}
