package com.iqscaffold.userservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when a merchant completes Stripe onboarding.
 * Consumed by user service to sync stripe_account_id to organization.
 */
public record MerchantOnboardedEvent(
    @JsonProperty("organization_id") Long organizationId,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("stripe_account_id") String stripeAccountId,
    @JsonProperty("charges_enabled") boolean chargesEnabled,
    @JsonProperty("payouts_enabled") boolean payoutsEnabled
) {
}
