package org.gripday.userservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating organization using Java 21 record.
 */
public record CreateOrganizationRequest(
    @NotBlank(message = "Organization name is required")
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

    Long ownerId
) {
}
