package com.iqscaffold.billingservice.admin;

import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantStripeConfigRepository extends JpaRepository<MerchantStripeConfig, UUID> {
  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<MerchantStripeConfig> findByStripeAccountId(String stripeAccountId);

  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<MerchantStripeConfig> findByOrganizationId(Long organizationId);

  boolean existsByOrganizationId(Long organizationId);
}
