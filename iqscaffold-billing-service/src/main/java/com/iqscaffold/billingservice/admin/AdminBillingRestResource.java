package com.iqscaffold.billingservice.admin;

import jakarta.validation.Valid;

import com.iqscaffold.billingservice.plan.SubscriptionPlanDto;
import com.iqscaffold.billingservice.plan.SubscriptionPlanService;
import com.iqscaffold.billingservice.subscription.SubscriptionApplicationService;
import com.iqscaffold.billingservice.subscription.SubscriptionDto;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for admin billing operations.
 *
 * <p>Provides administrative APIs for managing subscription plans and subscriptions.
 * All endpoints require ADMIN or SUPER_ADMIN authority.
 *
 * <p>Key capabilities:
 * <ul>
 *   <li>Create and update subscription plans</li>
 *   <li>View all subscriptions across tenants with filtering</li>
 *   <li>Force cancel subscriptions</li>
 *   <li>Extend trial periods</li>
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>All endpoints require authentication</li>
 *   <li>All endpoints require ADMIN or SUPER_ADMIN authority</li>
 *   <li>JWT token validation via Spring Security</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Billing", description = "Administrative billing operations (ADMIN role required)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminBillingRestResource {

  private final SubscriptionPlanService subscriptionPlanService;
  private final SubscriptionApplicationService subscriptionApplicationService;
  private final AdminSubscriptionService adminSubscriptionService;
  private final com.iqscaffold.billingservice.invoice.InvoiceApplicationService invoiceApplicationService;
  private final com.iqscaffold.billingservice.analytics.BillingAnalyticsService billingAnalyticsService;

  /**
   * Creates a new subscription plan.
   *
   * <p>This endpoint allows administrators to create new subscription plans
   * with custom pricing, features, and quotas.
   *
   * @param request plan creation request
   * @return created subscription plan DTO
   */
  @PostMapping("/plans")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.plans.create", description = "Time taken to create subscription plan")
  @Operation(
      summary = "Create subscription plan",
      description = """
          Creates a new subscription plan with specified pricing, features, and quotas.
          
          **Features:**
          - Define custom pricing and billing cycles
          - Configure feature flags for plan capabilities
          - Set resource quotas and limits
          - Configure trial period
          - Control public visibility
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          - Plan code must be unique
          - Base price must be non-negative
          - Trial days must be non-negative
          
          **Use Cases:**
          - Create new pricing tiers
          - Add seasonal or promotional plans
          - Configure custom enterprise plans
          - Set up trial plans
          
          **Example Request:**
          Creates a new PRO plan with monthly billing, advanced features, and 14-day trial.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Subscription plan created successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionPlanDto.class),
              examples = @ExampleObject(
                  name = "Created Plan Response",
                  value = """
                      {
                        "id": 5,
                        "planCode": "PRO_MONTHLY_V2",
                        "name": "Professional Monthly V2",
                        "description": "Enhanced professional plan with new features",
                        "tier": "PRO",
                        "billingCycle": "MONTHLY",
                        "basePrice": 59.99,
                        "currency": "USD",
                        "features": {
                          "advanced_analytics": true,
                          "api_access": true,
                          "priority_support": true,
                          "custom_branding": true,
                          "ai_features": true
                        },
                        "quotas": {
                          "apiCalls": 150000,
                          "storageGb": 100,
                          "emailSends": 15000,
                          "activeUsers": 15
                        },
                        "trialDays": 14,
                        "active": true,
                        "publicPlan": true,
                        "createdAt": "2024-01-15T10:30:00",
                        "updatedAt": "2024-01-15T10:30:00"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Validation Error",
                  value = """
                      {
                        "timestamp": "2024-01-15T10:30:00Z",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Validation failed: basePrice must be non-negative",
                        "path": "/api/v1/admin/billing/plans"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      ),
      @ApiResponse(
          responseCode = "409",
          description = "Conflict - Plan code already exists",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Conflict Error",
                  value = """
                      {
                        "timestamp": "2024-01-15T10:30:00Z",
                        "status": 409,
                        "error": "Conflict",
                        "message": "Plan code already exists: PRO_MONTHLY_V2",
                        "path": "/api/v1/admin/billing/plans"
                      }
                      """
              )
          )
      )
  })
  public ResponseEntity<SubscriptionPlanDto> createPlan(
      @Parameter(description = "Plan creation request", required = true)
      @Valid @RequestBody CreatePlanRequest request) {

    log.info("Admin creating subscription plan: {}", request.planCode());

    var plan = subscriptionPlanService.createPlan(
        request.planCode(),
        request.name(),
        request.description(),
        request.tier(),
        request.billingCycle(),
        request.basePrice(),
        request.currency(),
        request.features(),
        request.quotas(),
        request.trialDays(),
        request.publicPlan()
    );

    log.info("Successfully created plan: {} (ID: {})", plan.planCode(), plan.id());

    return ResponseEntity.status(HttpStatus.CREATED).body(plan);
  }

  /**
   * Updates an existing subscription plan.
   *
   * <p>This endpoint allows administrators to update plan details, features,
   * quotas, and pricing. Price changes only affect new subscriptions.
   *
   * @param id      plan identifier
   * @param request plan update request
   * @return updated subscription plan DTO
   */
  @PutMapping("/plans/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.plans.update", description = "Time taken to update subscription plan")
  @Operation(
      summary = "Update subscription plan",
      description = """
          Updates an existing subscription plan's details, features, quotas, and pricing.
          
          **Features:**
          - Update plan name and description
          - Modify feature flags
          - Adjust resource quotas
          - Change pricing (affects new subscriptions only)
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          - Plan must exist
          - Base price must be non-negative
          
          **Important Notes:**
          - Price changes only affect new subscriptions
          - Existing subscriptions maintain their original price
          - Feature and quota changes may affect existing subscriptions
          
          **Use Cases:**
          - Adjust pricing for market changes
          - Add or remove features
          - Increase or decrease quotas
          - Update plan descriptions
          
          **Example Request:**
          Updates a plan's pricing and quotas while maintaining other settings.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Subscription plan updated successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionPlanDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Plan not found"
      )
  })
  public ResponseEntity<SubscriptionPlanDto> updatePlan(
      @Parameter(description = "Plan ID", required = true, example = "1")
      @PathVariable Long id,
      @Parameter(description = "Plan update request", required = true)
      @Valid @RequestBody UpdatePlanRequest request) {

    log.info("Admin updating subscription plan: {}", id);

    var plan = subscriptionPlanService.updatePlan(
        id,
        request.name(),
        request.description(),
        request.features(),
        request.quotas(),
        request.basePrice(),
        request.currency()
    );

    log.info("Successfully updated plan: {} (ID: {})", plan.planCode(), plan.id());

    return ResponseEntity.ok(plan);
  }

  /**
   * Lists all subscriptions with filtering and pagination.
   *
   * <p>This endpoint allows administrators to view all subscriptions across
   * all tenants with optional filtering by status, plan, and tenant.
   *
   * @param status   optional status filter
   * @param planCode optional plan code filter
   * @param tenantId optional tenant ID filter
   * @param pageable pagination parameters
   * @return page of subscription DTOs
   */
  @GetMapping("/subscriptions")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.subscriptions.list", description = "Time taken to list subscriptions")
  @Operation(
      summary = "List all subscriptions",
      description = """
          Retrieves all subscriptions across all tenants with optional filtering and pagination.
          
          **Features:**
          - View all subscriptions across tenants
          - Filter by status (ACTIVE, TRIAL, CANCELED, etc.)
          - Filter by plan code
          - Filter by tenant ID
          - Paginated results with sorting
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          
          **Use Cases:**
          - Monitor subscription health across platform
          - Identify subscriptions by status
          - Find subscriptions for specific plans
          - Audit tenant subscriptions
          
          **Pagination:**
          - Default page size: 20
          - Maximum page size: 100
          - Sortable by any field (default: createdAt desc)
          
          **Example Response:**
          Returns a paginated list of subscriptions with complete details.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved subscriptions",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Page.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      )
  })
  public ResponseEntity<Page<SubscriptionDto>> listSubscriptions(
      @Parameter(description = "Filter by subscription status", example = "ACTIVE")
      @RequestParam(required = false) SubscriptionStatus status,
      @Parameter(description = "Filter by plan code", example = "PRO_MONTHLY")
      @RequestParam(required = false) String planCode,
      @Parameter(description = "Filter by tenant ID")
      @RequestParam(required = false) String tenantId,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

    log.info(
        "Admin listing subscriptions with filters - status: {}, planCode: {}, tenantId: {}",
        status,
        planCode,
        tenantId
    );

    var subscriptions = adminSubscriptionService.listSubscriptions(
        status,
        planCode,
        tenantId,
        pageable
    );

    log.info("Retrieved {} subscriptions (page {} of {})",
        subscriptions.getNumberOfElements(),
        subscriptions.getNumber() + 1,
        subscriptions.getTotalPages()
    );

    return ResponseEntity.ok(subscriptions);
  }

  /**
   * Retrieves detailed information about a specific subscription.
   *
   * <p>This endpoint allows administrators to view complete subscription
   * details including plan, status, billing information, and metadata.
   *
   * @param id subscription identifier
   * @return subscription DTO
   */
  @GetMapping("/subscriptions/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.subscriptions.get", description = "Time taken to get subscription")
  @Operation(
      summary = "Get subscription details",
      description = """
          Retrieves detailed information about a specific subscription.
          
          **Features:**
          - View complete subscription details
          - Access plan information
          - View billing history
          - Check subscription status and dates
          - Review metadata and custom fields
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          - Subscription must exist
          
          **Use Cases:**
          - Investigate subscription issues
          - Review subscription configuration
          - Audit subscription changes
          - Support customer inquiries
          
          **Example Response:**
          Returns complete subscription details including plan, dates, status, and metadata.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved subscription",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Subscription not found"
      )
  })
  public ResponseEntity<SubscriptionDto> getSubscription(
      @Parameter(description = "Subscription ID", required = true, example = "1")
      @PathVariable Long id) {

    log.info("Admin retrieving subscription: {}", id);

    var subscription = subscriptionApplicationService.getSubscription(id);

    log.info("Successfully retrieved subscription: {} for tenant: {}",
        subscription.id(),
        subscription.tenantId()
    );

    return ResponseEntity.ok(subscription);
  }

  /**
   * Force cancels a subscription immediately.
   *
   * <p>This endpoint allows administrators to immediately cancel a subscription,
   * bypassing normal cancellation rules. Use with caution.
   *
   * @param id      subscription identifier
   * @param request cancellation request with reason
   * @return canceled subscription DTO
   */
  @PostMapping("/subscriptions/{id}/cancel")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.subscriptions.cancel", description = "Time taken to cancel subscription")
  @Operation(
      summary = "Force cancel subscription",
      description = """
          Immediately cancels a subscription, bypassing normal cancellation rules.
          
          **Features:**
          - Immediate cancellation (no grace period)
          - Bypasses normal cancellation restrictions
          - Requires reason for audit trail
          - Publishes SubscriptionCanceled event
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          - Subscription must exist
          - Reason must be provided
          
          **Important Notes:**
          - This is a destructive operation
          - Customer loses access immediately
          - No refunds are automatically processed
          - Use for policy violations or fraud cases
          
          **Use Cases:**
          - Handle policy violations
          - Respond to fraud cases
          - Emergency service termination
          - Customer request escalations
          
          **Example Request:**
          Cancels a subscription with a reason for the audit trail.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Subscription canceled successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request - reason required"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Subscription not found"
      )
  })
  public ResponseEntity<SubscriptionDto> cancelSubscription(
      @Parameter(description = "Subscription ID", required = true, example = "1")
      @PathVariable Long id,
      @Parameter(description = "Cancellation request", required = true)
      @Valid @RequestBody AdminCancelSubscriptionRequest request) {

    log.info("Admin force canceling subscription: {}, reason: {}", id, request.reason());

    var subscription = adminSubscriptionService.forceCancelSubscription(id, request.reason());

    log.info("Successfully canceled subscription: {} for tenant: {}",
        subscription.id(),
        subscription.tenantId()
    );

    return ResponseEntity.ok(subscription);
  }

  /**
   * Extends the trial period for a subscription.
   *
   * <p>This endpoint allows administrators to extend trial periods to give
   * customers more time to evaluate the service.
   *
   * @param id      subscription identifier
   * @param request trial extension request
   * @return updated subscription DTO
   */
  @PostMapping("/subscriptions/{id}/extend-trial")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.subscriptions.extend.trial", description = "Time taken to extend trial")
  @Operation(
      summary = "Extend trial period",
      description = """
          Extends the trial period for a subscription by a specified number of days.
          
          **Features:**
          - Add additional trial days
          - Requires reason for audit trail
          - Updates trial end date
          - Maintains trial status
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          - Subscription must exist
          - Subscription must be in TRIAL status
          - Additional days must be positive
          - Reason must be provided
          
          **Use Cases:**
          - Give customers more evaluation time
          - Compensate for service issues during trial
          - Support sales negotiations
          - Handle customer requests
          
          **Example Request:**
          Extends a trial by 7 days with a reason for the audit trail.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Trial extended successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request - subscription not in trial or invalid days"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Subscription not found"
      )
  })
  public ResponseEntity<SubscriptionDto> extendTrial(
      @Parameter(description = "Subscription ID", required = true, example = "1")
      @PathVariable Long id,
      @Parameter(description = "Trial extension request", required = true)
      @Valid @RequestBody ExtendTrialRequest request) {

    log.info("Admin extending trial for subscription: {}, additionalDays: {}, reason: {}",
        id,
        request.additionalDays(),
        request.reason()
    );

    var subscription = subscriptionApplicationService.extendTrial(
        id,
        request.additionalDays(),
        request.reason()
    );

    log.info("Successfully extended trial for subscription: {}, new trial end: {}",
        subscription.id(),
        subscription.trialEnd()
    );

    return ResponseEntity.ok(subscription);
  }

  /**
   * Lists all invoices with filtering and pagination.
   *
   * <p>This endpoint allows administrators to view all invoices across
   * all tenants with optional filtering by status, tenant, and date range.
   *
   * @param status   optional status filter
   * @param tenantId optional tenant ID filter
   * @param pageable pagination parameters
   * @return page of invoice DTOs
   */
  @GetMapping("/invoices")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.invoices.list", description = "Time taken to list invoices")
  @Operation(
      summary = "List all invoices",
      description = """
          Retrieves all invoices across all tenants with optional filtering and pagination.
          
          **Features:**
          - View all invoices across tenants
          - Filter by status (DRAFT, OPEN, PAID, VOID, UNCOLLECTIBLE)
          - Filter by tenant ID
          - Paginated results with sorting
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          
          **Use Cases:**
          - Monitor invoice status across platform
          - Identify unpaid invoices
          - Audit billing operations
          - Generate financial reports
          
          **Pagination:**
          - Default page size: 20
          - Maximum page size: 100
          - Sortable by any field (default: createdAt desc)
          
          **Example Response:**
          Returns a paginated list of invoices with complete details including line items.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved invoices",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Page.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      )
  })
  public ResponseEntity<Page<com.iqscaffold.billingservice.invoice.InvoiceDto>> listInvoices(
      @Parameter(description = "Filter by invoice status", example = "OPEN")
      @RequestParam(required = false) com.iqscaffold.billingservice.invoice.InvoiceStatus status,
      @Parameter(description = "Filter by tenant ID")
      @RequestParam(required = false) String tenantId,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

    log.info(
        "Admin listing invoices with filters - status: {}, tenantId: {}",
        status,
        tenantId
    );

    var invoices = billingAnalyticsService.listInvoices(
        status,
        tenantId,
        pageable
    );

    log.info("Retrieved {} invoices (page {} of {})",
        invoices.getNumberOfElements(),
        invoices.getNumber() + 1,
        invoices.getTotalPages()
    );

    return ResponseEntity.ok(invoices);
  }

  /**
   * Voids an invoice.
   *
   * <p>This endpoint allows administrators to void an unpaid invoice,
   * preventing payment and marking it as canceled.
   *
   * @param id      invoice identifier
   * @param request void request with reason
   * @return voided invoice DTO
   */
  @PostMapping("/invoices/{id}/void")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.invoices.void", description = "Time taken to void invoice")
  @Operation(
      summary = "Void invoice",
      description = """
          Voids an unpaid invoice, preventing payment and marking it as canceled.
          
          **Features:**
          - Void unpaid invoices (DRAFT or OPEN status)
          - Requires reason for audit trail
          - Publishes InvoiceVoided event
          - Triggers customer notification
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          - Invoice must exist
          - Invoice must be unpaid (DRAFT or OPEN status)
          - Reason must be provided
          
          **Important Notes:**
          - Only unpaid invoices can be voided
          - Paid invoices cannot be voided (use refund instead)
          - Voiding is permanent and cannot be undone
          - Customer will be notified of voided invoice
          
          **Use Cases:**
          - Cancel incorrect invoices
          - Handle billing disputes
          - Correct billing errors
          - Process customer refund requests
          
          **Example Request:**
          Voids an invoice with a reason for the audit trail.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Invoice voided successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = com.iqscaffold.billingservice.invoice.InvoiceDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request - invoice already paid or reason missing"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Invoice not found"
      )
  })
  public ResponseEntity<com.iqscaffold.billingservice.invoice.InvoiceDto> voidInvoice(
      @Parameter(description = "Invoice ID", required = true, example = "1")
      @PathVariable Long id,
      @Parameter(description = "Void request with reason", required = true)
      @Valid @RequestBody VoidInvoiceRequest request) {

    log.info("Admin voiding invoice: {}, reason: {}", id, request.reason());

    var invoice = invoiceApplicationService.voidInvoice(id, request.reason());

    log.info("Successfully voided invoice: {} for tenant: {}",
        invoice.id(),
        invoice.tenantId()
    );

    return ResponseEntity.ok(invoice);
  }

  /**
   * Retrieves Monthly Recurring Revenue (MRR) analytics.
   *
   * <p>This endpoint provides MRR metrics including current MRR, growth rate,
   * and breakdown by plan tier.
   *
   * @return MRR report DTO
   */
  @GetMapping("/analytics/mrr")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.analytics.mrr", description = "Time taken to calculate MRR")
  @Operation(
      summary = "Get Monthly Recurring Revenue (MRR)",
      description = """
          Retrieves Monthly Recurring Revenue (MRR) analytics and metrics.
          
          **Features:**
          - Current MRR calculation
          - MRR growth rate (month-over-month)
          - MRR breakdown by plan tier (FREE, PRO, ENTERPRISE)
          - New MRR from new subscriptions
          - Churned MRR from canceled subscriptions
          - Expansion MRR from upgrades
          - Contraction MRR from downgrades
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          
          **Calculation:**
          - MRR = Sum of all active monthly subscription values
          - Yearly subscriptions normalized to monthly (price / 12)
          - Lifetime subscriptions excluded from MRR
          - Only ACTIVE and TRIAL subscriptions counted
          
          **Use Cases:**
          - Monitor revenue health
          - Track growth trends
          - Identify revenue drivers
          - Financial reporting and forecasting
          
          **Example Response:**
          Returns comprehensive MRR metrics with historical comparison.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved MRR analytics",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = com.iqscaffold.billingservice.analytics.RevenueReportDto.class),
              examples = @ExampleObject(
                  name = "MRR Report",
                  value = """
                      {
                        "currentMrr": 125000.00,
                        "previousMrr": 118000.00,
                        "growthRate": 5.93,
                        "newMrr": 12000.00,
                        "churnedMrr": 3500.00,
                        "expansionMrr": 4500.00,
                        "contractionMrr": 1000.00,
                        "mrrByTier": {
                          "FREE": 0.00,
                          "PRO": 85000.00,
                          "ENTERPRISE": 40000.00
                        },
                        "calculatedAt": "2024-01-15T10:30:00Z"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      )
  })
  public ResponseEntity<com.iqscaffold.billingservice.analytics.RevenueReportDto> getMrrAnalytics() {
    log.info("Admin retrieving MRR analytics");

    var mrrReport = billingAnalyticsService.calculateMrr();

    log.info("Successfully calculated MRR: {}", mrrReport);

    return ResponseEntity.ok(mrrReport);
  }

  /**
   * Retrieves churn rate analytics.
   *
   * <p>This endpoint provides churn metrics including churn rate, churned
   * subscriptions count, and churn reasons breakdown.
   *
   * @return churn analysis DTO
   */
  @GetMapping("/analytics/churn")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Timed(value = "admin.billing.analytics.churn", description = "Time taken to calculate churn")
  @Operation(
      summary = "Get churn rate analytics",
      description = """
          Retrieves churn rate analytics and customer retention metrics.
          
          **Features:**
          - Current month churn rate
          - Churned subscriptions count
          - Churn reasons breakdown
          - Churn trend over time
          - Revenue churn vs customer churn
          - Churn by plan tier
          
          **Requirements:**
          - ADMIN or SUPER_ADMIN authority required
          
          **Calculation:**
          - Churn Rate = (Canceled Subscriptions / Total Active Subscriptions at Start) × 100
          - Calculated for current month
          - Includes voluntary and involuntary churn
          - Revenue churn weighted by subscription value
          
          **Use Cases:**
          - Monitor customer retention
          - Identify churn patterns
          - Evaluate product-market fit
          - Inform retention strategies
          
          **Example Response:**
          Returns comprehensive churn metrics with historical trends.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved churn analytics",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = com.iqscaffold.billingservice.analytics.ChurnAnalysisDto.class),
              examples = @ExampleObject(
                  name = "Churn Analysis",
                  value = """
                      {
                        "churnRate": 3.5,
                        "churnedCount": 42,
                        "totalActiveStart": 1200,
                        "revenueChurnRate": 4.2,
                        "churnedRevenue": 5250.00,
                        "churnByTier": {
                          "FREE": 15,
                          "PRO": 22,
                          "ENTERPRISE": 5
                        },
                        "churnReasons": {
                          "price": 18,
                          "features": 12,
                          "support": 5,
                          "other": 7
                        },
                        "calculatedAt": "2024-01-15T10:30:00Z",
                        "periodStart": "2024-01-01T00:00:00Z",
                        "periodEnd": "2024-01-31T23:59:59Z"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - JWT token missing or invalid"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - ADMIN authority required"
      )
  })
  public ResponseEntity<com.iqscaffold.billingservice.analytics.ChurnAnalysisDto> getChurnAnalytics() {
    log.info("Admin retrieving churn analytics");

    var churnAnalysis = billingAnalyticsService.calculateChurn();

    log.info("Successfully calculated churn rate: {}%", churnAnalysis.churnRate());

    return ResponseEntity.ok(churnAnalysis);
  }
}
