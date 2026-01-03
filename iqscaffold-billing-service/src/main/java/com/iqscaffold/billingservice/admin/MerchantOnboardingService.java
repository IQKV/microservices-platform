package com.iqscaffold.billingservice.admin;

import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.security.UserContext;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantOnboardingService {

  private final MerchantStripeConfigRepository repository;
  private final PaymentProviderAdapter paymentProvider;
  private final EmailService emailService;

  public MerchantOnboardingService(
      MerchantStripeConfigRepository repository,
      PaymentProviderAdapter paymentProvider,
      EmailService emailService) {
    this.repository = repository;
    this.paymentProvider = paymentProvider;
    this.emailService = emailService;
  }

  @Transactional
  public String initiateOnboarding(String refreshUrl, String returnUrl) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    UserContext user = SecurityContextHelper.getCurrentUserContextOrThrow();

    // Check if config exists
    Optional<MerchantStripeConfig> existingConfig = repository.findByTenantId(tenantId);

    if (existingConfig.isPresent() && existingConfig.get().isChargesEnabled()
        && existingConfig.get().isPayoutsEnabled()) {
      throw new IllegalStateException("Merchant already fully onboarded");
    }

    String accountId;
    if (existingConfig.isPresent()) {
      accountId = existingConfig.get().getStripeAccountId();
    } else {
      // Create Stripe Connect Account
      accountId = paymentProvider.createConnectAccount();

      // Save local config
      MerchantStripeConfig config = new MerchantStripeConfig();
      config.setTenantId(tenantId);
      config.setStripeAccountId(accountId);
      config.setChargesEnabled(false);
      config.setPayoutsEnabled(false);
      repository.save(config);
    }

    // Create Account Link
    String accountLink = paymentProvider.createAccountLink(accountId, refreshUrl, returnUrl);

    // Send email notification
    if (user.email() != null) {
      emailService.sendEmail(
          user.email(),
          "email.merchant.onboarding.subject",
          "merchant-onboarding",
          Map.of(
              "merchantName", user.firstName() != null ? user.firstName() : "Merchant",
              "onboardingUrl", accountLink),
          java.util.Locale.ROOT // Should come from UserContext or request
      );
    }

    return accountLink;
  }

  public Optional<MerchantStripeConfig> getMerchantStatus() {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    return repository.findByTenantId(tenantId);
  }

  private MerchantStripeConfig createNewConfig(String tenantId) {
    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId(tenantId);
    return repository.save(config);
  }
}
