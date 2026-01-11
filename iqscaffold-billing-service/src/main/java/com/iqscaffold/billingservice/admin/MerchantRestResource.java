package com.iqscaffold.billingservice.admin;

import jakarta.validation.Valid;

import com.iqscaffold.billingservice.admin.dto.MerchantStatusResponse;
import com.iqscaffold.billingservice.admin.dto.OnboardingLinkResponse;
import com.iqscaffold.billingservice.admin.dto.OnboardingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/billing/merchants")
@Tag(name = "Merchant Onboarding", description = "Admin operations for Merchant Stripe Connect with organization support")
@SecurityRequirement(name = "bearerAuth")
public class MerchantRestResource {

  private final MerchantOnboardingService onboardingService;

  public MerchantRestResource(final MerchantOnboardingService onboardingService) {
    this.onboardingService = onboardingService;
  }

  @Operation(
      summary = "Initiate Merchant Onboarding",
      description = "Creates a Stripe Connect account for an organization and returns an Account Link URL. " +
                    "The organization must exist in the user service and belong to the current tenant."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Onboarding link generated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request or organization already onboarded"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires BILLING_ADMIN, TENANT_OWNER, or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Organization not found")
  })
  @PostMapping("/onboard")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  public ResponseEntity<OnboardingLinkResponse> initiateOnboarding(
      @Valid @RequestBody OnboardingRequest request) {
    var response = onboardingService.initiateOnboarding(
        request.organizationId(),
        request.gatewayProvider(),
        request.refreshUrl(),
        request.returnUrl()
    );
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get Merchant Status by Organization",
      description = "Retrieves the current Stripe Connect status for an organization's merchant account."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires BILLING_ADMIN, FINANCE_VIEWER, TENANT_OWNER, or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Merchant configuration not found")
  })
  @GetMapping("/status/{organizationId}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<MerchantStatusResponse> getMerchantStatus(
      @Parameter(description = "Organization ID", required = true)
      @PathVariable Long organizationId) {
    
    var config = onboardingService.getMerchantStatusByOrganization(organizationId)
        .orElse(null);

    if (config == null) {
      return ResponseEntity.notFound().build();
    }

    var response = new MerchantStatusResponse(
        config.getGatewayAccountId(),
        config.getOrganizationId(),
        config.getTenantId(),
        config.isChargesEnabled(),
        config.isPayoutsEnabled(),
        config.getApplicationFeePercent()
    );

    return ResponseEntity.ok(response);
  }
}
