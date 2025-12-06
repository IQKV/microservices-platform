package com.iqscaffold.billingservice.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for PaymentMethod aggregate.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
  // Repository methods will be added in subsequent tasks
}
