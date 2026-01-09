package com.iqscaffold.userservice.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating organization.
 */
public record UpdateOrganizationRequest(
    @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    String industry,

    @Size(max = 255, message = "Website must not exceed 255 characters")
    String website,

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    String phone,

    @Size(max = 500, message = "Address must not exceed 500 characters")
    String address,

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @Size(max = 100, message = "Country must not exceed 100 characters")
    String country,

    Boolean enabled,

    Long ownerUserId,

    @Email(message = "Billing email must be valid")
    @Size(max = 255, message = "Billing email must not exceed 255 characters")
    String billingEmail,

    @Size(max = 255, message = "Stripe account ID must not exceed 255 characters")
    String stripeAccountId,

    @Size(max = 50, message = "Subscription status must not exceed 50 characters")
    String subscriptionStatus,

    @Size(max = 100, message = "Subscription plan must not exceed 100 characters")
    String subscriptionPlan,

    Integer maxUsers
) {
}
