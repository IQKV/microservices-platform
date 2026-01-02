package com.iqscaffold.billingservice.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantStripeConfigRepository extends JpaRepository<MerchantStripeConfig, UUID> {
    Optional<MerchantStripeConfig> findByTenantId(String tenantId);
    Optional<MerchantStripeConfig> findByStripeAccountId(String stripeAccountId);
}
