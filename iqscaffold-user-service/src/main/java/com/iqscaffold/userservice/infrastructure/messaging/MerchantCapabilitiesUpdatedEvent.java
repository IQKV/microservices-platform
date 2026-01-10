package com.iqscaffold.userservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iqscaffold.userservice.shared.PaymentGatewayProvider;

/**
 * Event published when merchant payment gateway account capabilities are updated.
 * Consumed by user service to sync capability status to organization.
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
