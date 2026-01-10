package com.iqscaffold.billingservice.payout;

import com.iqscaffold.billingservice.payout.dto.PayoutDtos;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/payouts")
@Tag(name = "Payouts", description = "Operations for viewing payout information")
@SecurityRequirement(name = "bearerAuth")
public class PayoutRestResource {

  private final PayoutService payoutService;

  public PayoutRestResource(final PayoutService payoutService) {
    this.payoutService = payoutService;
  }

  @Operation(summary = "List payouts", description = "Retrieves a paginated list of payouts for the current tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payouts retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires BILLING_ADMIN, FINANCE_VIEWER, TENANT_OWNER, or SUPER_ADMIN role")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<Page<PayoutDtos.PayoutResponse>> listPayouts(Pageable pageable) {
    return ResponseEntity.ok(payoutService.getPayouts(pageable));
  }

  @Operation(summary = "Get payout details", description = "Retrieves details of a specific payout by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payout details found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires BILLING_ADMIN, FINANCE_VIEWER, TENANT_OWNER, or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Payout not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<PayoutDtos.PayoutResponse> getPayout(@PathVariable String id) {
    return ResponseEntity.ok(payoutService.getPayout(id));
  }
}
