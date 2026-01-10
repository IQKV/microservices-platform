package com.iqscaffold.billingservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;

/**
 * Event published when merchant payment gateway account capabilities are updated.
 * Published by billing service to notify user service of capability changes.
 */
public record MerchantCapabilitiesUpdatedEvent(
    @JsonProperty("organization_id") Long organizationId,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("gateway_account_id") String gatewayAccountId,
    @JsonProperty("gateway_provider") PaymentGatewayProvider gatewayProvider,
    @JsonProperty("charges_enabled") boolean chargesEnabled,
    @JsonProperty("payouts_enabled") boolean payoutsEnabled
) {
}
