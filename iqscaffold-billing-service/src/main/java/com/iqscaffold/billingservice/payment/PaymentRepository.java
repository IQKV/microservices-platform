package com.iqscaffold.billingservice.payment;

import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<Payment> findByPaymentIntentId(String paymentIntentId);

  org.springframework.data.domain.Page<Payment> findAll(org.springframework.data.domain.Pageable pageable);
}
