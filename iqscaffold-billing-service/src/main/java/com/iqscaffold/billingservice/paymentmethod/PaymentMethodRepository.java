package com.iqscaffold.billingservice.paymentmethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for PaymentMethod aggregate.
 *
 * <p>This repository provides methods to query and persist payment method aggregates.
 * Payment methods are stored in tenant-scoped schemas for data isolation.
 *
 * <p>Uses text blocks (Java 21) for multi-line JPQL queries to improve readability.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

  /**
   * Finds all payment methods for a tenant.
   *
   * @param tenantId tenant identifier
   * @return list of payment methods ordered by default flag and creation date
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.active = true
      ORDER BY pm.isDefault DESC, pm.createdAt DESC
      """)
  List<PaymentMethod> findByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds all payment methods for a tenant (for GDPR export).
   *
   * @param tenantId tenant identifier as string
   * @return list of all payment methods for the tenant
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = CAST(:tenantId AS uuid)
      ORDER BY pm.createdAt DESC
      """)
  List<PaymentMethod> findByTenantId(@Param("tenantId") String tenantId);

  /**
   * Finds all active payment methods for a tenant.
   *
   * @param tenantId tenant identifier
   * @return list of active payment methods
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.active = true
      ORDER BY pm.isDefault DESC, pm.createdAt DESC
      """)
  List<PaymentMethod> findActiveByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds the default payment method for a tenant.
   *
   * @param tenantId tenant identifier
   * @return optional containing the default payment method, or empty if none exists
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.isDefault = true
        AND pm.active = true
      """)
  Optional<PaymentMethod> findDefaultByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds a payment method by provider payment method ID.
   *
   * @param providerPaymentMethodId payment method identifier from payment provider
   * @return optional containing the payment method, or empty if not found
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.providerPaymentMethodId = :providerPaymentMethodId
      """)
  Optional<PaymentMethod> findByProviderPaymentMethodId(
      @Param("providerPaymentMethodId") String providerPaymentMethodId
  );

  /**
   * Finds payment methods by user.
   *
   * @param userId user identifier
   * @return list of payment methods created by the user
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.userId = :userId
        AND pm.active = true
      ORDER BY pm.isDefault DESC, pm.createdAt DESC
      """)
  List<PaymentMethod> findByUserId(@Param("userId") UUID userId);

  /**
   * Finds payment methods by type.
   *
   * @param tenantId tenant identifier
   * @param type     payment method type (CARD, BANK_ACCOUNT, PAYPAL)
   * @return list of payment methods of the specified type
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.type = :type
        AND pm.active = true
      ORDER BY pm.isDefault DESC, pm.createdAt DESC
      """)
  List<PaymentMethod> findByTenantIdAndType(
      @Param("tenantId") UUID tenantId,
      @Param("type") PaymentMethodType type
  );

  /**
   * Finds expiring payment methods.
   * Returns payment methods expiring in the specified month and year.
   *
   * @param expiryMonth expiration month (1-12)
   * @param expiryYear  expiration year
   * @return list of expiring payment methods
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.expiryMonth = :expiryMonth
        AND pm.expiryYear = :expiryYear
        AND pm.active = true
      ORDER BY pm.tenantId ASC
      """)
  List<PaymentMethod> findExpiringPaymentMethods(
      @Param("expiryMonth") Integer expiryMonth,
      @Param("expiryYear") Integer expiryYear
  );

  /**
   * Finds expired payment methods.
   * Returns payment methods where expiry date is in the past.
   *
   * @param currentMonth current month (1-12)
   * @param currentYear  current year
   * @return list of expired payment methods
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.active = true
        AND (
          pm.expiryYear < :currentYear
          OR (pm.expiryYear = :currentYear AND pm.expiryMonth < :currentMonth)
        )
      ORDER BY pm.tenantId ASC
      """)
  List<PaymentMethod> findExpiredPaymentMethods(
      @Param("currentMonth") Integer currentMonth,
      @Param("currentYear") Integer currentYear
  );

  /**
   * Counts active payment methods for a tenant.
   *
   * @param tenantId tenant identifier
   * @return number of active payment methods
   */
  @Query("""
      SELECT COUNT(pm) FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.active = true
      """)
  long countActiveByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Checks if a tenant has a default payment method.
   *
   * @param tenantId tenant identifier
   * @return true if tenant has a default payment method
   */
  @Query("""
      SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
      FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.isDefault = true
        AND pm.active = true
      """)
  boolean hasDefaultPaymentMethod(@Param("tenantId") UUID tenantId);

  /**
   * Checks if a provider payment method ID already exists.
   * Used for idempotency checking.
   *
   * @param providerPaymentMethodId provider payment method identifier
   * @return true if payment method exists
   */
  @Query("""
      SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
      FROM PaymentMethod pm
      WHERE pm.providerPaymentMethodId = :providerPaymentMethodId
      """)
  boolean existsByProviderPaymentMethodId(
      @Param("providerPaymentMethodId") String providerPaymentMethodId
  );

  /**
   * Finds all default payment methods (for validation).
   * Should return at most one per tenant.
   *
   * @return list of default payment methods
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.isDefault = true
        AND pm.active = true
      ORDER BY pm.tenantId ASC
      """)
  List<PaymentMethod> findAllDefaultPaymentMethods();

  /**
   * Finds a payment method by ID and tenant ID.
   *
   * @param id       payment method ID
   * @param tenantId tenant identifier
   * @return optional containing the payment method, or empty if not found
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.id = :id
        AND pm.tenantId = :tenantId
      """)
  Optional<PaymentMethod> findByIdAndTenantId(
      @Param("id") Long id,
      @Param("tenantId") UUID tenantId
  );

  /**
   * Finds all default payment methods for a tenant.
   *
   * @param tenantId tenant identifier
   * @return list of default payment methods (should be at most one)
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.isDefault = true
      """)
  List<PaymentMethod> findByTenantIdAndIsDefaultTrue(@Param("tenantId") UUID tenantId);

  /**
   * Finds all active payment methods for a tenant.
   *
   * @param tenantId tenant identifier
   * @return list of active payment methods
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.tenantId = :tenantId
        AND pm.active = true
      ORDER BY pm.isDefault DESC, pm.createdAt DESC
      """)
  List<PaymentMethod> findByTenantIdAndActiveTrue(@Param("tenantId") UUID tenantId);

  /**
   * Deletes all payment methods for a tenant (for GDPR deletion).
   *
   * @param tenantId tenant identifier as string
   * @return number of payment methods deleted
   */
  @Query("""
      DELETE FROM PaymentMethod pm
      WHERE pm.tenantId = CAST(:tenantId AS uuid)
      """)
  int deleteByTenantId(@Param("tenantId") String tenantId);

  /**
   * Finds payment methods deleted before a specific date that are not anonymized.
   * Used for retention policy enforcement.
   *
   * @param cutoffDate date before which to find deleted payment methods
   * @return list of payment methods to anonymize
   */
  @Query("""
      SELECT pm FROM PaymentMethod pm
      WHERE pm.deletedAt IS NOT NULL
        AND pm.deletedAt < :cutoffDate
        AND pm.last4 != '****'
      """)
  List<PaymentMethod> findByDeletedAtBeforeAndNotAnonymized(
      @Param("cutoffDate") java.time.LocalDateTime cutoffDate
  );
}
