package com.iqscaffold.pipelineservice.followup.dto;

import com.iqscaffold.pipelineservice.followup.FollowUpPriority;
import com.iqscaffold.pipelineservice.followup.FollowUpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTOs for Follow-Up operations.
 */
public final class FollowUpDtos {

  private FollowUpDtos() {
    // Utility class
  }

  /**
   * Request DTO for creating a follow-up.
   */
  @Schema(description = "Request to schedule a follow-up")
  public record CreateFollowUpRequest(
      @NotNull(message = "Lead ID is required")
      @Schema(description = "ID of the lead for this follow-up", example = "42")
      Long leadId,

      @NotBlank(message = "Title is required")
      @Size(max = 200, message = "Title must not exceed 200 characters")
      @Schema(description = "Title of the follow-up", example = "Call to discuss proposal")
      String title,

      @Size(max = 1000, message = "Description must not exceed 1000 characters")
      @Schema(description = "Detailed description of the follow-up", example = "Follow up on the proposal sent last week")
      String description,

      @NotNull(message = "Due date is required")
      @Schema(description = "Due date and time for the follow-up", example = "2026-01-20T14:30:00")
      LocalDateTime dueDate,

      @Schema(description = "Priority level", example = "HIGH")
      FollowUpPriority priority,

      @Schema(description = "User assigned to this follow-up", example = "user123")
      String assignedTo
  ) {
  }

  /**
   * Request DTO for updating a follow-up.
   */
  @Schema(description = "Request to update a follow-up")
  public record UpdateFollowUpRequest(
      @Size(max = 200, message = "Title must not exceed 200 characters")
      @Schema(description = "Title of the follow-up", example = "Call to discuss proposal")
      String title,

      @Size(max = 1000, message = "Description must not exceed 1000 characters")
      @Schema(description = "Detailed description of the follow-up", example = "Follow up on the proposal sent last week")
      String description,

      @Schema(description = "Due date and time for the follow-up", example = "2026-01-20T14:30:00")
      LocalDateTime dueDate,

      @Schema(description = "Priority level", example = "HIGH")
      FollowUpPriority priority,

      @Schema(description = "User assigned to this follow-up", example = "user123")
      String assignedTo
  ) {
  }

  /**
   * Response DTO for follow-up.
   */
  @Schema(description = "Follow-up response")
  public record FollowUpResponse(
      @Schema(description = "Unique identifier of the follow-up", example = "1")
      Long id,

      @Schema(description = "ID of the lead associated with this follow-up", example = "42")
      Long leadId,

      @Schema(description = "Title of the follow-up", example = "Call to discuss proposal")
      String title,

      @Schema(description = "Detailed description of the follow-up")
      String description,

      @Schema(description = "Due date and time for the follow-up")
      LocalDateTime dueDate,

      @Schema(description = "Priority level", example = "HIGH")
      FollowUpPriority priority,

      @Schema(description = "Current status", example = "PENDING")
      FollowUpStatus status,

      @Schema(description = "Timestamp when the follow-up was completed")
      LocalDateTime completedAt,

      @Schema(description = "User assigned to this follow-up")
      String assignedTo,

      @Schema(description = "Indicates if the follow-up is overdue", example = "false")
      boolean overdue,

      @Schema(description = "Timestamp when the follow-up was created")
      LocalDateTime createdAt,

      @Schema(description = "Timestamp when the follow-up was last updated")
      LocalDateTime updatedAt,

      @Schema(description = "User who created the follow-up")
      String createdBy,

      @Schema(description = "User who last updated the follow-up")
      String updatedBy
  ) {
  }
}
