package com.iqscaffold.leadservice.activity;

/**
 * Mapper for converting LeadActivity entities to DTOs.
 */
public final class LeadActivityMapper {

  private LeadActivityMapper() {
    // Utility class
  }

  /**
   * Converts a LeadActivity entity to a LeadActivityDto.
   *
   * @param activity the activity entity
   * @return the activity DTO
   */
  public static LeadActivityDto toDto(final LeadActivity activity) {
    if (activity == null) {
      return null;
    }

    return new LeadActivityDto(
        activity.getId(),
        activity.getLead().getId(),
        activity.getType(),
        activity.getDescription(),
        activity.getMetadata(),
        activity.getCreatedAt(),
        activity.getCreatedBy()
    );
  }
}
