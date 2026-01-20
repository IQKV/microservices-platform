package com.iqscaffold.billingservice.payment;

import jakarta.validation.Valid;
import java.util.UUID;

import com.iqscaffold.billingservice.payment.dto.PaymentDtos;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/billing/payments")
@Tag(name = "Payments", description = "Operations for managing payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentRestResource {

  private final PaymentService paymentService;
  private final RefundService refundService;

  public PaymentRestResource(final PaymentService paymentService, final RefundService refundService) {
    this.paymentService = paymentService;
    this.refundService = refundService;
  }

  @Operation(summary = "Create a payment intent", description = "Initiates a new payment flow by creating a Stripe PaymentIntent")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payment intent created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @PostMapping("/intent")
  @PreAuthorize("hasAnyAuthority('BILLING_ACCESS', 'BILLING_MANAGER', 'BILLING_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PaymentDtos.PaymentResponse> createPaymentIntent(@Valid @RequestBody PaymentDtos.CreatePaymentRequest request) {
    return ResponseEntity.ok(paymentService.createPaymentIntent(request));
  }

  @Operation(summary = "Get payment details", description = "Retrieves details of a payment by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payment details found"),
      @ApiResponse(responseCode = "404", description = "Payment not found or access denied")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('BILLING_ACCESS', 'BILLING_MANAGER', 'BILLING_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PaymentDtos.PaymentResponse> getPayment(@PathVariable UUID id) {
    return ResponseEntity.ok(paymentService.getPayment(id));
  }

  @Operation(summary = "List payments", description = "Retrieves a paginated list of payments for the current tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payments retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires BILLING_ADMIN, FINANCE_VIEWER, TENANT_OWNER, or SUPER_ADMIN role")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('BILLING_MANAGER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'TENANT_OWNER', 'FINANCE_VIEWER')")
  public ResponseEntity<org.springframework.data.domain.Page<PaymentDtos.PaymentResponse>> listPayments(org.springframework.data.domain.Pageable pageable) {
    return ResponseEntity.ok(paymentService.getPayments(pageable));
  }

  @Operation(summary = "Refund payment", description = "Initiates a full refund for a successful payment")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Refund initiated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid state for refund"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires BILLING_ADMIN, TENANT_OWNER, or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Payment not found")
  })
  @PostMapping("/{id}/refund")
  @PreAuthorize("hasAnyAuthority('BILLING_MANAGER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'TENANT_OWNER')")
  public ResponseEntity<Void> refundPayment(@PathVariable UUID id) {
    refundService.processRefund(id);
    return ResponseEntity.noContent().build();
  }
}
