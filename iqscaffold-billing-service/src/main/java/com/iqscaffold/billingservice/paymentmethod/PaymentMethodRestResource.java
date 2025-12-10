package com.iqscaffold.billingservice.paymentmethod;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment method management endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/payment-methods")
@Tag(name = "Payment Methods", description = "Payment method management operations")
public class PaymentMethodRestResource {

  private final PaymentMethodService paymentMethodService;

  public PaymentMethodRestResource(PaymentMethodService paymentMethodService) {
    this.paymentMethodService = paymentMethodService;
  }

  // REST endpoints will be added in subsequent tasks
}
