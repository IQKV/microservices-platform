package com.iqscaffold.billingservice.portal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import com.iqscaffold.billingservice.invoice.InvoiceApplicationService;
import com.iqscaffold.billingservice.subscription.SubscriptionApplicationService;
import com.iqscaffold.billingservice.usage.UsageApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for customer billing portal operations.
 *
 * <p>Provides business logic for the customer billing portal, including:
 * <ul>
 *   <li>Comprehensive billing dashboard generation</li>
 *   <li>Subscription management operations</li>
 *   <li>Usage and quota monitoring</li>
 *   <li>Invoice and payment history</li>
 * </ul>
 */
@Service
public class BillingPortalService {

  private static final Logger log = LoggerFactory.getLogger(BillingPortalService.class);

  private final SubscriptionApplicationService subscriptionApplicationService;
  private final UsageApplicationService usageApplicationService;
  private final InvoiceApplicationService invoiceApplicationService;

  public BillingPortalService(final SubscriptionApplicationService subscriptionApplicationService,
                              final UsageApplicationService usageApplicationService,
                              final InvoiceApplicationService invoiceApplicationService) {
    this.subscriptionApplicationService = subscriptionApplicationService;
    this.usageApplicationService = usageApplicationService;
    this.invoiceApplicationService = invoiceApplicationService;
  }

  /**
   * Retrieves comprehensive billing dashboard for a tenant.
   *
   * <p>Aggregates data from multiple services to provide a complete view of:
   * <ul>
   *   <li>Current subscription status and details</li>
   *   <li>Quota usage across all metrics</li>
   *   <li>Recent invoices (last 12 months)</li>
   *   <li>Upcoming invoice estimate</li>
   *   <li>Payment methods on file</li>
   *   <li>Available upgrade and downgrade options</li>
   * </ul>
   *
   * @param tenantId the tenant unique identifier
   * @return comprehensive billing dashboard DTO
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "billing-dashboard", key = "#tenantId")
  public BillingDashboardDto getDashboard(UUID tenantId) {
    log.info("Generating billing dashboard for tenant: {}", tenantId);

    // Get current subscription
    var subscription = subscriptionApplicationService.getActiveSubscription(tenantId);

    // Get quota usage - TODO: Implement quota usage conversion in future task
    // For now, return empty list as QuotaUsageDto requires quota limits from subscription plan
    var quotaUsage = Collections.<com.iqscaffold.billingservice.usage.QuotaUsageDto>emptyList();

    // Get recent invoices (all invoices for tenant)
    var recentInvoices = invoiceApplicationService.getInvoicesByTenant(tenantId);

    // Calculate upcoming invoice amount (current plan price)
    var upcomingInvoiceAmount = subscription.planPrice();

    // Get payment methods - TODO: Implement in future task
    var paymentMethods = Collections.<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto>emptyList();

    // Calculate total paid (sum of all paid invoices)
    var totalPaid = recentInvoices.stream()
        .filter(invoice -> invoice.status() == com.iqscaffold.billingservice.invoice.InvoiceStatus.PAID)
        .map(invoice -> invoice.total())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calculate days remaining in period
    var daysRemaining = ChronoUnit.DAYS.between(
        LocalDateTime.now(),
        subscription.currentPeriodEnd()
    );

    // Determine subscription status flags
    var inTrial = subscription.trialEnd() != null
                  && subscription.trialEnd().isAfter(LocalDateTime.now());
    var pastDue = subscription.status() == com.iqscaffold.billingservice.subscription.SubscriptionStatus.PAST_DUE;
    var canceled = subscription.cancelAtPeriodEnd() 
                   || subscription.status() == com.iqscaffold.billingservice.subscription.SubscriptionStatus.CANCELED;

    // Get available upgrade and downgrade options
    // TODO: Implement plan comparison logic in future task
    var availableUpgrades = Collections.<BillingDashboardDto.UpgradePlanOption>emptyList();
    var availableDowngrades = Collections.<BillingDashboardDto.DowngradePlanOption>emptyList();

    return new BillingDashboardDto(
        tenantId,
        subscription,
        quotaUsage,
        recentInvoices,
        upcomingInvoiceAmount,
        paymentMethods,
        totalPaid,
        daysRemaining,
        inTrial,
        pastDue,
        canceled,
        availableUpgrades,
        availableDowngrades
    );
  }
}
