package com.iqscaffold.billingservice.webhook;

import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.infrastructure.messaging.EventPublisher;
import com.iqscaffold.billingservice.infrastructure.messaging.MerchantCapabilitiesUpdatedEvent;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles merchant account-related webhook events from all payment providers.
 * <p>
 * This handler processes normalized account events (updated, verified, etc.)
 * and updates the local merchant configuration accordingly. It works with the abstract
 * {@link WebhookEvent} structure, making it provider-independent.
 * <p>
 * Supported Event Types:
 * <ul>
 *   <li>{@link WebhookEvent.EventType#ACCOUNT_UPDATED} - Merchant account capabilities changed</li>
 * </ul>
 * <p>
 * When merchant capabilities change (charges_enabled, payouts_enabled), this handler:
 * <ol>
 *   <li>Updates the local merchant configuration</li>
 *   <li>Publishes a domain event for cross-service synchronization</li>
 * </ol>
 */
@Component
public class AccountWebhookEventHandler implements WebhookEventHandler {
  private static final Logger logger = LoggerFactory.getLogger(AccountWebhookEventHandler.class);

  private final MerchantStripeConfigRepository merchantConfigRepository;
  private final EventPublisher eventPublisher;

  public AccountWebhookEventHandler(
      final MerchantStripeConfigRepository merchantConfigRepository,
      final EventPublisher eventPublisher) {
    this.merchantConfigRepository = merchantConfigRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public boolean supports(WebhookEvent event) {
    return event.isAccountEvent();
  }

  @Override
  public void handleEvent(WebhookEvent event) {
    if (!supports(event)) {
      throw new IllegalArgumentException("Event type not supported by AccountWebhookEventHandler: " + event.eventType());
    }

    logger.info("Processing account webhook event: type={}, resourceId={}, provider={}",
        event.eventType(), event.resourceId(), event.provider());

    if (WebhookEvent.EventType.ACCOUNT_UPDATED.equals(event.eventType())) {
      handleAccountUpdated(event);
    } else {
      logger.warn("Unhandled account event type: {}", event.eventType());
    }
  }

  private void handleAccountUpdated(WebhookEvent event) {
    // Currently only Stripe accounts are supported
    // For other providers, add handling here based on event.provider()
    if (event.provider() == PaymentGatewayProvider.STRIPE && event.rawPayload() instanceof com.stripe.model.Account account) {
      handleStripeAccountUpdated(account);
    } else {
      logger.warn("Unsupported account update payload: provider={}, type={}",
          event.provider(), event.rawPayload().getClass().getName());
    }
  }

  private void handleStripeAccountUpdated(com.stripe.model.Account account) {
    String accountId = account.getId();
    var configOpt = merchantConfigRepository.findByStripeAccountId(accountId);

    if (configOpt.isEmpty()) {
      logger.warn("Received account update for unknown account: {}", accountId);
      return;
    }

    var merchant = configOpt.get();
    boolean chargesEnabled = Boolean.TRUE.equals(account.getChargesEnabled());
    boolean payoutsEnabled = Boolean.TRUE.equals(account.getPayoutsEnabled());

    merchant.setChargesEnabled(chargesEnabled);
    merchant.setPayoutsEnabled(payoutsEnabled);
    merchantConfigRepository.save(merchant);
    logger.info("Updated merchant capabilities for account: {}", accountId);

    // Publish event to sync capabilities to user service
    if (merchant.getOrganizationId() != null) {
      var capabilitiesEvent = new MerchantCapabilitiesUpdatedEvent(
          merchant.getOrganizationId(),
          merchant.getTenantId(),
          accountId,
          PaymentGatewayProvider.STRIPE,
          chargesEnabled,
          payoutsEnabled
      );
      eventPublisher.publishMerchantCapabilitiesUpdated(capabilitiesEvent);
      logger.info("Published capabilities update event for organization: {}", merchant.getOrganizationId());
    }
  }
}
