package com.iqscaffold.pipelineservice.pipeline.dto;

import java.time.LocalDateTime;

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

  /**
   * Converts a create request DTO to a PipelineItem entity.
   *
   * @param request   The create request DTO
   * @param stageId   The stage ID to use (from request or default)
   * @param createdBy The user creating the item
   * @return The pipeline item entity
   */
  public static PipelineItem toEntity(
      final PipelineItemDtos.CreatePipelineItemRequest request,
      final Long stageId,
      final String createdBy) {

    if (request == null) {
      return null;
    }

    PipelineItem item = new PipelineItem();
    item.setLeadId(request.leadId());
    item.setStageId(stageId);
    item.setExpectedValue(request.expectedValue());
    item.setProbability(request.probability());
    item.setEnteredStageAt(LocalDateTime.now());
    item.setDaysInStage(0);
    item.setCreatedBy(createdBy);
    item.setUpdatedBy(createdBy);

    return item;
  }
}
