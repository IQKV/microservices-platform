package com.iqscaffold.billingservice.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UsageRecord entity.
 */
@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
  // Repository methods will be added in subsequent tasks
}
