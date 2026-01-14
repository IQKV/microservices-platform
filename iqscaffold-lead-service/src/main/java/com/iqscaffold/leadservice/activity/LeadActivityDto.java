package com.iqscaffold.leadservice.activity;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * DTO for Lead Activity responses.
 * Represents an activity log entry in the API response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeadActivityDto(
    Long id,
    Long leadId,
    ActivityType type,
    String description,
    String metadata,
    LocalDateTime createdAt,
    String createdBy
) {
}
