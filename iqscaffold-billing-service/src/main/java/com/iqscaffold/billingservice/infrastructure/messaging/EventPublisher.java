package com.iqscaffold.billingservice.infrastructure.messaging;

/**
 * Interface for publishing domain events to message broker.
 * Provides methods for publishing billing-related events to other services.
 */
public interface EventPublisher {

  /**
   * Publish merchant onboarded event when a new merchant completes Stripe onboarding.
   * This event is consumed by user service to sync stripe_account_id to organization.
   *
   * @param event The merchant onboarded event
   */
  void publishMerchantOnboarded(MerchantOnboardedEvent event);

  /**
   * Publish merchant capabilities updated event when Stripe account capabilities change.
   * This event is consumed by user service to sync capability status to organization.
   *
   * @param event The merchant capabilities updated event
   */
  void publishMerchantCapabilitiesUpdated(MerchantCapabilitiesUpdatedEvent event);
}
