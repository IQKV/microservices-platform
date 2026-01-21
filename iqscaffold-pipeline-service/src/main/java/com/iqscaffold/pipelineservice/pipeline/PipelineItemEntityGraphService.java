package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for optimized pipeline item data fetching using entity graphs.
 * Provides methods for different fetching strategies based on use case requirements.
 */
public interface PipelineItemEntityGraphService {

  /**
   * Get pipeline item with stage information using entity graph optimization.
   * Ideal for pipeline views where stage details are needed.
   *
   * @param id the pipeline item ID
   * @return pipeline item with stage loaded, or empty if not found
   */
  Optional<PipelineItem> getPipelineItemWithStage(Long id);

  /**
   * Get pipeline item with stage information by lead ID using entity graph optimization.
   * Perfect for lead detail views showing pipeline position.
   *
   * @param leadId the lead ID
   * @return pipeline item with stage loaded, or empty if not found
   */
  Optional<PipelineItem> getPipelineItemWithStageByLeadId(Long leadId);

  /**
   * Get basic pipeline item without relationships using entity graph optimization.
   * Optimized for scenarios where only basic item data is needed.
   *
   * @param id the pipeline item ID
   * @return basic pipeline item, or empty if not found
   */
  Optional<PipelineItem> getBasicPipelineItem(Long id);

  /**
   * Get basic pipeline item by lead ID without relationships using entity graph optimization.
   * Optimized for scenarios where only basic item data is needed.
   *
   * @param leadId the lead ID
   * @return basic pipeline item, or empty if not found
   */
  Optional<PipelineItem> getBasicPipelineItemByLeadId(Long leadId);

  /**
   * Get pipeline items with stage information for a specific stage using entity graph optimization.
   * Ideal for stage-based pipeline views.
   *
   * @param stageId the stage ID
   * @param pageable pagination information
   * @return page of pipeline items with stages loaded
   */
  Page<PipelineItem> getPipelineItemsWithStageByStage(Long stageId, Pageable pageable);

  /**
   * Get all pipeline items with stage information for a specific stage using entity graph optimization.
   * Useful for stage statistics and reporting.
   *
   * @param stageId the stage ID
   * @return list of pipeline items with stages loaded
   */
  List<PipelineItem> getAllPipelineItemsWithStageByStage(Long stageId);
}