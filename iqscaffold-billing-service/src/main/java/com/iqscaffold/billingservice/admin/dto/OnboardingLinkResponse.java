package com.iqscaffold.billingservice.admin.dto;

/**
 * Response DTO containing Stripe onboarding link.
 */
public record OnboardingLinkResponse(
    String url,
    String stripeAccountId,
    Long organizationId
) {
}
