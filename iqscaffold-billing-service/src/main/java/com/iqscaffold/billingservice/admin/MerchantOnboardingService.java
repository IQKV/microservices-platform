package com.iqscaffold.billingservice.admin;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.dto.OnboardingLinkResponse;
import com.iqscaffold.billingservice.admin.dto.OrganizationDto;
import com.iqscaffold.billingservice.infrastructure.client.UserServiceClient;
import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.infrastructure.messaging.EventPublisher;
import com.iqscaffold.billingservice.infrastructure.messaging.MerchantOnboardedEvent;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.security.UserContext;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merchant onboarding service with organization support.
 * Links merchant Stripe accounts to organizations in the user service.
 */
@Service
public class MerchantOnboardingService {

  private static final Logger logger = LoggerFactory.getLogger(MerchantOnboardingService.class);

  private final MerchantPaymentConfigRepository repository;
  private final PaymentProviderFactory paymentProviderFactory;
  private final EmailService emailService;
  private final UserServiceClient userServiceClient;
  private final EventPublisher eventPublisher;

  public MerchantOnboardingService(
      final MerchantPaymentConfigRepository repository,
      final PaymentProviderFactory paymentProviderFactory,
      final EmailService emailService,
      final UserServiceClient userServiceClient,
      final EventPublisher eventPublisher) {
    this.repository = repository;
    this.paymentProviderFactory = paymentProviderFactory;
    this.emailService = emailService;
    this.userServiceClient = userServiceClient;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Initiates merchant onboarding for an organization with specified payment gateway.
   *
   * @param organizationId  Organization ID from user service
   * @param gatewayProvider Payment gateway provider (Stripe, PayPal, etc.)
   * @param refreshUrl      URL to redirect if onboarding link expires
   * @param returnUrl       URL to redirect after onboarding completion
   * @return Onboarding link response with URL and account details
   */
  @Transactional
  public OnboardingLinkResponse initiateOnboarding(
      Long organizationId,
      PaymentGatewayProvider gatewayProvider,
      String refreshUrl,
      String returnUrl) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    UserContext user = SecurityContextHelper.getCurrentUserContextOrThrow();

    logger.info("Initiating {} merchant onboarding for organization {} by user {}",
        gatewayProvider, organizationId, user.userId());

    // 1. Validate gateway provider is supported
    if (!paymentProviderFactory.isProviderSupported(gatewayProvider)) {
      throw new IllegalArgumentException("Unsupported payment gateway provider: " + gatewayProvider);
    }

    // Get payment provider adapter
    PaymentProviderAdapter paymentProvider = paymentProviderFactory.getProvider(gatewayProvider);

    // 2. Validate organization exists and belongs to tenant
    OrganizationDto organization = userServiceClient.getOrganization(organizationId)
        .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

    if (!organization.tenantId().equals(tenantId)) {
      logger.warn("Access denied: Organization {} does not belong to tenant {}", organizationId, tenantId);
      throw new AccessDeniedException("Organization does not belong to current tenant");
    }

    if (!organization.isActive()) {
      throw new IllegalStateException("Organization is not active: " + organizationId);
    }

    // 3. Check if already onboarded for this gateway provider
    var existingConfig = repository.findByOrganizationIdAndGatewayProvider(
        organizationId, gatewayProvider);

    if (existingConfig.isPresent()) {
      var config = existingConfig.get();
      if (config.isChargesEnabled() && config.isPayoutsEnabled()) {
        throw new IllegalStateException(
            "Organization already fully onboarded with " + gatewayProvider);
      }

      // Re-generate onboarding link for incomplete onboarding
      logger.info("Re-generating {} onboarding link for organization {}",
          gatewayProvider, organizationId);
      String accountLink = paymentProvider.createAccountLink(
          config.getGatewayAccountId(),
          refreshUrl,
          returnUrl
      );

      return new OnboardingLinkResponse(
          accountLink,
          config.getGatewayAccountId(),
          gatewayProvider,
          organizationId
      );
    }

    // 4. Create gateway-specific merchant account
    logger.info("Creating {} merchant account for organization {}", gatewayProvider, organizationId);
    String accountId = paymentProvider.createConnectAccount();

    // 5. Save merchant config with organization and gateway provider
    MerchantPaymentConfig config = new MerchantPaymentConfig();
    config.setTenantId(tenantId);
    config.setOrganizationId(organizationId);
    config.setGatewayProvider(gatewayProvider);
    config.setGatewayAccountId(accountId);
    config.setChargesEnabled(false);
    config.setPayoutsEnabled(false);
    repository.save(config);

    logger.info("Saved merchant config for organization {} with {} account {}",
        organizationId, gatewayProvider, accountId);

    // 6. Publish event for user service to update organization
    publishMerchantOnboardedEvent(organizationId, tenantId, accountId, gatewayProvider, false, false);

    // 7. Create onboarding link
    String accountLink = paymentProvider.createAccountLink(accountId, refreshUrl, returnUrl);

    // 8. Send email notification
    sendOnboardingEmail(organization, user, accountLink, gatewayProvider);

    logger.info("Successfully initiated {} onboarding for organization {}", gatewayProvider, organizationId);

    return new OnboardingLinkResponse(accountLink, accountId, gatewayProvider, organizationId);
  }

  /**
   * Get merchant status by organization ID.
   */
  public Optional<MerchantPaymentConfig> getMerchantStatusByOrganization(Long organizationId) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    var config = repository.findByOrganizationId(organizationId);

    // Validate tenant access
    if (config.isPresent() && !config.get().getTenantId().equals(tenantId)) {
      logger.warn("Access denied: Merchant config for organization {} does not belong to tenant {}",
          organizationId, tenantId);
      throw new AccessDeniedException("Access denied to merchant configuration");
    }

    return config;
  }

  private void publishMerchantOnboardedEvent(Long organizationId, String tenantId, String gatewayAccountId,
                                             PaymentGatewayProvider gatewayProvider,
                                             boolean chargesEnabled, boolean payoutsEnabled) {
    try {
      var event = new MerchantOnboardedEvent(organizationId, tenantId, gatewayAccountId,
          gatewayProvider, chargesEnabled, payoutsEnabled);
      eventPublisher.publishMerchantOnboarded(event);
      logger.info("Published MerchantOnboardedEvent for organization {} with {}",
          organizationId, gatewayProvider);
    } catch (final Exception e) {
      logger.error("Failed to publish MerchantOnboardedEvent for organization {}: {}",
          organizationId, e.getMessage(), e);
      // Don't fail the onboarding if event publishing fails
    }
  }

  private void sendOnboardingEmail(OrganizationDto organization, UserContext user,
                                   String onboardingUrl, PaymentGatewayProvider gatewayProvider) {
    try {
      String recipientEmail = organization.billingEmail() != null
          ? organization.billingEmail()
          : user.email();

      if (recipientEmail != null) {
        emailService.sendEmail(
            recipientEmail,
            "email.merchant.onboarding.subject",
            "merchant-onboarding",
            Map.of(
                "organizationName", organization.name(),
                "merchantName", user.firstName() != null ? user.firstName() : "Merchant",
                "onboardingUrl", onboardingUrl
            ),
            java.util.Locale.ROOT
        );
        logger.info("Sent onboarding email to {} for organization {}", recipientEmail, organization.id());
      }
    } catch (final Exception e) {
      logger.error("Failed to send onboarding email for organization {}: {}",
          organization.id(), e.getMessage(), e);
      // Don't fail the onboarding if email fails
    }
  }
}
