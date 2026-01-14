package com.iqscaffold.pipelineservice.pipeline.dto;

import com.iqscaffold.pipelineservice.pipeline.PipelineStage;

/**
 * Mapper for converting between PipelineStage entity and DTOs.
 */
public final class PipelineStageMapper {

  private PipelineStageMapper() {
    // Utility class
  }

  /**
   * Converts a PipelineStage entity to a StageResponse DTO.
   *
   * @param stage The pipeline stage entity
   * @return The stage response DTO
   */
  public static PipelineStageDtos.StageResponse toResponse(final PipelineStage stage) {
    if (stage == null) {
      return null;
    }

    return new PipelineStageDtos.StageResponse(
        stage.getId(),
        stage.getName(),
        stage.getDescription(),
        stage.getDisplayOrder(),
        stage.getIsActive(),
        stage.getIsFinalStage(),
        stage.getColorCode(),
        stage.getCreatedAt(),
        stage.getUpdatedAt(),
        stage.getCreatedBy(),
        stage.getUpdatedBy()
    );
  }

  /**
   * Converts a CreateStageRequest DTO to a PipelineStage entity.
   *
   * @param request The create stage request
   * @param userId The user creating the stage
   * @return The pipeline stage entity
   */
  public static PipelineStage toEntity(final PipelineStageDtos.CreateStageRequest request, final String userId) {
    if (request == null) {
      return null;
    }

    PipelineStage stage = new PipelineStage();
    stage.setName(request.name());
    stage.setDescription(request.description());
    stage.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    stage.setIsActive(true);
    stage.setIsFinalStage(request.isFinalStage() != null ? request.isFinalStage() : false);
    stage.setColorCode(request.colorCode());
    stage.setCreatedBy(userId);
    stage.setUpdatedBy(userId);

    return stage;
  }

  /**
   * Updates a PipelineStage entity from an UpdateStageRequest DTO.
   *
   * @param stage The pipeline stage entity to update
   * @param request The update stage request
   * @param userId The user updating the stage
   */
  public static void updateEntity(final PipelineStage stage, final PipelineStageDtos.UpdateStageRequest request, final String userId) {
    if (stage == null || request == null) {
      return;
    }

    stage.setName(request.name());
    stage.setDescription(request.description());
    if (request.isActive() != null) {
      stage.setIsActive(request.isActive());
    }
    if (request.isFinalStage() != null) {
      stage.setIsFinalStage(request.isFinalStage());
    }
    stage.setColorCode(request.colorCode());
    stage.setUpdatedBy(userId);
  }
}
