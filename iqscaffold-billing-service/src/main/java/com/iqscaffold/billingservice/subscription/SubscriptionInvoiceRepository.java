package com.iqscaffold.billingservice.subscription;

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
 * Repository for {@link SubscriptionInvoice} entities.
 */
@Repository
public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, UUID> {

  /**
   * Find invoices by tenant subscription ID.
   *
   * @param tenantSubscriptionId Tenant subscription ID
   * @param pageable             Pagination parameters
   * @return Page of invoices
   */
  @Query("SELECT i FROM SubscriptionInvoice i WHERE i.tenantSubscription.id = :tenantSubscriptionId")
  Page<SubscriptionInvoice> findByTenantSubscriptionId(@Param("tenantSubscriptionId") UUID tenantSubscriptionId, Pageable pageable);

  /**
   * Find invoices by tenant ID.
   *
   * @param tenantId Tenant ID
   * @param pageable Pagination parameters
   * @return Page of invoices
   */
  Page<SubscriptionInvoice> findByTenantId(String tenantId, Pageable pageable);

  /**
   * Find an invoice by Stripe invoice ID.
   *
   * @param stripeInvoiceId Stripe invoice ID
   * @return Optional invoice
   */
  Optional<SubscriptionInvoice> findByStripeInvoiceId(String stripeInvoiceId);

  /**
   * Find invoices by status.
   *
   * @param status   Invoice status
   * @param pageable Pagination parameters
   * @return Page of invoices
   */
  Page<SubscriptionInvoice> findByStatus(InvoiceStatus status, Pageable pageable);

  /**
   * Find open invoices for a tenant.
   *
   * @param tenantId Tenant ID
   * @return List of open invoices
   */
  @Query("SELECT i FROM SubscriptionInvoice i WHERE i.tenantId = :tenantId AND i.status = 'OPEN'")
  List<SubscriptionInvoice> findOpenInvoicesByTenantId(@Param("tenantId") String tenantId);

  /**
   * Find overdue invoices.
   *
   * @return List of overdue invoices
   */
  @Query("SELECT i FROM SubscriptionInvoice i WHERE i.status = 'OPEN' AND i.dueDate < CURRENT_TIMESTAMP")
  List<SubscriptionInvoice> findOverdueInvoices();
}
