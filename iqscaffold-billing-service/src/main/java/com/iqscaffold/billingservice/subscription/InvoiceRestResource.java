package com.iqscaffold.billingservice.subscription;

import com.iqscaffold.billingservice.security.BillingAuthorizationService;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for subscription invoice management.
 * <p>
 * Provides endpoints for viewing invoices with proper authorization:
 * <ul>
 *   <li>Viewing invoice details</li>
 *   <li>Listing invoices for subscriptions</li>
 *   <li>Listing invoices for current tenant</li>
 * </ul>
 * 
 * <h4>Authorization:</h4>
 * Invoice access requires: Owner (same tenant), BILLING_ADMIN, or FINANCE_VIEWER role
 */
@RestController
@RequestMapping("/api/v1/billing/invoices")
@Tag(name = "Invoices", description = "Subscription invoice management")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceRestResource {

  private final SubscriptionInvoiceRepository invoiceRepository;
  private final BillingAuthorizationService authorizationService;

  public InvoiceRestResource(
      SubscriptionInvoiceRepository invoiceRepository,
      BillingAuthorizationService authorizationService) {
    this.invoiceRepository = invoiceRepository;
    this.authorizationService = authorizationService;
  }

  @Operation(
      summary = "Get invoice by ID",
      description = "Retrieves a specific invoice by its ID. Requires owner, BILLING_ADMIN, or FINANCE_VIEWER role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Invoice found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Invoice not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<SubscriptionDtos.InvoiceResponse> getInvoice(@PathVariable UUID id) {
    // Verify authorization with ownership check
    authorizationService.requireInvoiceViewPermission(id);
    
    SubscriptionInvoice invoice = invoiceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    
    return ResponseEntity.ok(mapToResponse(invoice));
  }

  @Operation(
      summary = "List invoices for current tenant",
      description = "Retrieves a paginated list of all invoices for the current tenant. Requires owner, BILLING_ADMIN, or FINANCE_VIEWER role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<Page<SubscriptionDtos.InvoiceResponse>> listInvoices(Pageable pageable) {
    // Verify authorization
    authorizationService.requireInvoiceListPermission();
    
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("Tenant context is required");
    }
    
    Page<SubscriptionInvoice> invoices = invoiceRepository.findByTenantId(tenantId, pageable);
    return ResponseEntity.ok(invoices.map(this::mapToResponse));
  }

  @Operation(
      summary = "List invoices for a subscription",
      description = "Retrieves a paginated list of invoices for a specific subscription. Requires owner, BILLING_ADMIN, or FINANCE_VIEWER role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Subscription not found")
  })
  @GetMapping("/subscription/{subscriptionId}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<Page<SubscriptionDtos.InvoiceResponse>> listInvoicesBySubscription(
      @PathVariable UUID subscriptionId,
      Pageable pageable) {
    // Verify authorization for subscription access
    authorizationService.requireSubscriptionViewPermission(subscriptionId);
    
    Page<SubscriptionInvoice> invoices = invoiceRepository.findByTenantSubscriptionId(subscriptionId, pageable);
    return ResponseEntity.ok(invoices.map(this::mapToResponse));
  }

  @Operation(
      summary = "List open invoices",
      description = "Retrieves all open (unpaid) invoices for the current tenant. Requires owner, BILLING_ADMIN, or FINANCE_VIEWER role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Open invoices retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
  })
  @GetMapping("/open")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  public ResponseEntity<java.util.List<SubscriptionDtos.InvoiceResponse>> listOpenInvoices() {
    // Verify authorization
    authorizationService.requireInvoiceListPermission();
    
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("Tenant context is required");
    }
    
    java.util.List<SubscriptionInvoice> invoices = invoiceRepository.findOpenInvoicesByTenantId(tenantId);
    return ResponseEntity.ok(invoices.stream().map(this::mapToResponse).toList());
  }

  private SubscriptionDtos.InvoiceResponse mapToResponse(SubscriptionInvoice invoice) {
    return new SubscriptionDtos.InvoiceResponse(
        invoice.getId(),
        invoice.getTenantSubscription() != null ? invoice.getTenantSubscription().getId() : null,
        invoice.getTenantId(),
        invoice.getStripeInvoiceId(),
        invoice.getInvoiceNumber(),
        invoice.getStatus().name(),
        invoice.getAmountDue(),
        invoice.getAmountPaid(),
        invoice.getCurrency(),
        invoice.getDueDate(),
        invoice.getPaidAt(),
        invoice.getHostedInvoiceUrl(),
        invoice.getInvoicePdfUrl(),
        invoice.getCreatedAt(),
        invoice.getUpdatedAt()
    );
  }
}
