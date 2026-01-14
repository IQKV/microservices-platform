package com.iqscaffold.pipelineservice.pipeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTOs for Pipeline Item operations.
 */
public final class PipelineItemDtos {

  private PipelineItemDtos() {
    // Utility class
  }

  /**
   * Request DTO for moving a pipeline item to a different stage.
   */
  @Schema(description = "Request to move a pipeline item to a different stage")
  public record MoveToStageRequest(
      @NotNull(message = "Stage ID is required")
      @Min(value = 1, message = "Stage ID must be positive")
      @Schema(description = "ID of the target stage", example = "3")
      Long stageId
  ) {
  }

  /**
   * Response DTO for pipeline item.
   */
  @Schema(description = "Pipeline item response")
  public record PipelineItemResponse(
      @Schema(description = "Unique identifier of the pipeline item", example = "1")
      Long id,

      @Schema(description = "ID of the lead associated with this pipeline item", example = "42")
      Long leadId,

      @Schema(description = "ID of the current stage", example = "2")
      Long stageId,

      @Schema(description = "Expected value/revenue from this lead", example = "5000.00")
      BigDecimal expectedValue,

      @Schema(description = "Probability of conversion (0-100)", example = "75.50")
      BigDecimal probability,

      @Schema(description = "Timestamp when the lead entered the current stage")
      LocalDateTime enteredStageAt,

      @Schema(description = "Number of days the lead has been in the current stage", example = "5")
      Integer daysInStage,

      @Schema(description = "Timestamp when the lead was converted (Won stage)")
      LocalDateTime convertedAt,

      @Schema(description = "Timestamp when the pipeline item was created")
      LocalDateTime createdAt,

      @Schema(description = "Timestamp when the pipeline item was last updated")
      LocalDateTime updatedAt,

      @Schema(description = "User who created the pipeline item")
      String createdBy,

      @Schema(description = "User who last updated the pipeline item")
      String updatedBy
  ) {
  }
}
