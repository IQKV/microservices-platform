package com.iqscaffold.billingservice.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Payment aggregate.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
  // Repository methods will be added in subsequent tasks
}
