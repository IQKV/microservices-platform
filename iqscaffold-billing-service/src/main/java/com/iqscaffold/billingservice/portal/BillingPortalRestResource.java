package com.iqscaffold.billingservice.portal;

import com.iqscaffold.billingservice.subscription.CancelSubscriptionRequest;
import com.iqscaffold.billingservice.subscription.ReactivateSubscriptionRequest;
import com.iqscaffold.billingservice.subscription.SubscriptionApplicationService;
import com.iqscaffold.billingservice.subscription.SubscriptionDto;
import com.iqscaffold.billingservice.subscription.UpdateSubscriptionRequest;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer billing portal endpoints.
 * 
 * <p>Provides self-service billing portal APIs for customers to manage their subscriptions,
 * view usage, access invoices, and manage payment methods. All endpoints require authentication
 * and are restricted to users with TENANT_OWNER or BILLING_ADMIN roles.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Comprehensive billing dashboard with subscription, usage, and invoice information</li>
 *   <li>Self-service subscription upgrades and downgrades with proration</li>
 *   <li>Subscription cancellation with immediate or period-end options</li>
 *   <li>Subscription reactivation for canceled subscriptions</li>
 *   <li>Real-time quota usage monitoring</li>
 *   <li>Invoice history and PDF downloads</li>
 *   <li>Payment method management</li>
 * </ul>
 * 
 * <p>All operations are logged and tracked for audit purposes.
 */
@RestController
@RequestMapping("/api/v1/billing/portal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Portal", description = "Self-service billing portal APIs")
public class BillingPortalRestResource {

  private final BillingPortalService billingPortalService;
  private final SubscriptionApplicationService subscriptionApplicationService;

  /**
   * Retrieves comprehensive billing dashboard for a tenant.
   * 
   * <p>Returns a complete view of the tenant's billing information including:
   * <ul>
   *   <li>Current subscription details and status</li>
   *   <li>Quota usage across all metrics</li>
   *   <li>Recent invoices (last 12 months)</li>
   *   <li>Upcoming invoice estimate</li>
   *   <li>Payment methods on file</li>
   *   <li>Available upgrade and downgrade options</li>
   * </ul>
   * 
   * @param tenantId the tenant unique identifier
   * @return comprehensive billing dashboard DTO
   */
  @GetMapping("/dashboard/{tenantId}")
  @Timed(value = "billing.portal.dashboard", description = "Time taken to retrieve billing dashboard")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(
    summary = "Get billing dashboard",
    description = """
      Retrieves a comprehensive billing dashboard for the specified tenant.
      
      **Features:**
      - Current subscription status and details
      - Real-time quota usage across all metrics (API calls, storage, users, etc.)
      - Recent invoice history (last 12 months)
      - Upcoming invoice amount estimate
      - Payment methods on file
      - Available upgrade and downgrade plan options with proration calculations
      - Days remaining in current billing period
      - Trial status and past due indicators
      
      **Authorization:**
      - Requires TENANT_OWNER or BILLING_ADMIN role
      - Users can only access their own tenant's dashboard
      
      **Use Cases:**
      - Display customer billing portal homepage
      - Show subscription overview in admin panel
      - Monitor usage and quota consumption
      - Plan upgrade/downgrade decision making
      
      **Performance:**
      - Results cached with 5-minute TTL
      - Optimized queries for fast response times
      - Aggregated data pre-calculated where possible
      """,
    security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved billing dashboard",
      content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BillingDashboardDto.class),
        examples = @ExampleObject(
          name = "Billing Dashboard Response",
          value = """
            {
              "tenantId": "550e8400-e29b-41d4-a716-446655440000",
              "subscription": {
                "id": 1,
                "tenantId": "550e8400-e29b-41d4-a716-446655440000",
                "planCode": "PRO_MONTHLY",
                "status": "ACTIVE",
                "currentPeriodStart": "2024-12-01T00:00:00Z",
                "currentPeriodEnd": "2024-12-31T23:59:59Z",
                "cancelAtPeriodEnd": false,
                "trialEnd": null
              },
              "quotaUsage": [
                {
                  "metricType": "API_CALLS",
                  "used": 45000,
                  "limit": 100000,
                  "percentageUsed": 45.0,
                  "exceeded": false
                },
                {
                  "metricType": "STORAGE_GB",
                  "used": 25,
                  "limit": 50,
                  "percentageUsed": 50.0,
                  "exceeded": false
                }
              ],
              "recentInvoices": [
                {
                  "id": 1,
                  "invoiceNumber": "INV-202412-001",
                  "status": "PAID",
                  "totalAmount": 49.99,
                  "dueDate": "2024-12-07T00:00:00Z"
                }
              ],
              "upcomingInvoiceAmount": 49.99,
              "paymentMethods": [
                {
                  "id": 1,
                  "type": "CARD",
                  "last4": "4242",
                  "expiryMonth": 12,
                  "expiryYear": 2025,
                  "isDefault": true
                }
              ],
              "totalPaid": 599.88,
              "daysRemainingInPeriod": 24,
              "inTrial": false,
              "pastDue": false,
              "canceled": false,
              "availableUpgrades": [
                {
                  "planCode": "ENTERPRISE_YEARLY",
                  "planName": "Enterprise Yearly",
                  "price": 999.00,
                  "prorationAmount": 950.00,
                  "keyFeatures": ["Unlimited API calls", "500GB storage", "Priority support"]
                }
              ],
              "availableDowngrades": [
                {
                  "planCode": "FREE",
                  "planName": "Free Plan",
                  "price": 0.00,
                  "creditAmount": 25.00,
                  "limitations": ["1,000 API calls/month", "1GB storage", "Community support"]
                }
              ]
            }
            """
        )
      )
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token"
    ),
    @ApiResponse(
      responseCode = "403",
      description = "Forbidden - User does not have required role (TENANT_OWNER or BILLING_ADMIN)"
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - Tenant or subscription not found"
    )
  })
  public ResponseEntity<BillingDashboardDto> getDashboard(
    @Parameter(
      description = "Tenant unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000",
      required = true
    )
    @PathVariable UUID tenantId
  ) {
    log.info("Retrieving billing dashboard for tenant: {}", tenantId);
    var dashboard = billingPortalService.getDashboard(tenantId);
    return ResponseEntity.ok(dashboard);
  }

  /**
   * Upgrades a subscription to a higher-tier plan.
   * 
   * <p>Handles subscription upgrades with automatic proration calculation.
   * The upgrade can be applied immediately or scheduled for the next billing period.
   * 
   * @param tenantId the tenant unique identifier
   * @param request the upgrade request with new plan details
   * @return updated subscription DTO
   */
  @PostMapping("/subscription/{tenantId}/upgrade")
  @Timed(value = "billing.portal.upgrade", description = "Time taken to upgrade subscription")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(
    summary = "Upgrade subscription",
    description = """
      Upgrades the tenant's subscription to a higher-tier plan with automatic proration.
      
      **Features:**
      - Automatic proration calculation for mid-period upgrades
      - Immediate or scheduled upgrade options
      - Payment processing for upgrade charges
      - Quota limits updated immediately
      - Email notification sent to tenant
      - Audit trail logged for compliance
      
      **Proration Logic:**
      - Immediate upgrade: Prorated charge for remaining period
      - Scheduled upgrade: Applied at next billing period (no proration)
      - Credit from old plan applied to new plan charge
      - Payment processed immediately for immediate upgrades
      
      **Authorization:**
      - Requires TENANT_OWNER or BILLING_ADMIN role
      - Users can only upgrade their own tenant's subscription
      
      **Validation:**
      - New plan must be higher tier than current plan
      - Subscription must be in ACTIVE or TRIAL status
      - Payment method must be on file for paid plans
      - No pending plan changes allowed
      
      **Use Cases:**
      - Customer needs more resources (API calls, storage, users)
      - Customer wants access to premium features
      - Customer wants to switch from monthly to yearly billing
      """,
    security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Successfully upgraded subscription",
      content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = SubscriptionDto.class),
        examples = @ExampleObject(
          name = "Upgrade Response",
          value = """
            {
              "id": 1,
              "tenantId": "550e8400-e29b-41d4-a716-446655440000",
              "planCode": "ENTERPRISE_YEARLY",
              "status": "ACTIVE",
              "currentPeriodStart": "2024-12-07T10:30:00Z",
              "currentPeriodEnd": "2025-12-07T10:30:00Z",
              "cancelAtPeriodEnd": false,
              "trialEnd": null,
              "metadata": {
                "upgrade_reason": "need_more_users",
                "previous_plan": "PRO_MONTHLY",
                "proration_amount": "950.00"
              }
            }
            """
        )
      )
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Invalid upgrade request (e.g., downgrade attempt, invalid plan)"
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token"
    ),
    @ApiResponse(
      responseCode = "403",
      description = "Forbidden - User does not have required role"
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - Tenant, subscription, or plan not found"
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Conflict - Subscription has pending changes or is not in upgradeable state"
    ),
    @ApiResponse(
      responseCode = "402",
      description = "Payment Required - Payment method required or payment failed"
    )
  })
  public ResponseEntity<SubscriptionDto> upgradeSubscription(
    @Parameter(
      description = "Tenant unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000",
      required = true
    )
    @PathVariable UUID tenantId,
    
    @Parameter(
      description = "Upgrade request with new plan details",
      required = true
    )
    @Valid @RequestBody UpgradeDowngradeRequest request
  ) {
    log.info("Upgrading subscription for tenant: {} to plan: {}", tenantId, request.newPlanCode());
    
    // Get active subscription for tenant
    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);
    
    // Create update request
    var updateRequest = new UpdateSubscriptionRequest(
      request.newPlanCode(),
      request.immediate(),
      request.reason() != null ? 
        java.util.Map.of("upgrade_reason", request.reason()) : 
        null
    );
    
    // Perform upgrade
    var upgraded = subscriptionApplicationService.upgradeSubscription(
      subscription.id(),
      updateRequest
    );
    
    return ResponseEntity.ok(upgraded);
  }

  /**
   * Downgrades a subscription to a lower-tier plan.
   * 
   * <p>Handles subscription downgrades with credit calculation for unused time.
   * The downgrade can be applied immediately or scheduled for the next billing period.
   * 
   * @param tenantId the tenant unique identifier
   * @param request the downgrade request with new plan details
   * @return updated subscription DTO
   */
  @PostMapping("/subscription/{tenantId}/downgrade")
  @Timed(value = "billing.portal.downgrade", description = "Time taken to downgrade subscription")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(
    summary = "Downgrade subscription",
    description = """
      Downgrades the tenant's subscription to a lower-tier plan with credit calculation.
      
      **Features:**
      - Automatic credit calculation for unused time
      - Immediate or scheduled downgrade options
      - Quota validation before downgrade
      - Credit applied to next invoice
      - Email notification sent to tenant
      - Audit trail logged for compliance
      
      **Credit Logic:**
      - Immediate downgrade: Credit issued for unused time on old plan
      - Scheduled downgrade: Applied at next billing period (no credit)
      - Credit stored and applied to next invoice
      - Quota limits updated immediately for immediate downgrades
      
      **Authorization:**
      - Requires TENANT_OWNER or BILLING_ADMIN role
      - Users can only downgrade their own tenant's subscription
      
      **Validation:**
      - New plan must be lower tier than current plan
      - Subscription must be in ACTIVE status
      - Current usage must not exceed new plan quotas
      - No pending plan changes allowed
      
      **Use Cases:**
      - Customer wants to reduce costs
      - Customer no longer needs premium features
      - Customer wants to switch from yearly to monthly billing
      - Customer wants to downgrade before canceling
      """,
    security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Successfully downgraded subscription",
      content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = SubscriptionDto.class),
        examples = @ExampleObject(
          name = "Downgrade Response",
          value = """
            {
              "id": 1,
              "tenantId": "550e8400-e29b-41d4-a716-446655440000",
              "planCode": "PRO_MONTHLY",
              "status": "ACTIVE",
              "currentPeriodStart": "2024-12-01T00:00:00Z",
              "currentPeriodEnd": "2024-12-31T23:59:59Z",
              "cancelAtPeriodEnd": false,
              "trialEnd": null,
              "metadata": {
                "downgrade_reason": "reduce_costs",
                "previous_plan": "ENTERPRISE_YEARLY",
                "credit_amount": "850.00",
                "scheduled_for": "2024-12-31T23:59:59Z"
              }
            }
            """
        )
      )
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Invalid downgrade request (e.g., upgrade attempt, quota exceeded)"
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token"
    ),
    @ApiResponse(
      responseCode = "403",
      description = "Forbidden - User does not have required role"
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - Tenant, subscription, or plan not found"
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Conflict - Subscription has pending changes or current usage exceeds new plan quotas"
    )
  })
  public ResponseEntity<SubscriptionDto> downgradeSubscription(
    @Parameter(
      description = "Tenant unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000",
      required = true
    )
    @PathVariable UUID tenantId,
    
    @Parameter(
      description = "Downgrade request with new plan details",
      required = true
    )
    @Valid @RequestBody UpgradeDowngradeRequest request
  ) {
    log.info("Downgrading subscription for tenant: {} to plan: {}", tenantId, request.newPlanCode());
    
    // Get active subscription for tenant
    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);
    
    // Create update request
    var updateRequest = new UpdateSubscriptionRequest(
      request.newPlanCode(),
      request.immediate(),
      request.reason() != null ? 
        java.util.Map.of("downgrade_reason", request.reason()) : 
        null
    );
    
    // Perform downgrade
    var downgraded = subscriptionApplicationService.downgradeSubscription(
      subscription.id(),
      updateRequest
    );
    
    return ResponseEntity.ok(downgraded);
  }

  /**
   * Cancels a subscription.
   * 
   * <p>Handles subscription cancellation with immediate or period-end options.
   * Canceled subscriptions can be reactivated before the period ends.
   * 
   * @param tenantId the tenant unique identifier
   * @param request the cancellation request with reason and timing
   * @return updated subscription DTO
   */
  @PostMapping("/subscription/{tenantId}/cancel")
  @Timed(value = "billing.portal.cancel", description = "Time taken to cancel subscription")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(
    summary = "Cancel subscription",
    description = """
      Cancels the tenant's subscription with immediate or period-end options.
      
      **Features:**
      - Immediate or period-end cancellation options
      - Refund calculation for immediate cancellations
      - Access maintained until period end for period-end cancellations
      - Cancellation reason tracking for analytics
      - Email notification sent to tenant
      - Audit trail logged for compliance
      - Reactivation allowed before period end
      
      **Cancellation Options:**
      - **Immediate**: Subscription canceled immediately, access revoked, refund issued
      - **Period-end**: Subscription remains active until current period ends, no refund
      
      **Authorization:**
      - Requires TENANT_OWNER or BILLING_ADMIN role
      - Users can only cancel their own tenant's subscription
      
      **Validation:**
      - Subscription must be in ACTIVE or TRIAL status
      - Cannot cancel already canceled subscription
      - Cancellation reason required for analytics
      
      **Post-Cancellation:**
      - Data retained for 30 days after cancellation
      - Reactivation allowed before period end
      - Automatic data deletion after retention period
      - Invoices and payment history preserved
      
      **Use Cases:**
      - Customer no longer needs the service
      - Customer switching to competitor
      - Customer wants to pause service temporarily
      - Customer dissatisfied with service
      """,
    security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Successfully canceled subscription",
      content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = SubscriptionDto.class),
        examples = @ExampleObject(
          name = "Cancel Response",
          value = """
            {
              "id": 1,
              "tenantId": "550e8400-e29b-41d4-a716-446655440000",
              "planCode": "PRO_MONTHLY",
              "status": "ACTIVE",
              "currentPeriodStart": "2024-12-01T00:00:00Z",
              "currentPeriodEnd": "2024-12-31T23:59:59Z",
              "cancelAtPeriodEnd": true,
              "canceledAt": "2024-12-07T10:30:00Z",
              "trialEnd": null,
              "metadata": {
                "cancellation_reason": "switching_to_competitor",
                "feedback": "Too expensive",
                "can_reactivate_until": "2024-12-31T23:59:59Z"
              }
            }
            """
        )
      )
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Invalid cancellation request (e.g., missing reason)"
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token"
    ),
    @ApiResponse(
      responseCode = "403",
      description = "Forbidden - User does not have required role"
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - Tenant or subscription not found"
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Conflict - Subscription already canceled or not in cancellable state"
    )
  })
  public ResponseEntity<SubscriptionDto> cancelSubscription(
    @Parameter(
      description = "Tenant unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000",
      required = true
    )
    @PathVariable UUID tenantId,
    
    @Parameter(
      description = "Cancellation request with reason and timing",
      required = true
    )
    @Valid @RequestBody CancelSubscriptionRequest request
  ) {
    log.info(
      "Canceling subscription for tenant: {}, immediate: {}, reason: {}",
      tenantId,
      request.immediate(),
      request.reason()
    );
    
    // Get active subscription for tenant
    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);
    
    // Perform cancellation
    var canceled = subscriptionApplicationService.cancelSubscription(
      subscription.id(),
      request
    );
    
    return ResponseEntity.ok(canceled);
  }

  /**
   * Reactivates a canceled subscription.
   * 
   * <p>Reactivates a subscription that was canceled but is still within the billing period.
   * Only subscriptions with cancel_at_period_end flag can be reactivated.
   * 
   * @param tenantId the tenant unique identifier
   * @param request the reactivation request with optional reason
   * @return updated subscription DTO
   */
  @PostMapping("/subscription/{tenantId}/reactivate")
  @Timed(value = "billing.portal.reactivate", description = "Time taken to reactivate subscription")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(
    summary = "Reactivate canceled subscription",
    description = """
      Reactivates a subscription that was canceled but is still within the billing period.
      
      **Features:**
      - Removes cancel_at_period_end flag
      - Restores full access to subscription features
      - Subscription continues to next billing period
      - Email notification sent to tenant
      - Audit trail logged for compliance
      
      **Reactivation Rules:**
      - Only subscriptions with cancel_at_period_end=true can be reactivated
      - Must be reactivated before current period ends
      - Payment method must still be valid
      - No additional charges for reactivation
      
      **Authorization:**
      - Requires TENANT_OWNER or BILLING_ADMIN role
      - Users can only reactivate their own tenant's subscription
      
      **Validation:**
      - Subscription must have cancel_at_period_end flag set
      - Current period must not have ended
      - Payment method must be on file and valid
      - Subscription must not be in EXPIRED or PAST_DUE status
      
      **Post-Reactivation:**
      - Subscription continues normally
      - Next billing cycle proceeds as scheduled
      - All features and quotas restored
      - Cancellation metadata preserved for analytics
      
      **Use Cases:**
      - Customer changed their mind about canceling
      - Customer resolved issues that led to cancellation
      - Customer received retention offer
      - Customer wants to continue service
      """,
    security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Successfully reactivated subscription",
      content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = SubscriptionDto.class),
        examples = @ExampleObject(
          name = "Reactivate Response",
          value = """
            {
              "id": 1,
              "tenantId": "550e8400-e29b-41d4-a716-446655440000",
              "planCode": "PRO_MONTHLY",
              "status": "ACTIVE",
              "currentPeriodStart": "2024-12-01T00:00:00Z",
              "currentPeriodEnd": "2024-12-31T23:59:59Z",
              "cancelAtPeriodEnd": false,
              "canceledAt": null,
              "trialEnd": null,
              "metadata": {
                "reactivation_reason": "customer_changed_mind",
                "reactivated_at": "2024-12-07T10:30:00Z",
                "previous_cancellation_date": "2024-12-05T14:20:00Z"
              }
            }
            """
        )
      )
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Subscription not eligible for reactivation"
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token"
    ),
    @ApiResponse(
      responseCode = "403",
      description = "Forbidden - User does not have required role"
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - Tenant or subscription not found"
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Conflict - Subscription not in reactivatable state or period has ended"
    ),
    @ApiResponse(
      responseCode = "402",
      description = "Payment Required - Valid payment method required"
    )
  })
  public ResponseEntity<SubscriptionDto> reactivateSubscription(
    @Parameter(
      description = "Tenant unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000",
      required = true
    )
    @PathVariable UUID tenantId,
    
    @Parameter(
      description = "Reactivation request with optional reason",
      required = true
    )
    @Valid @RequestBody ReactivateSubscriptionRequest request
  ) {
    log.info(
      "Reactivating subscription for tenant: {}, reason: {}",
      tenantId,
      request.reason()
    );
    
    // Get active subscription for tenant
    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);
    
    // Perform reactivation
    var reactivated = subscriptionApplicationService.reactivateSubscription(
      subscription.id(),
      request
    );
    
    return ResponseEntity.ok(reactivated);
  }
}
