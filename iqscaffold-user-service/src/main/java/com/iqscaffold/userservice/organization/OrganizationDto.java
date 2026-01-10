package com.iqscaffold.userservice.organization;

import com.iqscaffold.userservice.shared.PaymentGatewayProvider;

/**
 * DTO for Organization entity with payment gateway abstraction.
 * Clean greenfield implementation without backward compatibility.
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
    String paymentGatewayAccountId,
    PaymentGatewayProvider paymentGatewayProvider,
    Boolean chargesEnabled,
    Boolean payoutsEnabled,
    String subscriptionStatus,
    String subscriptionPlan,
    Integer maxUsers
) {

  /**
   * Check if organization has a payment gateway account configured.
   */
  public boolean hasPaymentGatewayAccount() {
    return paymentGatewayAccountId != null && !paymentGatewayAccountId.isEmpty();
  }

  /**
   * Check if organization can accept payments.
   */
  public boolean canAcceptPayments() {
    return hasPaymentGatewayAccount() && Boolean.TRUE.equals(chargesEnabled);
  }

  /**
   * Check if organization can receive payouts.
   */
  public boolean canReceivePayouts() {
    return hasPaymentGatewayAccount() && Boolean.TRUE.equals(payoutsEnabled);
  }

  /**
   * Check if organization is using a specific payment gateway provider.
   */
  public boolean isUsingProvider(PaymentGatewayProvider provider) {
    return paymentGatewayProvider == provider;
  }

  /**
   * Check if organization is active.
   */
  public boolean isActive() {
    return Boolean.TRUE.equals(enabled);
  }

  /**
   * Check if organization has an active subscription.
   */
  public boolean isSubscriptionActive() {
    return "active".equalsIgnoreCase(subscriptionStatus);
  }

  /**
   * Get formatted location string.
   */
  public String getLocation() {
    if (city != null && country != null) {
      return city + ", " + country;
    }
    return city != null ? city : (country != null ? country : "");
  }
}
