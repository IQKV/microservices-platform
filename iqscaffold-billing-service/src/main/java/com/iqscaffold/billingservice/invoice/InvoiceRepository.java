package com.iqscaffold.billingservice.invoice;

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
 * Repository interface for Invoice aggregate.
 * 
 * <p>This repository provides methods to query and persist invoice aggregates.
 * All queries return fully reconstituted aggregates with their associated subscription
 * and payment method data.
 * 
 * <p>Uses text blocks (Java 21) for multi-line JPQL queries to improve readability.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  /**
   * Finds an invoice by its unique invoice number.
   * 
   * @param invoiceNumber unique invoice identifier
   * @return optional containing the invoice, or empty if not found
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.invoiceNumber = :invoiceNumber
      """)
  Optional<Invoice> findByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

  /**
   * Finds all invoices for a tenant.
   * 
   * @param tenantId tenant identifier
   * @param pageable pagination information
   * @return page of invoices ordered by creation date descending
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.tenantId = :tenantId
      ORDER BY i.createdAt DESC
      """)
  Page<Invoice> findByTenantId(
      @Param("tenantId") UUID tenantId,
      Pageable pageable
  );

  /**
   * Finds invoices by tenant and status.
   * 
   * @param tenantId tenant identifier
   * @param status invoice status
   * @param pageable pagination information
   * @return page of invoices matching criteria
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.tenantId = :tenantId
        AND i.status = :status
      ORDER BY i.createdAt DESC
      """)
  Page<Invoice> findByTenantIdAndStatus(
      @Param("tenantId") UUID tenantId,
      @Param("status") InvoiceStatus status,
      Pageable pageable
  );

  /**
   * Finds invoices by subscription.
   * 
   * @param subscriptionId subscription identifier
   * @return list of invoices for the subscription ordered by creation date descending
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.subscription.id = :subscriptionId
      ORDER BY i.createdAt DESC
      """)
  List<Invoice> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

  /**
   * Finds invoices by status.
   * 
   * @param status invoice status
   * @param pageable pagination information
   * @return page of invoices with the specified status
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.status = :status
      ORDER BY i.createdAt DESC
      """)
  Page<Invoice> findByStatus(
      @Param("status") InvoiceStatus status,
      Pageable pageable
  );

  /**
   * Finds overdue invoices.
   * Returns invoices in OPEN status where due date has passed.
   * 
   * @param now current timestamp
   * @return list of overdue invoices ordered by due date ascending
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.status = 'OPEN'
        AND i.dueDate IS NOT NULL
        AND i.dueDate < :now
      ORDER BY i.dueDate ASC
      """)
  List<Invoice> findOverdueInvoices(@Param("now") LocalDateTime now);

  /**
   * Finds invoices due soon.
   * Returns invoices in OPEN status where due date is within the specified days.
   * 
   * @param now current timestamp
   * @param dueDate end of the look-ahead period
   * @return list of invoices due soon ordered by due date ascending
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.status = 'OPEN'
        AND i.dueDate IS NOT NULL
        AND i.dueDate >= :now
        AND i.dueDate <= :dueDate
      ORDER BY i.dueDate ASC
      """)
  List<Invoice> findInvoicesDueSoon(
      @Param("now") LocalDateTime now,
      @Param("dueDate") LocalDateTime dueDate
  );

  /**
   * Finds unpaid invoices for a tenant.
   * Returns invoices in OPEN or DRAFT status.
   * 
   * @param tenantId tenant identifier
   * @return list of unpaid invoices ordered by due date ascending
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.tenantId = :tenantId
        AND i.status IN ('OPEN', 'DRAFT')
      ORDER BY i.dueDate ASC
      """)
  List<Invoice> findUnpaidByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds the most recent invoice for a subscription.
   * 
   * @param subscriptionId subscription identifier
   * @return optional containing the most recent invoice, or empty if none exists
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.subscription.id = :subscriptionId
      ORDER BY i.createdAt DESC
      LIMIT 1
      """)
  Optional<Invoice> findMostRecentBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

  /**
   * Finds invoices by period.
   * Returns invoices where period start and end fall within the specified range.
   * 
   * @param startDate period start date
   * @param endDate period end date
   * @param pageable pagination information
   * @return page of invoices for the specified period
   */
  @Query("""
      SELECT i FROM Invoice i
      LEFT JOIN FETCH i.subscription s
      LEFT JOIN FETCH s.plan
      LEFT JOIN FETCH i.paymentMethod
      WHERE i.periodStart >= :startDate
        AND i.periodEnd <= :endDate
      ORDER BY i.createdAt DESC
      """)
  Page<Invoice> findByPeriod(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable
  );

  /**
   * Counts unpaid invoices for a tenant.
   * 
   * @param tenantId tenant identifier
   * @return number of unpaid invoices
   */
  @Query("""
      SELECT COUNT(i) FROM Invoice i
      WHERE i.tenantId = :tenantId
        AND i.status IN ('OPEN', 'DRAFT')
      """)
  long countUnpaidByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Calculates total amount of unpaid invoices for a tenant.
   * 
   * @param tenantId tenant identifier
   * @return sum of total amounts for unpaid invoices
   */
  @Query("""
      SELECT COALESCE(SUM(i.total), 0) FROM Invoice i
      WHERE i.tenantId = :tenantId
        AND i.status IN ('OPEN', 'DRAFT')
      """)
  java.math.BigDecimal calculateUnpaidTotalByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Checks if an invoice number already exists.
   * 
   * @param invoiceNumber invoice number to check
   * @return true if invoice number exists
   */
  @Query("""
      SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
      FROM Invoice i
      WHERE i.invoiceNumber = :invoiceNumber
      """)
  boolean existsByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

  /**
   * Finds the next invoice sequence number for a given year and month.
   * Used for generating unique invoice numbers.
   * 
   * @param yearMonth year and month prefix (e.g., "202412")
   * @return the highest sequence number for the period, or 0 if none exists
   */
  @Query("""
      SELECT COALESCE(MAX(
        CAST(SUBSTRING(i.invoiceNumber, LENGTH(i.invoiceNumber) - 4, 5) AS integer)
      ), 0)
      FROM Invoice i
      WHERE i.invoiceNumber LIKE CONCAT('INV-', :yearMonth, '-%')
      """)
  int findMaxSequenceForPeriod(@Param("yearMonth") String yearMonth);
}
