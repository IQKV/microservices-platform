package com.iqscaffold.userservice.organization;

import java.util.List;
import java.util.Optional;

import com.iqscaffold.userservice.shared.PaymentGatewayProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Organization entity with payment gateway abstraction.
 * Clean greenfield implementation without backward compatibility.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  /**
   * Find organization by tenant ID with complete profile (tenant and preferences).
   */
  @EntityGraph("organization-complete")
  @Query("SELECT o FROM Organization o WHERE o.tenantId = :tenantId")
  Optional<Organization> findByTenantIdWithComplete(@Param("tenantId") String tenantId);

  /**
   * Find organization by tenant ID with tenant loaded.
   */
  @EntityGraph("organization-with-tenant")
  @Query("SELECT o FROM Organization o WHERE o.tenantId = :tenantId")
  Optional<Organization> findByTenantIdWithTenant(@Param("tenantId") String tenantId);

  /**
   * Find organization by ID with preferences loaded.
   */
  @EntityGraph("organization-with-preferences")
  @Query("SELECT o FROM Organization o WHERE o.id = :id")
  Optional<Organization> findByIdWithPreferences(@Param("id") Long id);

  /**
   * Find organization by tenant ID.
   */
  Optional<Organization> findByTenantId(String tenantId);

  /**
   * Find organization by payment gateway account ID and provider.
   */
  @Query("""
      SELECT o FROM Organization o 
      WHERE o.paymentGatewayAccountId = :accountId
      AND o.paymentGatewayProvider = :provider
      """)
  Optional<Organization> findByPaymentGatewayAccountIdAndProvider(
      @Param("accountId") String accountId,
      @Param("provider") PaymentGatewayProvider provider
  );

  /**
   * Find all organizations using a specific payment gateway provider.
   */
  List<Organization> findByPaymentGatewayProvider(PaymentGatewayProvider provider);

  /**
   * Find all organizations with payment gateway configured.
   */
  @Query("""
      SELECT o FROM Organization o 
      WHERE o.paymentGatewayAccountId IS NOT NULL
      AND o.paymentGatewayProvider IS NOT NULL
      """)
  List<Organization> findAllWithPaymentGateway();

  /**
   * Find all organizations that can accept payments.
   */
  @Query("""
      SELECT o FROM Organization o 
      WHERE o.paymentGatewayAccountId IS NOT NULL
      AND o.chargesEnabled = true
      """)
  List<Organization> findAllWithChargesEnabled();

  /**
   * Find all organizations that can receive payouts.
   */
  @Query("""
      SELECT o FROM Organization o 
      WHERE o.paymentGatewayAccountId IS NOT NULL
      AND o.payoutsEnabled = true
      """)
  List<Organization> findAllWithPayoutsEnabled();

  /**
   * Check if organization exists by tenant ID.
   */
  boolean existsByTenantId(String tenantId);

  /**
   * Check if payment gateway account is already in use.
   */
  boolean existsByPaymentGatewayAccountIdAndPaymentGatewayProvider(
      String accountId,
      PaymentGatewayProvider provider
  );

  /**
   * Find organizations by subscription status.
   */
  List<Organization> findBySubscriptionStatus(String status);

  /**
   * Find organizations by subscription plan.
   */
  List<Organization> findBySubscriptionPlan(String plan);

  /**
   * Find enabled organizations.
   */
  List<Organization> findByEnabledTrue();

  /**
   * Count organizations by payment gateway provider.
   */
  @Query("""
      SELECT COUNT(o) FROM Organization o 
      WHERE o.paymentGatewayProvider = :provider
      """)
  long countByPaymentGatewayProvider(@Param("provider") PaymentGatewayProvider provider);
}
