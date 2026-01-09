package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.Instant;

/**
 * Event published when merchant capabilities are updated (charges_enabled, payouts_enabled).
 * This event is consumed by the user service for notification purposes.
 */
public record MerchantCapabilitiesUpdatedEvent(
    Long organizationId,
    String tenantId,
    String stripeAccountId,
    boolean chargesEnabled,
    boolean payoutsEnabled,
    Instant timestamp
) {
  public MerchantCapabilitiesUpdatedEvent(
      final Long organizationId,
      final String tenantId,
      final String stripeAccountId,
      final boolean chargesEnabled,
      final boolean payoutsEnabled) {
    this(organizationId, tenantId, stripeAccountId, chargesEnabled, payoutsEnabled, Instant.now());
  }
}
