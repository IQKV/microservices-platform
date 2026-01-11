package com.iqscaffold.billingservice.admin.dto;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;

/**
 * Response DTO containing payment gateway onboarding link.
 * Supports multiple payment providers (Stripe, PayPal, Square, etc.).
 */
public record OnboardingLinkResponse(
    String url,
    String gatewayAccountId,
    PaymentGatewayProvider gatewayProvider,
    Long organizationId
) {
}
