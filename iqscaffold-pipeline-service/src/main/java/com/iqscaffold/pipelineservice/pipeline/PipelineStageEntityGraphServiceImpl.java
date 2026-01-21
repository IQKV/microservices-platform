package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PipelineStageEntityGraphService providing optimized data fetching
 * using entity graphs for different use case scenarios.
 */
@Service
@Transactional(readOnly = true)
public class PipelineStageEntityGraphServiceImpl implements PipelineStageEntityGraphService {

  private final PipelineStageRepository pipelineStageRepository;

  public PipelineStageEntityGraphServiceImpl(final PipelineStageRepository pipelineStageRepository) {
    this.pipelineStageRepository = pipelineStageRepository;
  }

  @Override
  public Optional<PipelineStage> getPipelineStageWithItems(final Long id) {
    return pipelineStageRepository.findWithItemsById(id);
  }

  @Override
  public Optional<PipelineStage> getBasicPipelineStage(final Long id) {
    return pipelineStageRepository.findBasicById(id);
  }

  @Override
  public List<PipelineStage> getActivePipelineStagesWithItems() {
    return pipelineStageRepository.findWithItemsByIsActiveTrueOrderByDisplayOrderAsc();
  }

  @Override
  public List<PipelineStage> getAllPipelineStagesWithItems() {
    return pipelineStageRepository.findWithItemsAllByOrderByDisplayOrderAsc();
  }
}