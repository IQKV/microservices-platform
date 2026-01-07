package com.iqscaffold.billingservice.payment;

import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeCustomerRepository extends JpaRepository<StripeCustomer, UUID> {
  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<StripeCustomer> findByEmailAndStripeAccountId(String email, String stripeAccountId);
  // Note: If stripeAccountId is null (platform), we search by null.
  // This matches PHP's findFirstWhere(['email' => ..., 'stripe_account_id' => ...])
}
