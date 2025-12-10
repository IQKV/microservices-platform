package com.iqscaffold.billingservice.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Payment aggregate.
 *
 * <p>This repository provides methods to query and persist payment aggregates.
 * All queries return fully reconstituted aggregates with their associated invoice
 * and payment method data.
 *
 * <p>Uses text blocks (Java 21) for multi-line JPQL queries to improve readability.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  /**
   * Finds a payment by its provider payment ID.
   *
   * @param providerPaymentId payment identifier from payment provider
   * @return optional containing the payment, or empty if not found
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.providerPaymentId = :providerPaymentId
      """)
  Optional<Payment> findByProviderPaymentId(@Param("providerPaymentId") String providerPaymentId);

  /**
   * Finds all payments for a tenant.
   *
   * @param tenantId tenant identifier
   * @param pageable pagination information
   * @return page of payments ordered by creation date descending
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.tenantId = :tenantId
      ORDER BY p.createdAt DESC
      """)
  Page<Payment> findByTenantId(
      @Param("tenantId") UUID tenantId,
      Pageable pageable
  );

  /**
   * Finds all payments for a tenant (without pagination).
   *
   * @param tenantId tenant identifier
   * @return list of payments ordered by creation date descending
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.tenantId = :tenantId
      ORDER BY p.createdAt DESC
      """)
  List<Payment> findByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds all payments for a tenant (for GDPR export).
   *
   * @param tenantId tenant identifier as string
   * @return list of all payments for the tenant
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.tenantId = CAST(:tenantId AS uuid)
      ORDER BY p.createdAt DESC
      """)
  List<Payment> findByTenantId(@Param("tenantId") String tenantId);

  /**
   * Finds payments by invoice.
   *
   * @param invoiceId invoice identifier
   * @return list of payments for the invoice ordered by creation date descending
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.invoice.id = :invoiceId
      ORDER BY p.createdAt DESC
      """)
  List<Payment> findByInvoiceId(@Param("invoiceId") Long invoiceId);

  /**
   * Finds payments by status.
   *
   * @param status   payment status
   * @param pageable pagination information
   * @return page of payments with the specified status
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.status = :status
      ORDER BY p.createdAt DESC
      """)
  Page<Payment> findByStatus(
      @Param("status") PaymentStatus status,
      Pageable pageable
  );

  /**
   * Finds payments by tenant and status.
   *
   * @param tenantId tenant identifier
   * @param status   payment status
   * @param pageable pagination information
   * @return page of payments matching criteria
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.tenantId = :tenantId
        AND p.status = :status
      ORDER BY p.createdAt DESC
      """)
  Page<Payment> findByTenantIdAndStatus(
      @Param("tenantId") UUID tenantId,
      @Param("status") PaymentStatus status,
      Pageable pageable
  );

  /**
   * Finds failed payments eligible for retry.
   * Returns payments in FAILED status created within the retry window.
   *
   * @param retryAfter earliest creation date for retry eligibility
   * @return list of failed payments eligible for retry
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.status = 'FAILED'
        AND p.createdAt >= :retryAfter
      ORDER BY p.createdAt ASC
      """)
  List<Payment> findFailedPaymentsForRetry(@Param("retryAfter") LocalDateTime retryAfter);

  /**
   * Finds the most recent payment for an invoice.
   *
   * @param invoiceId invoice identifier
   * @return optional containing the most recent payment, or empty if none exists
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.invoice.id = :invoiceId
      ORDER BY p.createdAt DESC
      LIMIT 1
      """)
  Optional<Payment> findMostRecentByInvoiceId(@Param("invoiceId") Long invoiceId);

  /**
   * Finds successful payments for a tenant within a date range.
   * Used for revenue reporting and analytics.
   *
   * @param tenantId  tenant identifier
   * @param startDate start of date range
   * @param endDate   end of date range
   * @return list of successful payments in the date range
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.tenantId = :tenantId
        AND p.status = 'SUCCEEDED'
        AND p.createdAt >= :startDate
        AND p.createdAt <= :endDate
      ORDER BY p.createdAt DESC
      """)
  List<Payment> findSuccessfulPaymentsByTenantIdAndDateRange(
      @Param("tenantId") UUID tenantId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Finds payments by payment method.
   *
   * @param paymentMethodId payment method identifier
   * @return list of payments using the payment method
   */
  @Query("""
      SELECT p FROM Payment p
      LEFT JOIN FETCH p.invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH p.paymentMethod
      WHERE p.paymentMethod.id = :paymentMethodId
      ORDER BY p.createdAt DESC
      """)
  List<Payment> findByPaymentMethodId(@Param("paymentMethodId") Long paymentMethodId);

  /**
   * Counts failed payments for a tenant.
   *
   * @param tenantId tenant identifier
   * @return number of failed payments
   */
  @Query("""
      SELECT COUNT(p) FROM Payment p
      WHERE p.tenantId = :tenantId
        AND p.status = 'FAILED'
      """)
  long countFailedByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Calculates total successful payment amount for a tenant.
   *
   * @param tenantId tenant identifier
   * @return sum of amounts for successful payments
   */
  @Query("""
      SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
      WHERE p.tenantId = :tenantId
        AND p.status = 'SUCCEEDED'
      """)
  java.math.BigDecimal calculateTotalSuccessfulByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Calculates payment success rate for a tenant.
   * Returns the percentage of successful payments.
   *
   * @param tenantId tenant identifier
   * @return success rate as a decimal (0.0 to 1.0)
   */
  @Query("""
      SELECT CASE WHEN COUNT(p) > 0 THEN
        CAST(SUM(CASE WHEN p.status = 'SUCCEEDED' THEN 1 ELSE 0 END) AS double) / COUNT(p)
      ELSE 0.0 END
      FROM Payment p
      WHERE p.tenantId = :tenantId
      """)
  double calculateSuccessRateByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Checks if a provider payment ID already exists.
   * Used for idempotency checking.
   *
   * @param providerPaymentId provider payment identifier
   * @return true if payment exists
   */
  @Query("""
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM Payment p
      WHERE p.providerPaymentId = :providerPaymentId
      """)
  boolean existsByProviderPaymentId(@Param("providerPaymentId") String providerPaymentId);
}
