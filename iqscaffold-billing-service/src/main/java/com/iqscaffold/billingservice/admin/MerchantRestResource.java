package com.iqscaffold.billingservice.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/billing/merchants")
@Tag(name = "Merchant Onboarding", description = "Admin operations for Merchant Stripe Connect")
@SecurityRequirement(name = "bearerAuth")
public class MerchantRestResource {

  private final MerchantOnboardingService onboardingService;

  public MerchantRestResource(MerchantOnboardingService onboardingService) {
    this.onboardingService = onboardingService;
  }

  public record OnboardRequest(String refreshUrl, String returnUrl) {
  }

  public record OnboardResponse(String accountLink) {
  }

  @Operation(summary = "Initiate Merchant Onboarding", description = "Creates a Stripe Connect account if needed and returns an Account Link URL")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Onboarding link generated"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden (Admin only)")
  })
  @PostMapping("/onboard")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<OnboardResponse> initiateOnboarding(@RequestBody OnboardRequest request) {
    String url = onboardingService.initiateOnboarding(request.refreshUrl, request.returnUrl);
    return ResponseEntity.ok(new OnboardResponse(url));
  }

  @Operation(summary = "Get Merchant Status", description = "Retrieves the current Stripe Connect status for the merchant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Status retrieved"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden (Admin only)")
  })
  @GetMapping("/status")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'USER')")
  public ResponseEntity<MerchantStripeConfig> getMerchantStatus() {
    return ResponseEntity.ok(onboardingService.getMerchantStatus().orElse(null));
  }
}
