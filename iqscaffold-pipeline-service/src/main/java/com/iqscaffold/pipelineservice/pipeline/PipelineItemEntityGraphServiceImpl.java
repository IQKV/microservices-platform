package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PipelineItemEntityGraphService providing optimized data fetching
 * using entity graphs for different use case scenarios.
 */
@Service
@Transactional(readOnly = true)
public class PipelineItemEntityGraphServiceImpl implements PipelineItemEntityGraphService {

  private final PipelineItemRepository pipelineItemRepository;

  public PipelineItemEntityGraphServiceImpl(final PipelineItemRepository pipelineItemRepository) {
    this.pipelineItemRepository = pipelineItemRepository;
  }

  @Override
  public Optional<PipelineItem> getPipelineItemWithStage(final Long id) {
    return pipelineItemRepository.findWithStageById(id);
  }

  @Override
  public Optional<PipelineItem> getPipelineItemWithStageByLeadId(final Long leadId) {
    return pipelineItemRepository.findWithStageByLeadId(leadId);
  }

  @Override
  public Optional<PipelineItem> getBasicPipelineItem(final Long id) {
    return pipelineItemRepository.findBasicById(id);
  }

  @Override
  public Optional<PipelineItem> getBasicPipelineItemByLeadId(final Long leadId) {
    return pipelineItemRepository.findBasicByLeadId(leadId);
  }

  @Override
  public Page<PipelineItem> getPipelineItemsWithStageByStage(final Long stageId, final Pageable pageable) {
    return pipelineItemRepository.findWithStageByStageId(stageId, pageable);
  }

  @Override
  public List<PipelineItem> getAllPipelineItemsWithStageByStage(final Long stageId) {
    return pipelineItemRepository.findWithStageByStageId(stageId);
  }
}