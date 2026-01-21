package com.iqscaffold.userservice.organization;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service demonstrating optimal usage of entity graphs for Organization operations.
 *
 * <p>This service showcases how to leverage entity graphs to optimize query performance
 * for organization-related operations by eagerly loading specific relationships based
 * on business requirements.
 *
 * <h3>Entity Graph Usage Patterns</h3>
 * <ul>
 *   <li><strong>Tenant Context</strong> - Load organization with tenant for multi-tenant operations</li>
 *   <li><strong>Settings Management</strong> - Load organization with preferences for configuration</li>
 *   <li><strong>Complete Profile</strong> - Load organization with all associations for admin operations</li>
 * </ul>
 *
 * <h3>Business Use Cases</h3>
 * <ul>
 *   <li>Billing and subscription management</li>
 *   <li>Payment gateway configuration</li>
 *   <li>Organization settings and preferences</li>
 *   <li>Multi-tenant context resolution</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class OrganizationEntityGraphService {

  private final OrganizationRepository organizationRepository;

  public OrganizationEntityGraphService(final OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  /**
   * Find organization for tenant context resolution.
   * Use this when you need organization and tenant information together.
   *
   * @param tenantId the tenant ID to search for
   * @return Optional containing organization with tenant if found
   */
  public Optional<Organization> findOrganizationWithTenant(String tenantId) {
    return organizationRepository.findByTenantIdWithTenant(tenantId);
  }

  /**
   * Find organization for settings management.
   * Use this when displaying or updating organization preferences.
   *
   * @param organizationId the organization ID to search for
   * @return Optional containing organization with preferences if found
   */
  public Optional<Organization> findOrganizationForSettings(Long organizationId) {
    return organizationRepository.findByIdWithPreferences(organizationId);
  }

  /**
   * Find organization with complete profile.
   * Use this for admin operations or when you need full organization context.
   *
   * @param tenantId the tenant ID to search for
   * @return Optional containing organization with complete profile if found
   */
  public Optional<Organization> findOrganizationWithCompleteProfile(String tenantId) {
    return organizationRepository.findByTenantIdWithComplete(tenantId);
  }

  /**
   * Get organization billing information.
   * Optimized method for billing operations that need organization and tenant data.
   *
   * @param tenantId the tenant ID
   * @return OrganizationBillingInfo containing billing details, or null if not found
   */
  public OrganizationBillingInfo getOrganizationBillingInfo(String tenantId) {
    return organizationRepository.findByTenantIdWithTenant(tenantId)
        .map(org -> new OrganizationBillingInfo(
            org.getId(),
            org.getName(),
            org.getBillingEmail(),
            org.getSubscriptionStatus(),
            org.getSubscriptionPlan(),
            org.getPaymentGatewayProvider(),
            org.canAcceptPayments(),
            org.canReceivePayouts(),
            org.getTenant() != null ? org.getTenant().getName() : null
        ))
        .orElse(null);
  }

  /**
   * Get organization settings summary.
   * Loads organization with preferences for configuration display.
   *
   * @param organizationId the organization ID
   * @return OrganizationSettings containing configuration, or null if not found
   */
  public OrganizationSettings getOrganizationSettings(Long organizationId) {
    return organizationRepository.findByIdWithPreferences(organizationId)
        .map(org -> {
          var pref = org.getPreference();
          return new OrganizationSettings(
              org.getId(),
              org.getName(),
              org.getMaxUsers(),
              pref != null ? pref.getDefaultLocale() : "en",
              pref != null ? pref.getDefaultTimezone() : "UTC",
              pref != null ? pref.getDefaultCurrency() : "USD",
              pref != null ? pref.getPasswordMinLength() : 8
          );
        })
        .orElse(null);
  }

  /**
   * Check if organization can process payments.
   * Quick check for payment capability without loading unnecessary data.
   *
   * @param tenantId the tenant ID
   * @return true if organization can accept payments, false otherwise
   */
  public boolean canOrganizationAcceptPayments(String tenantId) {
    return organizationRepository.findByTenantId(tenantId)
        .map(Organization::canAcceptPayments)
        .orElse(false);
  }

  /**
   * Data transfer object for organization billing information.
   */
  public record OrganizationBillingInfo(
      Long organizationId,
      String organizationName,
      String billingEmail,
      String subscriptionStatus,
      String subscriptionPlan,
      com.iqscaffold.userservice.shared.PaymentGatewayProvider paymentGatewayProvider,
      boolean canAcceptPayments,
      boolean canReceivePayouts,
      String tenantName
  ) {
  }

  /**
   * Data transfer object for organization settings.
   */
  public record OrganizationSettings(
      Long organizationId,
      String organizationName,
      Integer maxUsers,
      String defaultLocale,
      String defaultTimezone,
      String defaultCurrency,
      Integer passwordMinLength
  ) {
  }
}
