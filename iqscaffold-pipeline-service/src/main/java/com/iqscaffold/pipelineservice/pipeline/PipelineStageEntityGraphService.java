package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for optimized pipeline stage data fetching using entity graphs.
 * Provides methods for different fetching strategies based on use case requirements.
 */
public interface PipelineStageEntityGraphService {

  /**
   * Get pipeline stage with all associated pipeline items using entity graph optimization.
   * Ideal for stage detail views and pipeline management.
   *
   * @param id the stage ID
   * @return pipeline stage with items loaded, or empty if not found
   */
  Optional<PipelineStage> getPipelineStageWithItems(Long id);

  /**
   * Get basic pipeline stage without relationships using entity graph optimization.
   * Optimized for scenarios where only basic stage data is needed.
   *
   * @param id the stage ID
   * @return basic pipeline stage, or empty if not found
   */
  Optional<PipelineStage> getBasicPipelineStage(Long id);

  /**
   * Get all active pipeline stages with their items using entity graph optimization.
   * Perfect for complete pipeline views and dashboards.
   *
   * @return list of active stages with items loaded, ordered by display order
   */
  List<PipelineStage> getActivePipelineStagesWithItems();

  /**
   * Get all pipeline stages with their items using entity graph optimization.
   * Useful for administrative views and stage management.
   *
   * @return list of all stages with items loaded, ordered by display order
   */
  List<PipelineStage> getAllPipelineStagesWithItems();
}