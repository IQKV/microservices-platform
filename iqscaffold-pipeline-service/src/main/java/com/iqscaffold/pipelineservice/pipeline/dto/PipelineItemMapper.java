package com.iqscaffold.pipelineservice.pipeline.dto;

import com.iqscaffold.pipelineservice.pipeline.PipelineItem;

/**
 * Mapper for converting between PipelineItem entities and DTOs.
 */
public final class PipelineItemMapper {

  private PipelineItemMapper() {
    // Utility class
  }

  /**
   * Converts a PipelineItem entity to a response DTO.
   *
   * @param item The pipeline item entity
   * @return The response DTO
   */
  public static PipelineItemDtos.PipelineItemResponse toResponse(final PipelineItem item) {
    if (item == null) {
      return null;
    }

    return new PipelineItemDtos.PipelineItemResponse(
        item.getId(),
        item.getLeadId(),
        item.getStageId(),
        item.getExpectedValue(),
        item.getProbability(),
        item.getEnteredStageAt(),
        item.getDaysInStage(),
        item.getConvertedAt(),
        item.getCreatedAt(),
        item.getUpdatedAt(),
        item.getCreatedBy(),
        item.getUpdatedBy()
    );
  }
}
