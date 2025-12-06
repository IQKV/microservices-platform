package com.iqscaffold.billingservice.portal;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer billing portal endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/portal")
@RequiredArgsConstructor
@Tag(name = "Billing Portal", description = "Customer billing portal operations")
public class BillingPortalRestResource {

  private final BillingPortalService billingPortalService;

  // REST endpoints will be added in subsequent tasks
}
