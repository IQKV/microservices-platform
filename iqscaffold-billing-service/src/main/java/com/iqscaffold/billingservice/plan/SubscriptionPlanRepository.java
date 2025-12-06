package com.iqscaffold.billingservice.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for SubscriptionPlan aggregate.
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
  // Repository methods will be added in subsequent tasks
}
