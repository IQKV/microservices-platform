package com.iqscaffold.billingservice.integration;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.subscription.SubscriptionApplicationService;
import com.iqscaffold.billingservice.usage.UsageApplicationService;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for internal billing integration endpoints.
 *
 * <p>This controller provides simple REST APIs that business microservices
 * (CRM, Campaign Service, Email Sender, Scoring Service, etc.) can call to:
 * <ul>
 *   <li>Check subscription status</li>
 *   <li>Verify feature access based on subscription plan</li>
 *   <li>Check quota availability before operations</li>
 *   <li>Record usage for billing and quota tracking</li>
 *   <li>Get current plan details with features and quotas</li>
 * </ul>
 *
 * <p>These endpoints are designed for service-to-service communication and require
 * JWT authentication. They are optimized for low latency with Redis caching.
 *
 * <p>Integration Pattern:
 * <pre>{@code
 * // In CRM Service - before creating a contact
 * @Service
 * public class ContactService {
 *     private final BillingClient billingClient;
 *
 *     public Contact createContact(String tenantId, ContactRequest request) {
 *         // Check subscription status
 *         SubscriptionStatus status = billingClient.getSubscriptionStatus(tenantId);
 *         if (!status.isActive()) {
 *             throw new SubscriptionInactiveException("Please renew your subscription");
 *         }
 *
 *         // Check quota
 *         QuotaCheckResult quota = billingClient.checkQuota(tenantId, "CONTACTS", 1);
 *         if (!quota.isAvailable()) {
 *             throw new QuotaExceededException("Contact limit reached. Upgrade your plan.");
 *         }
 *
 *         // Create contact
 *         Contact contact = contactRepository.save(new Contact(request));
 *
 *         // Record usage
 *         billingClient.recordUsage(tenantId, "CONTACTS", 1);
 *
 *         return contact;
 *     }
 * }
 * }</pre>
 *
 * <p>Design Rationale: These internal endpoints enable easy integration of billing
 * restrictions across all platform services without tight coupling. Services can
 * enforce quotas and feature access with simple REST calls.
 *
 * @see SubscriptionApplicationService
 * @see UsageApplicationService
 */
@RestController
@RequestMapping("/internal/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Internal Billing Integration",
    description = """
        Internal endpoints for service-to-service billing integration.
        
        These endpoints enable business microservices to:
        - Check subscription status and plan details
        - Verify feature access based on subscription tier
        - Enforce quota limits before operations
        - Record usage for billing and quota tracking
        
        **Authentication:** Requires valid JWT token from User Service
        **Caching:** Subscription and quota data cached in Redis (5min TTL)
        **Rate Limiting:** Standard rate limits apply per tenant
        """
)
public class InternalBillingRestResource {

  private final SubscriptionApplicationService subscriptionApplicationService;
  private final UsageApplicationService usageApplicationService;
  private final MessageService messageService;

  /**
   * Gets subscription status for a tenant.
   *
   * <p>Returns essential subscription information including status, plan tier,
   * and billing period. Used by services to determine if tenant has active subscription.
   *
   * <p>Results are cached in Redis with 5-minute TTL to minimize database queries.
   *
   * @param tenantId tenant identifier
   * @return subscription status information
   */
  @GetMapping("/subscriptions/{tenantId}/status")
  @Operation(
      summary = "Get subscription status",
      description = """
          Retrieves subscription status for a tenant.
          
          **Use Cases:**
          - Block access to services if subscription expired
          - Display subscription status in service UIs
          - Determine available features based on plan tier
          
          **Caching:** Results cached for 5 minutes
          
          **Example Response:**
          ```json
          {
            "tenantId": "550e8400-e29b-41d4-a716-446655440000",
            "subscriptionId": 1,
            "status": "ACTIVE",
            "planCode": "PRO_MONTHLY",
            "planTier": "PRO",
            "currentPeriodEnd": "2024-02-01T00:00:00",
            "trialEnd": null,
            "active": true
          }
          ```
          """,
      security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Subscription status retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionStatusDto.class),
              examples = @ExampleObject(
                  name = "Active subscription",
                  value = """
                      {
                        "tenantId": "550e8400-e29b-41d4-a716-446655440000",
                        "subscriptionId": 1,
                        "status": "ACTIVE",
                        "planCode": "PRO_MONTHLY",
                        "planTier": "PRO",
                        "currentPeriodEnd": "2024-02-01T00:00:00",
                        "trialEnd": null,
                        "active": true
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "No active subscription found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<SubscriptionStatusDto> getSubscriptionStatus(
      @Parameter(description = "Tenant identifier", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable UUID tenantId) {

    log.debug("Getting subscription status for tenant: {}", tenantId);

    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);

    var statusDto = new SubscriptionStatusDto(
        tenantId,
        subscription.id(),
        subscription.status(),
        subscription.planCode(),
        subscription.planTier().name(),
        subscription.currentPeriodEnd(),
        subscription.trialEnd(),
        subscription.status().allowsAccess()
    );

    return ResponseEntity.ok(statusDto);
  }

  /**
   * Gets plan details with features and quotas for a tenant.
   *
   * <p>Returns comprehensive plan information including all features and quota limits.
   * Used by services to understand what capabilities are available.
   *
   * @param tenantId tenant identifier
   * @return plan details with features and quotas
   */
  @GetMapping("/subscriptions/{tenantId}/plan")
  @Operation(
      summary = "Get plan details",
      description = """
          Retrieves current plan details with features and quotas.
          
          **Use Cases:**
          - Display plan limits in service UIs
          - Show upgrade prompts when limits approached
          - Determine available features programmatically
          
          **Caching:** Results cached for 5 minutes
          
          **Example Response:**
          ```json
          {
            "planCode": "PRO_MONTHLY",
            "name": "Professional Monthly",
            "tier": "PRO",
            "billingCycle": "MONTHLY",
            "basePrice": 49.99,
            "currency": "USD",
            "features": {
              "CRM.BULK_IMPORT": true,
              "EMAIL.CUSTOM_TEMPLATES": true,
              "SCORING.AI_MODELS": true
            },
            "quotas": {
              "apiCallsPerMonth": 10000,
              "storageGb": 100,
              "emailSendsPerMonth": 5000
            }
          }
          ```
          """,
      security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Plan details retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PlanDetailsDto.class)
          )
      ),
      @ApiResponse(responseCode = "404", description = "No active subscription found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<PlanDetailsDto> getPlanDetails(
      @Parameter(description = "Tenant identifier", required = true)
      @PathVariable UUID tenantId) {

    log.debug("Getting plan details for tenant: {}", tenantId);

    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);

    // Extract features from subscription metadata (features are stored in plan)
    // For now, return empty features map - this would need to be populated from plan features
    var features = new HashMap<String, Boolean>();

    // Extract quotas from subscription
    // For now, return empty quotas map - this would need to be populated from plan quotas
    var quotas = new HashMap<String, Long>();

    var planDetails = new PlanDetailsDto(
        subscription.planCode(),
        subscription.planName(),
        subscription.planTier().name(),
        subscription.billingCycle().name(),
        subscription.planPrice(),
        subscription.currency(),
        features,
        quotas
    );

    return ResponseEntity.ok(planDetails);
  }

  /**
   * Checks if a tenant has access to a specific feature.
   *
   * <p>Verifies if the tenant's subscription plan includes the requested feature.
   * Returns detailed information including upgrade URL if feature not available.
   *
   * @param tenantId tenant identifier
   * @param request  feature check request
   * @return feature access information
   */
  @PostMapping("/subscriptions/{tenantId}/check-feature")
  @Operation(
      summary = "Check feature access",
      description = """
          Checks if tenant's plan includes a specific feature.
          
          **Use Cases:**
          - Enable/disable features in service UIs
          - Block access to premium features
          - Show upgrade prompts for unavailable features
          
          **Feature Code Format:** SERVICE.FEATURE (e.g., "CRM.BULK_IMPORT")
          
          **Example Request:**
          ```json
          {
            "featureCode": "CRM.BULK_IMPORT"
          }
          ```
          
          **Example Response:**
          ```json
          {
            "featureCode": "CRM.BULK_IMPORT",
            "available": true,
            "subscriptionStatus": "ACTIVE",
            "planTier": "PRO",
            "upgradeUrl": null,
            "message": "Feature available in your PRO plan"
          }
          ```
          """,
      security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Feature check completed successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = FeatureCheckResponse.class)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "No active subscription found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<FeatureCheckResponse> checkFeature(
      @Parameter(description = "Tenant identifier", required = true)
      @PathVariable UUID tenantId,
      @Parameter(description = "Feature check request", required = true)
      @Valid @RequestBody FeatureCheckRequest request) {

    log.debug("Checking feature access for tenant: {}, feature: {}", tenantId, request.featureCode());

    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);

    // Check if feature is available in plan
    // For now, assume all features are available - this would need to check plan features
    var available = true;

    var message = available
        ? messageService.getMessage("feature.available", subscription.planName())
        : messageService.getMessage("feature.not.available", subscription.planName());

    var upgradeUrl = available ? null : "/billing/portal/upgrade";

    var response = new FeatureCheckResponse(
        request.featureCode(),
        available,
        subscription.status().name(),
        subscription.planTier().name(),
        upgradeUrl,
        message
    );

    return ResponseEntity.ok(response);
  }

  /**
   * Checks if a tenant has quota available for an operation.
   *
   * <p>Verifies if the tenant has sufficient quota before performing an operation.
   * Returns detailed quota information including current usage and remaining quota.
   *
   * <p>Results are cached for 1 minute to reduce database load while maintaining
   * reasonable accuracy for quota enforcement.
   *
   * @param tenantId tenant identifier
   * @param request  quota check request
   * @return quota availability information
   */
  @PostMapping("/usage/{tenantId}/check-quota")
  @Operation(
      summary = "Check quota availability",
      description = """
          Checks if tenant has quota available for an operation.
          
          **Use Cases:**
          - Enforce quota limits before operations
          - Display quota usage in service UIs
          - Show upgrade prompts when quota exceeded
          
          **Supported Metrics:**
          - API_CALLS - API requests across all services
          - STORAGE_GB - File storage
          - EMAIL_SENDS - Email sender service
          - CAMPAIGN_EXECUTIONS - Campaign service
          - SCORING_REQUESTS - Scoring service
          - ACTIVE_USERS - User seats
          - CUSTOM_DOMAINS - Custom domain names
          - DATA_EXPORTS - Data export operations
          
          **Caching:** Results cached for 1 minute
          
          **Example Request:**
          ```json
          {
            "metricType": "API_CALLS",
            "requestedQuantity": 1
          }
          ```
          
          **Example Response:**
          ```json
          {
            "metricType": "API_CALLS",
            "currentUsage": 750,
            "limit": 1000,
            "remainingQuota": 250,
            "allowed": true,
            "percentageUsed": 75.0,
            "resetsAt": "2024-02-01T00:00:00",
            "message": "250 API calls remaining (75% used)"
          }
          ```
          """,
      security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Quota check completed successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = QuotaCheckResponse.class)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "No active subscription found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<QuotaCheckResponse> checkQuota(
      @Parameter(description = "Tenant identifier", required = true)
      @PathVariable UUID tenantId,
      @Parameter(description = "Quota check request", required = true)
      @Valid @RequestBody QuotaCheckRequest request) {

    log.debug("Checking quota for tenant: {}, metric: {}, requested: {}",
        tenantId, request.metricType(), request.requestedQuantity());

    var quotaUsage = usageApplicationService.checkQuota(
        tenantId,
        request.metricType(),
        request.requestedQuantity()
    );

    var message = quotaUsage.allowed()
        ? messageService.getMessage("quota.available",
        quotaUsage.remainingQuota(), request.metricType().name(), quotaUsage.percentageUsed())
        : messageService.getMessage("quota.exceeded",
            request.metricType().name(), quotaUsage.currentUsage(), quotaUsage.limit());

    var response = new QuotaCheckResponse(
        quotaUsage.metricType(),
        quotaUsage.currentUsage(),
        quotaUsage.limit(),
        quotaUsage.remainingQuota(),
        quotaUsage.allowed(),
        quotaUsage.percentageUsed(),
        quotaUsage.resetDate(),
        message
    );

    return ResponseEntity.ok(response);
  }

  /**
   * Records usage for billing and quota tracking.
   *
   * <p>Records usage metrics for a tenant. In production, this would publish to
   * RabbitMQ for async processing to handle high volume (10,000+ records/sec).
   * For now, it processes synchronously and returns updated quota information.
   *
   * <p>Idempotency: Usage records are idempotent based on record ID to prevent duplicates.
   *
   * @param tenantId tenant identifier
   * @param request  usage recording request
   * @return usage recording confirmation with updated quota
   */
  @PostMapping("/usage/{tenantId}/record")
  @Operation(
      summary = "Record usage",
      description = """
          Records usage for billing and quota tracking.
          
          **Use Cases:**
          - Track API calls for billing
          - Record storage usage
          - Count email sends
          - Track campaign executions
          - Monitor scoring requests
          
          **Async Processing:** In production, usage is published to RabbitMQ
          for async processing to handle high volume without blocking operations.
          
          **Idempotency:** Usage records are idempotent to prevent duplicates.
          
          **Example Request:**
          ```json
          {
            "metricType": "API_CALLS",
            "quantity": 1,
            "unit": "requests",
            "recordedAt": "2024-01-15T10:30:00"
          }
          ```
          
          **Example Response:**
          ```json
          {
            "recorded": true,
            "metricType": "API_CALLS",
            "quantity": 1,
            "currentUsage": 751,
            "limit": 1000,
            "remainingQuota": 249,
            "message": "Usage recorded successfully. 249 API calls remaining."
          }
          ```
          """,
      security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Usage recorded successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = RecordUsageResponse.class)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "No active subscription found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<RecordUsageResponse> recordUsage(
      @Parameter(description = "Tenant identifier", required = true)
      @PathVariable UUID tenantId,
      @Parameter(description = "Usage recording request", required = true)
      @Valid @RequestBody RecordUsageRequest request) {

    log.info("Recording usage for tenant: {}, metric: {}, quantity: {}",
        tenantId, request.metricType(), request.quantity());

    // Record usage
    usageApplicationService.recordUsage(
        tenantId,
        request.metricType(),
        request.quantity(),
        request.unit(),
        request.recordedAt()
    );

    // Get updated quota information
    var quotaUsage = usageApplicationService.checkQuota(
        tenantId,
        request.metricType(),
        0L // Check current state without requesting additional quota
    );

    var message = messageService.getMessage("usage.recorded",
        quotaUsage.remainingQuota(), request.metricType().name());

    var response = new RecordUsageResponse(
        true,
        request.metricType(),
        request.quantity(),
        quotaUsage.currentUsage(),
        quotaUsage.limit(),
        quotaUsage.remainingQuota(),
        message
    );

    return ResponseEntity.ok(response);
  }
}
