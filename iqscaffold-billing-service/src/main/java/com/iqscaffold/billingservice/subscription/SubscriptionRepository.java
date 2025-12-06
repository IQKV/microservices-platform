package com.iqscaffold.billingservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Subscription aggregate.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
  // Repository methods will be added in subsequent tasks
}
