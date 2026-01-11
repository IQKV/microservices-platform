package com.iqscaffold.billingservice.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link PaymentGatewayConfig} entity.
 * Provides queries for managing tenant-specific payment gateway configurations.
 */
@Repository
public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfig, UUID> {

  /**
   * Find gateway configuration by tenant ID and gateway provider.
   *
   * @param tenantId        The tenant identifier
   * @param gatewayProvider The payment gateway provider
   * @return Optional containing the configuration if found
   */
  Optional<PaymentGatewayConfig> findByTenantIdAndGatewayProvider(String tenantId, PaymentGatewayProvider gatewayProvider);

  /**
   * Find all gateway configurations for a tenant.
   *
   * @param tenantId The tenant identifier
   * @return List of all gateway configurations for the tenant
   */
  List<PaymentGatewayConfig> findByTenantId(String tenantId);

  /**
   * Find all active gateway configurations for a tenant.
   *
   * @param tenantId The tenant identifier
   * @param isActive Whether the gateway is active
   * @return List of active gateway configurations
   */
  List<PaymentGatewayConfig> findByTenantIdAndIsActive(String tenantId, boolean isActive);

  /**
   * Find the primary gateway configuration for a tenant.
   *
   * @param tenantId  The tenant identifier
   * @param isPrimary Whether the gateway is primary
   * @return Optional containing the primary gateway configuration
   */
  Optional<PaymentGatewayConfig> findByTenantIdAndIsPrimary(String tenantId, boolean isPrimary);

  /**
   * Check if a gateway configuration exists for a tenant and provider.
   *
   * @param tenantId        The tenant identifier
   * @param gatewayProvider The payment gateway provider
   * @return true if configuration exists
   */
  boolean existsByTenantIdAndGatewayProvider(String tenantId, PaymentGatewayProvider gatewayProvider);

  /**
   * Find all active and primary gateway configurations for a tenant.
   * This is useful for quickly identifying the default payment method.
   *
   * @param tenantId  The tenant identifier
   * @param isActive  Whether the gateway is active
   * @param isPrimary Whether the gateway is primary
   * @return Optional containing the active primary gateway
   */
  Optional<PaymentGatewayConfig> findByTenantIdAndIsActiveAndIsPrimary(
      String tenantId, boolean isActive, boolean isPrimary);

  /**
   * Count active gateways for a tenant.
   *
   * @param tenantId The tenant identifier
   * @param isActive Whether the gateway is active
   * @return Number of active gateways
   */
  long countByTenantIdAndIsActive(String tenantId, boolean isActive);

  /**
   * Find all gateway configurations by mode (test or live).
   *
   * @param tenantId The tenant identifier
   * @param mode     The gateway mode (test/live)
   * @return List of gateway configurations in the specified mode
   */
  List<PaymentGatewayConfig> findByTenantIdAndMode(String tenantId, String mode);

  /**
   * Delete gateway configuration by tenant ID and provider.
   *
   * @param tenantId        The tenant identifier
   * @param gatewayProvider The payment gateway provider
   */
  void deleteByTenantIdAndGatewayProvider(String tenantId, PaymentGatewayProvider gatewayProvider);

  /**
   * Custom query to find gateways that need to be set as non-primary
   * when a new primary gateway is designated.
   *
   * @param tenantId        The tenant identifier
   * @param gatewayProvider The provider to exclude (the new primary)
   * @return List of configurations that should be set to non-primary
   */
  @Query("SELECT p FROM PaymentGatewayConfig p WHERE p.tenantId = :tenantId " +
         "AND p.isPrimary = true AND p.gatewayProvider <> :gatewayProvider")
  List<PaymentGatewayConfig> findOtherPrimaryGateways(
      @Param("tenantId") String tenantId,
      @Param("gatewayProvider") PaymentGatewayProvider gatewayProvider);
}
