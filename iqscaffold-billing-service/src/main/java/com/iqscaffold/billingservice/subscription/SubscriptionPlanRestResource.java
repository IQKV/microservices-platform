package com.iqscaffold.billingservice.subscription;

import jakarta.validation.Valid;
import java.util.UUID;

import com.iqscaffold.billingservice.security.BillingAuthorizationService;
import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for subscription plan management (admin operations).
 * <p>
 * Provides endpoints for platform administrators to manage subscription plans:
 * <ul>
 *   <li>Creating new subscription plans</li>
 *   <li>Viewing plan details</li>
 *   <li>Updating existing plans</li>
 *   <li>Synchronizing plans with Stripe</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * Plan management requires: SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN role
 */
@RestController
@RequestMapping("/api/v1/billing/subscription-plans")
@Tag(name = "Subscription Plans", description = "Platform-wide subscription plan management (admin)")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionPlanRestResource {

  private final SubscriptionPlanService subscriptionPlanService;
  private final BillingAuthorizationService authorizationService;

  public SubscriptionPlanRestResource(
      final SubscriptionPlanService subscriptionPlanService,
      final BillingAuthorizationService authorizationService) {
    this.subscriptionPlanService = subscriptionPlanService;
    this.authorizationService = authorizationService;
  }

  @Operation(
      summary = "Create subscription plan",
      description = "Creates a new platform-wide subscription plan and synchronizes it with Stripe")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription plan created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN role")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.PlanResponse> createPlan(
      @Valid @RequestBody SubscriptionDtos.UpsertPlanRequest request) {
    // Verify authorization
    authorizationService.requirePlanManagePermission();
    return ResponseEntity.ok(subscriptionPlanService.createPlan(request));
  }

  @Operation(
      summary = "Get subscription plan by ID",
      description = "Retrieves details of a specific subscription plan")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Plan found"),
      @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  @GetMapping("/{id}")
  public ResponseEntity<SubscriptionDtos.PlanResponse> getPlan(@PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionPlanService.getPlan(id));
  }

  @Operation(
      summary = "List all subscription plans",
      description = "Retrieves a paginated list of all subscription plans (active and inactive)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Plans retrieved successfully")
  })
  @GetMapping
  public ResponseEntity<Page<SubscriptionDtos.PlanResponse>> listPlans(Pageable pageable) {
    return ResponseEntity.ok(subscriptionPlanService.getAllPlans(pageable));
  }

  @Operation(
      summary = "List active subscription plans",
      description = "Retrieves a list of all active subscription plans available for tenants")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Active plans retrieved successfully")
  })
  @GetMapping("/active")
  public ResponseEntity<java.util.List<SubscriptionDtos.PlanResponse>> listActivePlans() {
    return ResponseEntity.ok(subscriptionPlanService.getActivePlans());
  }

  @Operation(
      summary = "Update subscription plan",
      description = "Updates an existing subscription plan and synchronizes changes with Stripe")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Plan updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.PlanResponse> updatePlan(
      @PathVariable UUID id,
      @Valid @RequestBody SubscriptionDtos.UpsertPlanRequest request) {
    // Verify authorization
    authorizationService.requirePlanManagePermission();
    return ResponseEntity.ok(subscriptionPlanService.updatePlan(id, request));
  }

  @Operation(
      summary = "Synchronize plan with Stripe",
      description = "Synchronizes a plan's details with Stripe (product and price)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Plan synchronized successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  @PostMapping("/{id}/sync")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<Void> syncPlanWithStripe(@PathVariable UUID id) {
    // Verify authorization
    authorizationService.requirePlanManagePermission();
    subscriptionPlanService.syncPlanWithStripe(id);
    return ResponseEntity.noContent().build();
  }
}
