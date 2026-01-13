package com.iqscaffold.billingservice.security;

import java.util.UUID;

import com.iqscaffold.billingservice.subscription.SubscriptionInvoice;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoiceRepository;
import com.iqscaffold.billingservice.subscription.SubscriptionNotFoundException;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Centralized authorization service for billing operations.
 * <p>
 * Implements the following authorization rules:
 * <ul>
 *   <li><strong>Subscription creation:</strong> Requires authenticated user</li>
 *   <li><strong>Subscription management:</strong> Owner or BILLING_ADMIN</li>
 *   <li><strong>Plan management:</strong> SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN</li>
 *   <li><strong>Invoice access:</strong> Owner, BILLING_ADMIN, or FINANCE_VIEWER</li>
 * </ul>
 */
@Service
public class BillingAuthorizationService {

  private static final Logger log = LoggerFactory.getLogger(BillingAuthorizationService.class);

  private final TenantSubscriptionRepository subscriptionRepository;
  private final SubscriptionInvoiceRepository invoiceRepository;

  public BillingAuthorizationService(
      final TenantSubscriptionRepository subscriptionRepository,
      final SubscriptionInvoiceRepository invoiceRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.invoiceRepository = invoiceRepository;
  }

  /**
   * Verify user can create subscriptions.
   * Requires: Authenticated user with SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN role
   *
   * @throws AccessDeniedException if user lacks permission
   */
  public void requireSubscriptionCreatePermission() {
    UserContext userContext = SecurityContextHelper.getCurrentUserContextOrThrow();
    
    if (!userContext.canModifyBilling()) {
      log.warn("User {} attempted to create subscription without permission", userContext.userId());
      throw new AccessDeniedException("Insufficient permissions to create subscriptions");
    }
  }

  /**
   * Verify user can manage a specific subscription.
   * Requires: Owner (same tenant via schema) or BILLING_ADMIN role
   *
   * @param subscriptionId The subscription ID
   * @throws AccessDeniedException if user lacks permission
   * @throws SubscriptionNotFoundException if subscription not found
   */
  public void requireSubscriptionManagePermission(UUID subscriptionId) {
    UserContext userContext = SecurityContextHelper.getCurrentUserContextOrThrow();
    
    // Verify subscription exists and is accessible (schema isolation ensures ownership)
    if (!subscriptionRepository.existsById(subscriptionId)) {
      throw new SubscriptionNotFoundException("Subscription not found: " + subscriptionId);
    }

    // Super admin can manage any subscription
    if (userContext.isSuperAdmin()) {
      return;
    }

    // Schema isolation ensures we can only see our own subscriptions
    // If we found it, we own it. Just check role permissions.
    if (!userContext.hasAuthority("BILLING_ADMIN") && !userContext.isTenantOwner()) {
      log.warn("User {} attempted to manage subscription {} without permission", 
          userContext.userId(), subscriptionId);
      throw new AccessDeniedException("Insufficient permissions to manage this subscription");
    }
  }

  /**
   * Verify user can view a specific subscription.
   * Requires: Owner (same tenant via schema), BILLING_ADMIN, or FINANCE_VIEWER role
   *
   * @param subscriptionId The subscription ID
   * @throws AccessDeniedException if user lacks permission
   * @throws SubscriptionNotFoundException if subscription not found
   */
  public void requireSubscriptionViewPermission(UUID subscriptionId) {
    UserContext userContext = SecurityContextHelper.getCurrentUserContextOrThrow();
    
    // Verify subscription exists and is accessible (schema isolation ensures ownership)
    if (!subscriptionRepository.existsById(subscriptionId)) {
      throw new SubscriptionNotFoundException("Subscription not found: " + subscriptionId);
    }

    // Super admin can view any subscription
    if (userContext.isSuperAdmin()) {
      return;
    }

    // Schema isolation ensures we can only see our own subscriptions
    // If we found it, we own it. Just check role permissions.
    if (!userContext.hasBillingAccess()) {
      log.warn("User {} attempted to view subscription {} without permission", 
          userContext.userId(), subscriptionId);
      throw new AccessDeniedException("Insufficient permissions to view this subscription");
    }
  }

  /**
   * Verify user can manage subscription plans.
   * Requires: SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN role
   *
   * @throws AccessDeniedException if user lacks permission
   */
  public void requirePlanManagePermission() {
    UserContext userContext = SecurityContextHelper.getCurrentUserContextOrThrow();
    
    if (!userContext.isSuperAdmin() 
        && !userContext.isTenantOwner() 
        && !userContext.hasAuthority("BILLING_ADMIN")) {
      log.warn("User {} attempted to manage subscription plans without permission", userContext.userId());
      throw new AccessDeniedException("Insufficient permissions to manage subscription plans");
    }
  }

  /**
   * Verify user can view an invoice.
   * Requires: Owner (same tenant via schema), BILLING_ADMIN, or FINANCE_VIEWER role
   *
   * @param invoiceId The invoice ID
   * @throws AccessDeniedException if user lacks permission
   */
  public void requireInvoiceViewPermission(UUID invoiceId) {
    UserContext userContext = SecurityContextHelper.getCurrentUserContextOrThrow();
    
    // Verify invoice exists and is accessible (schema isolation ensures ownership)
    if (!invoiceRepository.existsById(invoiceId)) {
      throw new IllegalArgumentException("Invoice not found: " + invoiceId);
    }

    // Super admin can view any invoice
    if (userContext.isSuperAdmin()) {
      return;
    }

    // Schema isolation ensures we can only see our own invoices
    // If we found it, we own it. Just check role permissions.
    if (!userContext.hasBillingAccess()) {
      log.warn("User {} attempted to view invoice {} without permission", 
          userContext.userId(), invoiceId);
      throw new AccessDeniedException("Insufficient permissions to view this invoice");
    }
  }

  /**
   * Verify user can list invoices for their tenant.
   * Requires: Owner (same tenant), BILLING_ADMIN, or FINANCE_VIEWER role
   *
   * @throws AccessDeniedException if user lacks permission
   */
  public void requireInvoiceListPermission() {
    UserContext userContext = SecurityContextHelper.getCurrentUserContextOrThrow();
    
    if (!userContext.hasBillingAccess()) {
      log.warn("User {} attempted to list invoices without permission", userContext.userId());
      throw new AccessDeniedException("Insufficient permissions to view invoices");
    }
  }

}
