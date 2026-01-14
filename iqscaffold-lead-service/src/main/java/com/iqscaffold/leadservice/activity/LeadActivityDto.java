package com.iqscaffold.leadservice.activity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

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
