package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.Instant;

/**
 * Event published when a merchant completes Stripe Connect onboarding.
 * This event is consumed by the user service to update the organization's stripe_account_id.
 */
public record MerchantOnboardedEvent(
    Long organizationId,
    String tenantId,
    String stripeAccountId,
    boolean chargesEnabled,
    boolean payoutsEnabled,
    Instant timestamp
) {
  public MerchantOnboardedEvent(final Long organizationId, final String tenantId, 
                                final String stripeAccountId, final boolean chargesEnabled, 
                                final boolean payoutsEnabled) {
    this(organizationId, tenantId, stripeAccountId, chargesEnabled, payoutsEnabled, Instant.now());
  }
}
