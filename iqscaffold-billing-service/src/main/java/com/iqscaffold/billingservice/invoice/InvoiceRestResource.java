package com.iqscaffold.billingservice.invoice;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for invoice management endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/invoices")
@Tag(name = "Invoice Management", description = "Invoice generation and management operations")
public class InvoiceRestResource {

  private final InvoiceService invoiceService;

  public InvoiceRestResource(final InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  // REST endpoints will be added in subsequent tasks
}
