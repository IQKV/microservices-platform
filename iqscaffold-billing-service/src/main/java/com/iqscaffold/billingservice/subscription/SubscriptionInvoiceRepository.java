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
 * <p>
 * All queries are automatically scoped to the current tenant's schema.
 * No tenant_id filtering needed - schema isolation provides tenant context.
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
   * Find open invoices for current tenant.
   *
   * @return List of open invoices
   */
  @Query("SELECT i FROM SubscriptionInvoice i WHERE i.status = 'OPEN'")
  List<SubscriptionInvoice> findOpenInvoices();

  /**
   * Find overdue invoices for current tenant.
   *
   * @return List of overdue invoices
   */
  @Query("SELECT i FROM SubscriptionInvoice i WHERE i.status = 'OPEN' AND i.dueDate < CURRENT_TIMESTAMP")
  List<SubscriptionInvoice> findOverdueInvoices();
}
