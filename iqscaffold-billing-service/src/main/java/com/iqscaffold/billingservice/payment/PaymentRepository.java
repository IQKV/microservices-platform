package com.iqscaffold.billingservice.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  Optional<Payment> findByPaymentIntentId(String paymentIntentId);

  org.springframework.data.domain.Page<Payment> findAllByTenantId(String tenantId, org.springframework.data.domain.Pageable pageable);
  // Multi-tenancy handled by filtering in service layer primarily, or @Filter if using Hibernate filters
  // For simplicity, we trust services to filter by tenant_id when searching lists
}
