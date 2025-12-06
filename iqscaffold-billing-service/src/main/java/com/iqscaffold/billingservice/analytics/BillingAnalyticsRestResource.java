package com.iqscaffold.billingservice.analytics;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for billing analytics endpoints (admin only).
 */
@RestController
@RequestMapping("/api/v1/admin/billing/analytics")
@RequiredArgsConstructor
@Tag(name = "Billing Analytics", description = "Billing analytics and reporting operations (admin)")
public class BillingAnalyticsRestResource {

  private final BillingAnalyticsService billingAnalyticsService;

  // REST endpoints will be added in subsequent tasks
}
