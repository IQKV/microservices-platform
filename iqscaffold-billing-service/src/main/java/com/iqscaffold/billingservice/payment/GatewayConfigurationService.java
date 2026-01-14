package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.MerchantPaymentConfig;
import com.iqscaffold.billingservice.admin.MerchantPaymentConfigRepository;
import com.iqscaffold.billingservice.infrastructure.client.UserServiceClient;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for resolving payment gateway configuration based on tenant context.
 *
 * <p>This service enables runtime payment gateway selection by resolving which
 * provider to use for a given tenant/organization, supporting multi-gateway deployments.
 *
 * <h3>Gateway Resolution Strategy</h3>
 * <ol>
 *   <li><strong>Organization Config</strong> - Check if organization has configured gateway</li>
 *   <li><strong>Tenant Default</strong> - Fall back to tenant-wide default gateway</li>
 *   <li><strong>System Default</strong> - Use Stripe as final fallback</li>
 * </ol>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li>Payment creation - Determine which gateway to process payment through</li>
 *   <li>Refunds - Process refund through same gateway as original payment</li>
 *   <li>Merchant onboarding - Configure new gateway for organization</li>
 *   <li>Multi-gateway support - Allow different organizations to use different gateways</li>
 * </ul>
 */
@Service
public class GatewayConfigurationService {

  private static final Logger logger = LoggerFactory.getLogger(GatewayConfigurationService.class);

  private final MerchantPaymentConfigRepository merchantConfigRepository;
  private final UserServiceClient userServiceClient;
  private final PaymentProviderFactory providerFactory;

  // Default gateway if no configuration exists
  private static final PaymentGatewayProvider DEFAULT_GATEWAY = PaymentGatewayProvider.STRIPE;

  /**
   * Configuration result containing gateway provider and related settings.
   */
  public record GatewayConfiguration(
      PaymentGatewayProvider provider,
      Optional<String> gatewayAccountId,
      Optional<BigDecimal> applicationFeePercent,
      boolean chargesEnabled,
      boolean payoutsEnabled,
      boolean hasConfiguration
  ) {
    /**
     * Create default configuration without merchant setup.
     */
    public static GatewayConfiguration defaultConfig(PaymentGatewayProvider provider) {
      return new GatewayConfiguration(
          provider,
          Optional.empty(),
          Optional.empty(),
          false,
          false,
          false
      );
    }

    /**
     * Create configuration from merchant payment config.
     */
    public static GatewayConfiguration fromMerchantConfig(MerchantPaymentConfig config) {
      return new GatewayConfiguration(
          config.getGatewayProvider(),
          Optional.ofNullable(config.getGatewayAccountId()),
          Optional.ofNullable(config.getApplicationFeePercent()),
          config.isChargesEnabled(),
          config.isPayoutsEnabled(),
          true
      );
    }
  }

  public GatewayConfigurationService(
      final MerchantPaymentConfigRepository merchantConfigRepository,
      final UserServiceClient userServiceClient,
      final PaymentProviderFactory providerFactory) {
    this.merchantConfigRepository = merchantConfigRepository;
    this.userServiceClient = userServiceClient;
    this.providerFactory = providerFactory;
  }

  /**
   * Resolve payment gateway configuration for the current tenant.
   *
   * <p>Resolution strategy:
   * <ol>
   *   <li>Check if tenant's organization has payment gateway configured</li>
   *   <li>If configured, return that gateway provider and settings</li>
   *   <li>If not configured, return default gateway (Stripe) with no merchant setup</li>
   * </ol>
   *
   * @return Gateway configuration with provider selection and settings
   */
  public GatewayConfiguration resolveGatewayForCurrentTenant() {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    logger.debug("Resolving payment gateway configuration for tenant: {}", tenantId);

    // Get organization for this tenant
    var organization = userServiceClient.getOrganizationByTenantId(tenantId);

    if (organization.isEmpty()) {
      logger.warn("No organization found for tenant {}, using default gateway: {}",
          tenantId, DEFAULT_GATEWAY);
      return GatewayConfiguration.defaultConfig(DEFAULT_GATEWAY);
    }

    // Check if organization has payment gateway configured
    var merchantConfig = merchantConfigRepository.findByOrganizationId(organization.get().id());

    if (merchantConfig.isEmpty()) {
      logger.debug("No merchant configuration for organization {}, using default gateway: {}",
          organization.get().id(), DEFAULT_GATEWAY);
      return GatewayConfiguration.defaultConfig(DEFAULT_GATEWAY);
    }

    MerchantPaymentConfig config = merchantConfig.get();
    logger.info("Resolved gateway {} for tenant {} (organization: {})",
        config.getGatewayProvider(), tenantId, config.getOrganizationId());

    return GatewayConfiguration.fromMerchantConfig(config);
  }

  /**
   * Resolve payment gateway configuration for a specific organization.
   *
   * <p>Use this method when you know the organization ID (e.g., admin operations).
   *
   * @param organizationId The organization ID
   * @return Gateway configuration for the organization
   */
  public GatewayConfiguration resolveGatewayForOrganization(Long organizationId) {
    logger.debug("Resolving payment gateway configuration for organization: {}", organizationId);

    var merchantConfig = merchantConfigRepository.findByOrganizationId(organizationId);

    if (merchantConfig.isEmpty()) {
      logger.debug("No merchant configuration for organization {}, using default gateway: {}",
          organizationId, DEFAULT_GATEWAY);
      return GatewayConfiguration.defaultConfig(DEFAULT_GATEWAY);
    }

    MerchantPaymentConfig config = merchantConfig.get();
    logger.info("Resolved gateway {} for organization {}",
        config.getGatewayProvider(), organizationId);

    return GatewayConfiguration.fromMerchantConfig(config);
  }

  /**
   * Get the appropriate payment provider adapter for the current tenant.
   *
   * <p>This is a convenience method that resolves the gateway and returns
   * the corresponding PaymentProviderAdapter implementation.
   *
   * @return PaymentProviderAdapter for the tenant's configured gateway
   * @throws IllegalArgumentException if the configured gateway is not supported
   */
  public PaymentProviderAdapter getProviderForCurrentTenant() {
    GatewayConfiguration config = resolveGatewayForCurrentTenant();
    return providerFactory.getProvider(config.provider());
  }

  /**
   * Get the appropriate payment provider adapter for a specific organization.
   *
   * @param organizationId The organization ID
   * @return PaymentProviderAdapter for the organization's configured gateway
   * @throws IllegalArgumentException if the configured gateway is not supported
   */
  public PaymentProviderAdapter getProviderForOrganization(Long organizationId) {
    GatewayConfiguration config = resolveGatewayForOrganization(organizationId);
    return providerFactory.getProvider(config.provider());
  }

  /**
   * Check if a specific gateway provider is supported by the system.
   *
   * @param provider The gateway provider to check
   * @return true if the provider is supported, false otherwise
   */
  public boolean isProviderSupported(PaymentGatewayProvider provider) {
    return providerFactory.isProviderSupported(provider);
  }

  /**
   * Get default gateway provider used when no configuration exists.
   *
   * @return The default payment gateway provider
   */
  public PaymentGatewayProvider getDefaultGateway() {
    return DEFAULT_GATEWAY;
  }
}
