package com.iqscaffold.billingservice.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Invoice aggregate.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
  // Repository methods will be added in subsequent tasks
}
