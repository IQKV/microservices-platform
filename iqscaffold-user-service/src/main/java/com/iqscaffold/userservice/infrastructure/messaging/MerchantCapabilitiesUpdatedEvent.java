package com.iqscaffold.userservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when merchant Stripe account capabilities are updated.
 * Consumed by user service to sync capability status to organization.
 */
public record MerchantCapabilitiesUpdatedEvent(
    @JsonProperty("organization_id") Long organizationId,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("stripe_account_id") String stripeAccountId,
    @JsonProperty("charges_enabled") boolean chargesEnabled,
    @JsonProperty("payouts_enabled") boolean payoutsEnabled
) {
}
