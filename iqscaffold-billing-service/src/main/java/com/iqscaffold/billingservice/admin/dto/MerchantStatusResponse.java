package com.iqscaffold.billingservice.admin.dto;

import java.math.BigDecimal;

/**
 * Response DTO for merchant status.
 */
public record MerchantStatusResponse(
    String stripeAccountId,
    Long organizationId,
    String tenantId,
    boolean chargesEnabled,
    boolean payoutsEnabled,
    BigDecimal applicationFeePercent
) {
}
