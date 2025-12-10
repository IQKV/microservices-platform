package com.iqscaffold.billingservice.paymentmethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service layer for payment method management business logic.
 */
@Service
public class PaymentMethodService {

  private static final Logger log = LoggerFactory.getLogger(PaymentMethodService.class);

  private final PaymentMethodRepository paymentMethodRepository;

  public PaymentMethodService(final PaymentMethodRepository paymentMethodRepository) {
    this.paymentMethodRepository = paymentMethodRepository;
  }

  // Service methods will be added in subsequent tasks
}
