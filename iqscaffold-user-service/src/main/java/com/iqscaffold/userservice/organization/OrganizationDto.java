package com.iqscaffold.userservice.organization;

import java.time.LocalDateTime;

/**
 * Organization DTO for system-wide organization data (public schema).
 */
public record OrganizationDto(
    Long id,
    String name,
    String description,
    String industry,
    String website,
    String phone,
    String address,
    String city,
    String country,
    Boolean enabled,
    String tenantId,
    Long ownerUserId,
    String billingEmail,
    String stripeAccountId,
    Boolean chargesEnabled,
    Boolean payoutsEnabled,
    String subscriptionStatus,
    String subscriptionPlan,
    Integer maxUsers,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy
) {

  public boolean isActive() {
    return Boolean.TRUE.equals(enabled);
  }

  public String getLocation() {
    if (city != null && country != null) {
      return city + ", " + country;
    }
    return city != null ? city : (country != null ? country : "");
  }

  public boolean hasSubscription() {
    return subscriptionStatus != null && !subscriptionStatus.isEmpty();
  }

  public boolean isSubscriptionActive() {
    return "active".equalsIgnoreCase(subscriptionStatus);
  }

  public boolean hasStripeAccount() {
    return stripeAccountId != null && !stripeAccountId.isEmpty();
  }

  public boolean canAcceptPayments() {
    return hasStripeAccount() && Boolean.TRUE.equals(chargesEnabled);
  }

  public boolean canReceivePayouts() {
    return hasStripeAccount() && Boolean.TRUE.equals(payoutsEnabled);
  }
}
