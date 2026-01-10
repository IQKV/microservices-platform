package com.iqscaffold.userservice.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.iqscaffold.userservice.shared.PaymentGatewayProvider;

/**
 * Request DTO for updating organization with payment gateway abstraction.
 * Clean greenfield implementation without backward compatibility.
 */
public record UpdateOrganizationRequest(
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
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

    @Email(message = "Billing email must be valid")
    @Size(max = 255, message = "Billing email must not exceed 255 characters")
    String billingEmail,

    @Size(max = 255, message = "Payment gateway account ID must not exceed 255 characters")
    String paymentGatewayAccountId,

    PaymentGatewayProvider paymentGatewayProvider,

    Boolean chargesEnabled,

    Boolean payoutsEnabled,

    @Size(max = 50, message = "Subscription status must not exceed 50 characters")
    String subscriptionStatus,

    @Size(max = 100, message = "Subscription plan must not exceed 100 characters")
    String subscriptionPlan,

    Integer maxUsers
) {
}
