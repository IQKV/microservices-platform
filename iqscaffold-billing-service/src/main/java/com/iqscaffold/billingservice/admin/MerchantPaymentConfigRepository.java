package com.iqscaffold.billingservice.admin;

import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantPaymentConfigRepository extends JpaRepository<MerchantPaymentConfig, UUID> {

  /**
   * Find merchant config by gateway account ID and provider.
   */
  Optional<MerchantPaymentConfig> findByGatewayAccountIdAndGatewayProvider(
      String gatewayAccountId,
      PaymentGatewayProvider gatewayProvider);

  /**
   * Find merchant config by organization ID.
   * Note: An organization may have multiple configs for different providers.
   */
  Optional<MerchantPaymentConfig> findByOrganizationId(Long organizationId);

  /**
   * Find merchant config by organization ID and provider.
   */
  Optional<MerchantPaymentConfig> findByOrganizationIdAndGatewayProvider(
      Long organizationId,
      PaymentGatewayProvider gatewayProvider);

  /**
   * Check if merchant config exists for organization.
   */
  boolean existsByOrganizationId(Long organizationId);

  /**
   * Check if merchant config exists for organization and provider.
   */
  boolean existsByOrganizationIdAndGatewayProvider(
      Long organizationId,
      PaymentGatewayProvider gatewayProvider);
}
