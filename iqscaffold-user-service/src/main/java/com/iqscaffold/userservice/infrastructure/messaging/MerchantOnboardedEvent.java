package com.iqscaffold.userservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iqscaffold.userservice.shared.PaymentGatewayProvider;

/**
 * Event published when a merchant completes payment gateway onboarding.
 * Consumed by user service to sync gateway account ID to organization.
 */
public record MerchantOnboardedEvent(
    @JsonProperty("organization_id") Long organizationId,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("gateway_account_id") String gatewayAccountId,
    @JsonProperty("gateway_provider") PaymentGatewayProvider gatewayProvider,
    @JsonProperty("charges_enabled") boolean chargesEnabled,
    @JsonProperty("payouts_enabled") boolean payoutsEnabled
) {
}
