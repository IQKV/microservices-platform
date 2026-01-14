package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PipelineItemService {

  PipelineItem addLeadToPipeline(PipelineItem item);

  Optional<PipelineItem> getPipelineItemById(Long id);

  Optional<PipelineItem> getPipelineItemByLeadId(Long leadId);

  Page<PipelineItem> getAllPipelineItems(Pageable pageable);

  Page<PipelineItem> getPipelineItemsByStage(Long stageId, Pageable pageable);

  List<PipelineItem> getPipelineItemsByStage(Long stageId);

  PipelineItem updatePipelineItem(Long id, PipelineItem item);

  PipelineItem moveToStage(Long id, Long newStageId);

  void removeFromPipeline(Long id);

  long countByStage(Long stageId);

  boolean existsByLeadId(Long leadId);
}
