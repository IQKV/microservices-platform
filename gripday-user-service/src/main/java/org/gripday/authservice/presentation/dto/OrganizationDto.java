package org.gripday.authservice.presentation.dto;

import java.time.LocalDateTime;

/**
 * Organization DTO using Java 21 record.
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
    Long ownerId,
    String ownerUsername,
    String tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
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
}
