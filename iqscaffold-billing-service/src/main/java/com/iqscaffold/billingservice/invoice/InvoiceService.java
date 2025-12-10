package com.iqscaffold.billingservice.invoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service layer for invoice management business logic.
 */
@Service
public class InvoiceService {

  private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

  private final InvoiceRepository invoiceRepository;

  public InvoiceService(final InvoiceRepository invoiceRepository) {
    this.invoiceRepository = invoiceRepository;
  }

  // Service methods will be added in subsequent tasks
}
