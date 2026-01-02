package com.iqscaffold.billingservice.admin;

import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.security.UserContext;
import java.util.Map;
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
      EmailService emailService
  ) {
    this.repository = repository;
    this.paymentProvider = paymentProvider;
    this.emailService = emailService;
  }

  @Transactional
  public String initiateOnboarding(String refreshUrl, String returnUrl) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    UserContext user = SecurityContextHelper.getCurrentUserContextOrThrow();

    MerchantStripeConfig config = repository.findByTenantId(tenantId)
        .orElseGet(() -> createNewConfig(tenantId));

    if (config.getStripeAccountId() == null) {
      String accountId = paymentProvider.createConnectAccount();
      config.setStripeAccountId(accountId);
      repository.save(config);
    }

    String accountLink = paymentProvider.createAccountLink(config.getStripeAccountId(), refreshUrl, returnUrl);

    // Send email notification
    if (user.email() != null) {
        emailService.sendEmail(
            user.email(),
            "email.merchant.onboarding.subject",
            "merchant-onboarding",
            Map.of(
                "merchantName", user.firstName() != null ? user.firstName() : "Merchant",
                "onboardingUrl", accountLink
            ),
            java.util.Locale.ROOT // Should come from UserContext or request
        );
    }

    return accountLink;
  }

  private MerchantStripeConfig createNewConfig(String tenantId) {
    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId(tenantId);
    return repository.save(config);
  }
}
