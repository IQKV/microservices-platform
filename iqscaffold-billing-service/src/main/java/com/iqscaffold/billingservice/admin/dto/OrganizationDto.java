package com.iqscaffold.billingservice.admin.dto;

/**
 * DTO for Organization from User Service.
 * Minimal representation for billing service needs.
 */
public record OrganizationDto(
    Long id,
    String name,
    String tenantId,
    String stripeAccountId,
    String subscriptionStatus,
    String subscriptionPlan,
    String billingEmail,
    Integer maxUsers,
    Boolean enabled
) {
  public boolean isActive() {
    return Boolean.TRUE.equals(enabled);
  }

  public boolean hasStripeAccount() {
    return stripeAccountId != null && !stripeAccountId.isEmpty();
  }
}
