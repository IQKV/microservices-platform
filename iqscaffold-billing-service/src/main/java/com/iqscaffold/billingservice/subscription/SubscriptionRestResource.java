package com.iqscaffold.billingservice.subscription;

import jakarta.validation.Valid;
import java.util.UUID;

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
 * REST API for tenant subscription management.
 * <p>
 * Provides endpoints for tenants to manage their subscriptions including:
 * <ul>
 *   <li>Creating new subscriptions</li>
 *   <li>Viewing subscription details</li>
 *   <li>Updating subscription plans</li>
 *   <li>Canceling subscriptions</li>
 *   <li>Pausing and resuming subscriptions</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@Tag(name = "Subscriptions", description = "Tenant subscription lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionRestResource {

  private final SubscriptionService subscriptionService;

  public SubscriptionRestResource(final SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @Operation(
      summary = "Create subscription",
      description = "Creates a new subscription for the current tenant with the specified plan")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or tenant already has active subscription"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires TENANT_OWNER or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Subscription plan not found")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> createSubscription(
      @Valid @RequestBody SubscriptionDtos.CreateSubscriptionRequest request) {
    return ResponseEntity.ok(subscriptionService.createSubscription(request));
  }

  @Operation(
      summary = "Get active subscription",
      description = "Retrieves the active subscription for the current tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Active subscription found"),
      @ApiResponse(responseCode = "204", description = "No active subscription"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/active")
  @PreAuthorize("hasAuthority('USER')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> getActiveSubscription() {
    return subscriptionService.getActiveSubscription()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @Operation(
      summary = "Get subscription by ID",
      description = "Retrieves a specific subscription by its ID for the current tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Subscription not found or access denied")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('USER')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> getSubscription(
      @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.getSubscription(id));
  }

  @Operation(
      summary = "List subscriptions",
      description = "Retrieves a paginated list of all subscriptions for the current tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<Page<SubscriptionDtos.SubscriptionResponse>> listSubscriptions(
      Pageable pageable) {
    return ResponseEntity.ok(subscriptionService.getSubscriptions(pageable));
  }

  @Operation(
      summary = "Update subscription",
      description = "Updates a subscription (e.g., change plan, update payment method)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or update not allowed in current state"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires TENANT_OWNER or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Subscription or new plan not found")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> updateSubscription(
      @PathVariable UUID id,
      @Valid @RequestBody SubscriptionDtos.UpdateSubscriptionRequest request) {
    return ResponseEntity.ok(subscriptionService.updateSubscription(id, request));
  }

  @Operation(
      summary = "Cancel subscription",
      description = "Cancels a subscription at the end of the current billing period")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription canceled successfully"),
      @ApiResponse(responseCode = "400", description = "Cancellation not allowed in current state"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires TENANT_OWNER or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Subscription not found")
  })
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> cancelSubscription(
      @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.cancelSubscription(id));
  }

  @Operation(
      summary = "Cancel subscription immediately",
      description = "Cancels a subscription immediately without waiting for the billing period to end")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription canceled immediately"),
      @ApiResponse(responseCode = "400", description = "Cancellation not allowed in current state"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires TENANT_OWNER or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Subscription not found")
  })
  @PostMapping("/{id}/cancel-immediately")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> cancelSubscriptionImmediately(
      @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.cancelSubscriptionImmediately(id));
  }

  @Operation(
      summary = "Pause subscription",
      description = "Pauses a subscription, preventing billing while preserving the subscription")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription paused successfully"),
      @ApiResponse(responseCode = "400", description = "Pause not allowed in current state"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires TENANT_OWNER or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Subscription not found")
  })
  @PostMapping("/{id}/pause")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> pauseSubscription(
      @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.pauseSubscription(id));
  }

  @Operation(
      summary = "Resume subscription",
      description = "Resumes a paused subscription")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription resumed successfully"),
      @ApiResponse(responseCode = "400", description = "Resume not allowed in current state"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires TENANT_OWNER or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Subscription not found")
  })
  @PostMapping("/{id}/resume")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<SubscriptionDtos.SubscriptionResponse> resumeSubscription(
      @PathVariable UUID id) {
    return ResponseEntity.ok(subscriptionService.resumeSubscription(id));
  }
}
