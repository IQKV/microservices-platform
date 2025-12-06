package com.iqscaffold.billingservice.usage;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for usage tracking and quota enforcement endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/usage")
@RequiredArgsConstructor
@Tag(name = "Usage Tracking", description = "Usage metering and quota enforcement operations")
public class UsageRestResource {

  private final UsageMeteringService usageMeteringService;

  // REST endpoints will be added in subsequent tasks
}
