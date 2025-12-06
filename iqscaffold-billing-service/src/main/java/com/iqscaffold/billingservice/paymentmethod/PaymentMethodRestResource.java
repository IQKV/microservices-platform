package com.iqscaffold.billingservice.paymentmethod;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment method management endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Payment method management operations")
public class PaymentMethodRestResource {

  private final PaymentMethodService paymentMethodService;

  // REST endpoints will be added in subsequent tasks
}
