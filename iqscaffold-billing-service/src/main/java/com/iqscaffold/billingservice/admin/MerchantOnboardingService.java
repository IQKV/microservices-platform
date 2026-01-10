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
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.security.UserContext;
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

  private final MerchantStripeConfigRepository repository;
  private final PaymentProviderAdapter paymentProvider;
  private final EmailService emailService;
  private final UserServiceClient userServiceClient;
  private final EventPublisher eventPublisher;

  public MerchantOnboardingService(
      final MerchantStripeConfigRepository repository,
      final PaymentProviderAdapter paymentProvider,
      final EmailService emailService,
      final UserServiceClient userServiceClient,
      final EventPublisher eventPublisher) {
    this.repository = repository;
    this.paymentProvider = paymentProvider;
    this.emailService = emailService;
    this.userServiceClient = userServiceClient;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Initiates merchant onboarding for an organization.
   *
   * @param organizationId Organization ID from user service
   * @param refreshUrl URL to redirect if onboarding link expires
   * @param returnUrl URL to redirect after onboarding completion
   * @return Onboarding link response with URL and account details
   */
  @Transactional
  public OnboardingLinkResponse initiateOnboarding(Long organizationId, String refreshUrl, String returnUrl) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    UserContext user = SecurityContextHelper.getCurrentUserContextOrThrow();

    logger.info("Initiating merchant onboarding for organization {} by user {}", organizationId, user.userId());

    // 1. Validate organization exists and belongs to tenant
    OrganizationDto organization = userServiceClient.getOrganization(organizationId)
        .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

    if (!organization.tenantId().equals(tenantId)) {
      logger.warn("Access denied: Organization {} does not belong to tenant {}", organizationId, tenantId);
      throw new AccessDeniedException("Organization does not belong to current tenant");
    }

    if (!organization.isActive()) {
      throw new IllegalStateException("Organization is not active: " + organizationId);
    }

    // 2. Check if already onboarded
    if (repository.existsByOrganizationId(organizationId)) {
      var existingConfig = repository.findByOrganizationId(organizationId).get();
      if (existingConfig.isChargesEnabled() && existingConfig.isPayoutsEnabled()) {
        throw new IllegalStateException("Organization already fully onboarded");
      }
      
      // Re-generate onboarding link for incomplete onboarding
      logger.info("Re-generating onboarding link for organization {}", organizationId);
      String accountLink = paymentProvider.createAccountLink(
          existingConfig.getStripeAccountId(), 
          refreshUrl, 
          returnUrl
      );
      
      return new OnboardingLinkResponse(
          accountLink,
          existingConfig.getStripeAccountId(),
          organizationId
      );
    }

    // 3. Create Stripe Connect Account
    logger.info("Creating Stripe Connect account for organization {}", organizationId);
    String accountId = paymentProvider.createConnectAccount();

    // 4. Save merchant config with organization link
    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId(tenantId);
    config.setOrganizationId(organizationId);
    config.setStripeAccountId(accountId);
    config.setChargesEnabled(false);
    config.setPayoutsEnabled(false);
    repository.save(config);

    logger.info("Saved merchant config for organization {} with Stripe account {}", organizationId, accountId);

    // 5. Publish event for user service to update organization
    publishMerchantOnboardedEvent(organizationId, tenantId, accountId, false, false);

    // 6. Create onboarding link
    String accountLink = paymentProvider.createAccountLink(accountId, refreshUrl, returnUrl);

    // 7. Send email notification
    sendOnboardingEmail(organization, user, accountLink);

    logger.info("Successfully initiated onboarding for organization {}", organizationId);

    return new OnboardingLinkResponse(accountLink, accountId, organizationId);
  }

  /**
   * Get merchant status by organization ID.
   */
  public Optional<MerchantStripeConfig> getMerchantStatusByOrganization(Long organizationId) {
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

  private void publishMerchantOnboardedEvent(Long organizationId, String tenantId, String stripeAccountId, 
                                             boolean chargesEnabled, boolean payoutsEnabled) {
    try {
      var event = new MerchantOnboardedEvent(organizationId, tenantId, stripeAccountId, 
          com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE, chargesEnabled, payoutsEnabled);
      eventPublisher.publishMerchantOnboarded(event);
      logger.info("Published MerchantOnboardedEvent for organization {}", organizationId);
    } catch (final Exception e) {
      logger.error("Failed to publish MerchantOnboardedEvent for organization {}: {}", 
          organizationId, e.getMessage(), e);
      // Don't fail the onboarding if event publishing fails
    }
  }

  private void sendOnboardingEmail(OrganizationDto organization, UserContext user, String onboardingUrl) {
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
