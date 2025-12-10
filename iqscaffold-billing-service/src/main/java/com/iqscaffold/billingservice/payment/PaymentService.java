package com.iqscaffold.billingservice.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service layer for payment processing business logic.
 */
@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;

  public PaymentService(final PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  // Service methods will be added in subsequent tasks
}
