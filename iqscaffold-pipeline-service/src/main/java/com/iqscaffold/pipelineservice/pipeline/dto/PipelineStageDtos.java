package com.iqscaffold.pipelineservice.pipeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTOs for Pipeline Stage operations.
 */
public final class PipelineStageDtos {

  private PipelineStageDtos() {
    // Utility class
  }

  /**
   * Request DTO for creating a new pipeline stage.
   */
  @Schema(description = "Request to create a new pipeline stage")
  public record CreateStageRequest(
      @NotBlank(message = "Stage name is required")
      @Size(max = 100, message = "Stage name must not exceed 100 characters")
      @Schema(description = "Name of the pipeline stage", example = "Qualified")
      String name,

      @Size(max = 500, message = "Description must not exceed 500 characters")
      @Schema(description = "Description of the pipeline stage", example = "Lead has been qualified and meets criteria")
      String description,

      @Min(value = 0, message = "Display order must be non-negative")
      @Schema(description = "Display order of the stage", example = "3")
      Integer displayOrder,

      @Schema(description = "Whether this is a final stage (Won/Lost)", example = "false")
      Boolean isFinalStage,

      @Size(max = 20, message = "Color code must not exceed 20 characters")
      @Schema(description = "Color code for UI display", example = "#4CAF50")
      String colorCode
  ) {
  }

  /**
   * Request DTO for updating an existing pipeline stage.
   */
  @Schema(description = "Request to update an existing pipeline stage")
  public record UpdateStageRequest(
      @NotBlank(message = "Stage name is required")
      @Size(max = 100, message = "Stage name must not exceed 100 characters")
      @Schema(description = "Name of the pipeline stage", example = "Qualified")
      String name,

      @Size(max = 500, message = "Description must not exceed 500 characters")
      @Schema(description = "Description of the pipeline stage", example = "Lead has been qualified and meets criteria")
      String description,

      @Schema(description = "Whether the stage is active", example = "true")
      Boolean isActive,

      @Schema(description = "Whether this is a final stage (Won/Lost)", example = "false")
      Boolean isFinalStage,

      @Size(max = 20, message = "Color code must not exceed 20 characters")
      @Schema(description = "Color code for UI display", example = "#4CAF50")
      String colorCode
  ) {
  }

  /**
   * Request DTO for reordering a pipeline stage.
   */
  @Schema(description = "Request to reorder a pipeline stage")
  public record ReorderStageRequest(
      @Min(value = 0, message = "Display order must be non-negative")
      @Schema(description = "New display order for the stage", example = "5")
      Integer displayOrder
  ) {
  }

  /**
   * Response DTO for pipeline stage.
   */
  @Schema(description = "Pipeline stage response")
  public record StageResponse(
      @Schema(description = "Unique identifier of the stage", example = "1")
      Long id,

      @Schema(description = "Name of the pipeline stage", example = "New")
      String name,

      @Schema(description = "Description of the pipeline stage", example = "Newly created lead")
      String description,

      @Schema(description = "Display order of the stage", example = "1")
      Integer displayOrder,

      @Schema(description = "Whether the stage is active", example = "true")
      Boolean isActive,

      @Schema(description = "Whether this is a final stage (Won/Lost)", example = "false")
      Boolean isFinalStage,

      @Schema(description = "Color code for UI display", example = "#2196F3")
      String colorCode,

      @Schema(description = "Timestamp when the stage was created")
      LocalDateTime createdAt,

      @Schema(description = "Timestamp when the stage was last updated")
      LocalDateTime updatedAt,

      @Schema(description = "User who created the stage")
      String createdBy,

      @Schema(description = "User who last updated the stage")
      String updatedBy
  ) {
  }
}
