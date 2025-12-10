package com.iqscaffold.billingservice.plan;

import java.util.List;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for public subscription plan endpoints.
 *
 * <p>Provides public APIs for retrieving subscription plan information.
 * These endpoints do not require authentication and are used by:
 * <ul>
 *   <li>Public pricing pages</li>
 *   <li>Plan comparison tools</li>
 *   <li>Customer self-service portals</li>
 *   <li>Integration with external systems</li>
 * </ul>
 *
 * <p>All responses are cached with 1-hour TTL to optimize performance.
 */
@RestController
@RequestMapping("/api/v1/billing/plans")
@Tag(name = "Subscription Plans", description = "Public subscription plan APIs")
public class SubscriptionPlanRestResource {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanRestResource.class);

  private final SubscriptionPlanService subscriptionPlanService;

  public SubscriptionPlanRestResource(SubscriptionPlanService subscriptionPlanService) {
    this.subscriptionPlanService = subscriptionPlanService;
  }

  /**
   * Retrieves all active public subscription plans.
   *
   * <p>Returns a list of publicly available subscription plans, optionally
   * filtered by billing cycle. Plans are ordered by tier (FREE, PRO, ENTERPRISE)
   * and price within each tier.
   *
   * @param billingCycle optional filter by billing cycle (MONTHLY, YEARLY, LIFETIME)
   * @return list of subscription plan DTOs
   */
  @GetMapping
  @Timed(value = "billing.plans.list", description = "Time taken to list subscription plans")
  @Operation(
      summary = "List all public subscription plans",
      description = """
          Retrieves all active public subscription plans with optional filtering by billing cycle.
          
          **Features:**
          - Returns only active and publicly visible plans
          - Optional filtering by billing cycle (MONTHLY, YEARLY, LIFETIME)
          - Plans ordered by tier and price
          - Results cached with 1-hour TTL for optimal performance
          - No authentication required
          
          **Use Cases:**
          - Display pricing page on public website
          - Show plan comparison tables
          - Populate plan selection dropdowns
          - Integration with external billing systems
          
          **Example Response:**
          Returns an array of subscription plans with complete details including:
          - Plan identification (id, code, name)
          - Pricing information (basePrice, currency, billingCycle)
          - Feature flags and capabilities
          - Resource quotas and limits
          - Trial period information
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved subscription plans",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionPlanDto.class),
              examples = @ExampleObject(
                  name = "Subscription Plans Response",
                  value = """
                      [
                        {
                          "id": 1,
                          "planCode": "FREE",
                          "name": "Free Plan",
                          "description": "Perfect for individuals getting started",
                          "tier": "FREE",
                          "billingCycle": "MONTHLY",
                          "basePrice": 0.00,
                          "currency": "USD",
                          "features": {
                            "basic_analytics": true,
                            "api_access": false,
                            "priority_support": false,
                            "custom_branding": false
                          },
                          "quotas": {
                            "apiCalls": 1000,
                            "storageGb": 1,
                            "emailSends": 100,
                            "activeUsers": 1
                          },
                          "trialDays": 0,
                          "active": true,
                          "publicPlan": true,
                          "createdAt": "2024-01-01T00:00:00",
                          "updatedAt": "2024-01-01T00:00:00"
                        },
                        {
                          "id": 2,
                          "planCode": "PRO_MONTHLY",
                          "name": "Professional Monthly",
                          "description": "Perfect for growing teams with advanced features",
                          "tier": "PRO",
                          "billingCycle": "MONTHLY",
                          "basePrice": 49.99,
                          "currency": "USD",
                          "features": {
                            "advanced_analytics": true,
                            "api_access": true,
                            "priority_support": true,
                            "custom_branding": true,
                            "sso": false
                          },
                          "quotas": {
                            "apiCalls": 100000,
                            "storageGb": 50,
                            "emailSends": 10000,
                            "activeUsers": 10
                          },
                          "trialDays": 14,
                          "active": true,
                          "publicPlan": true,
                          "createdAt": "2024-01-01T00:00:00",
                          "updatedAt": "2024-01-15T10:30:00"
                        },
                        {
                          "id": 3,
                          "planCode": "PRO_YEARLY",
                          "name": "Professional Yearly",
                          "description": "Annual billing with 20% discount",
                          "tier": "PRO",
                          "billingCycle": "YEARLY",
                          "basePrice": 479.99,
                          "currency": "USD",
                          "features": {
                            "advanced_analytics": true,
                            "api_access": true,
                            "priority_support": true,
                            "custom_branding": true,
                            "sso": false
                          },
                          "quotas": {
                            "apiCalls": 100000,
                            "storageGb": 50,
                            "emailSends": 10000,
                            "activeUsers": 10
                          },
                          "trialDays": 14,
                          "active": true,
                          "publicPlan": true,
                          "createdAt": "2024-01-01T00:00:00",
                          "updatedAt": "2024-01-01T00:00:00"
                        }
                      ]
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid billing cycle parameter",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  name = "Invalid Billing Cycle",
                  value = """
                      {
                        "timestamp": "2024-01-15T10:30:00Z",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Invalid billing cycle. Allowed values: MONTHLY, YEARLY, LIFETIME",
                        "path": "/api/v1/billing/plans"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "timestamp": "2024-01-15T10:30:00Z",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "An unexpected error occurred while retrieving subscription plans",
                        "path": "/api/v1/billing/plans"
                      }
                      """
              )
          )
      )
  })
  public ResponseEntity<List<SubscriptionPlanDto>> getPlans(
      @Parameter(
          description = "Filter plans by billing cycle",
          example = "MONTHLY",
          schema = @Schema(allowableValues = {"MONTHLY", "YEARLY", "LIFETIME"})
      )
      @RequestParam(required = false) BillingCycle billingCycle) {

    log.info("Retrieving public subscription plans with billing cycle filter: {}", billingCycle);

    List<SubscriptionPlanDto> plans;

    if (billingCycle != null) {
      // Filter by billing cycle
      plans = subscriptionPlanService.getPlansByBillingCycle(billingCycle);
      log.info("Retrieved {} plans with billing cycle: {}", plans.size(), billingCycle);
    } else {
      // Return all public plans
      plans = subscriptionPlanService.getPublicPlans();
      log.info("Retrieved {} public plans", plans.size());
    }

    return ResponseEntity.ok(plans);
  }

  /**
   * Retrieves a specific subscription plan by its unique plan code.
   *
   * <p>Returns detailed information about a single subscription plan including
   * pricing, features, quotas, and trial period information.
   *
   * @param code unique plan identifier (e.g., "PRO_MONTHLY", "ENTERPRISE_YEARLY")
   * @return subscription plan DTO
   */
  @GetMapping("/{code}")
  @Timed(value = "billing.plans.get", description = "Time taken to retrieve a subscription plan")
  @Operation(
      summary = "Get subscription plan by code",
      description = """
          Retrieves detailed information about a specific subscription plan using its unique plan code.
          
          **Features:**
          - Returns complete plan details including pricing, features, and quotas
          - Results cached with 1-hour TTL for optimal performance
          - No authentication required
          - Returns 404 if plan not found or not publicly visible
          
          **Use Cases:**
          - Display detailed plan information on pricing page
          - Show plan details during checkout process
          - Validate plan selection before subscription creation
          - Integration with external systems
          
          **Plan Code Format:**
          Plan codes typically follow the pattern: {TIER}_{BILLING_CYCLE}
          - Examples: "FREE", "PRO_MONTHLY", "PRO_YEARLY", "ENTERPRISE_MONTHLY"
          
          **Example Response:**
          Returns a single subscription plan with complete details including:
          - Plan identification (id, code, name, description)
          - Pricing information (basePrice, currency, billingCycle)
          - Feature flags and capabilities (advanced_analytics, api_access, etc.)
          - Resource quotas and limits (apiCalls, storageGb, emailSends, etc.)
          - Trial period information (trialDays)
          - Plan status (active, publicPlan)
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Successfully retrieved subscription plan",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionPlanDto.class),
              examples = @ExampleObject(
                  name = "Subscription Plan Response",
                  value = """
                      {
                        "id": 2,
                        "planCode": "PRO_MONTHLY",
                        "name": "Professional Monthly",
                        "description": "Perfect for growing teams with advanced features and priority support",
                        "tier": "PRO",
                        "billingCycle": "MONTHLY",
                        "basePrice": 49.99,
                        "currency": "USD",
                        "features": {
                          "advanced_analytics": true,
                          "custom_branding": true,
                          "api_access": true,
                          "priority_support": true,
                          "sso": false,
                          "white_label": false,
                          "dedicated_account_manager": false
                        },
                        "quotas": {
                          "apiCalls": 100000,
                          "storageGb": 50,
                          "emailSends": 10000,
                          "campaignExecutions": 1000,
                          "scoringRequests": 5000,
                          "activeUsers": 10,
                          "customDomains": 3,
                          "dataExports": 100
                        },
                        "trialDays": 14,
                        "active": true,
                        "publicPlan": true,
                        "createdAt": "2024-01-01T00:00:00",
                        "updatedAt": "2024-01-15T10:30:00"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Subscription plan not found or not publicly visible",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  name = "Plan Not Found",
                  value = """
                      {
                        "timestamp": "2024-01-15T10:30:00Z",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Subscription plan not found: INVALID_PLAN_CODE",
                        "path": "/api/v1/billing/plans/INVALID_PLAN_CODE"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "timestamp": "2024-01-15T10:30:00Z",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "An unexpected error occurred while retrieving the subscription plan",
                        "path": "/api/v1/billing/plans/PRO_MONTHLY"
                      }
                      """
              )
          )
      )
  })
  public ResponseEntity<SubscriptionPlanDto> getPlanByCode(
      @Parameter(
          description = "Unique plan code identifier",
          example = "PRO_MONTHLY",
          required = true
      )
      @PathVariable String code) {

    log.info("Retrieving subscription plan by code: {}", code);

    var plan = subscriptionPlanService.getPlanByCode(code);

    log.info("Successfully retrieved plan: {} (ID: {})", plan.planCode(), plan.id());

    return ResponseEntity.ok(plan);
  }

  /**
   * Simple error response record for OpenAPI documentation.
   * Used in @ApiResponse examples to show error response structure.
   */
  @Schema(description = "Standard error response")
  private record ErrorResponse(
      @Schema(description = "Error timestamp", example = "2024-01-15T10:30:00Z")
      String timestamp,

      @Schema(description = "HTTP status code", example = "404")
      int status,

      @Schema(description = "Error type", example = "Not Found")
      String error,

      @Schema(description = "Error message", example = "Subscription plan not found")
      String message,

      @Schema(description = "Request path", example = "/api/v1/billing/plans/INVALID_CODE")
      String path
  ) {
  }
}
