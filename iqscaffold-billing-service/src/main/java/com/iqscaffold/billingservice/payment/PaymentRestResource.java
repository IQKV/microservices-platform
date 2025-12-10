package com.iqscaffold.billingservice.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment processing endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/payments")
@Tag(name = "Payment Processing", description = "Payment processing operations")
public class PaymentRestResource {

  private final PaymentService paymentService;

  public PaymentRestResource(final PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  // REST endpoints will be added in subsequent tasks
}
