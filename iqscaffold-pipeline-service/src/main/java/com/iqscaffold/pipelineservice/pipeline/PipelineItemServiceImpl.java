package com.iqscaffold.pipelineservice.pipeline;

import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for pipeline item operations.
 */
@Service
@Transactional
public class PipelineItemServiceImpl implements PipelineItemService {

  private final PipelineItemRepository pipelineItemRepository;
  private final PipelineStageRepository pipelineStageRepository;

  public PipelineItemServiceImpl(
      final PipelineItemRepository pipelineItemRepository,
      final PipelineStageRepository pipelineStageRepository) {
    this.pipelineItemRepository = pipelineItemRepository;
    this.pipelineStageRepository = pipelineStageRepository;
  }

  @Override
  public PipelineItem addLeadToPipeline(final PipelineItem item) {
    return pipelineItemRepository.save(item);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PipelineItem> getPipelineItemById(final Long id) {
    return pipelineItemRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PipelineItem> getPipelineItemByLeadId(final Long leadId) {
    return pipelineItemRepository.findByLeadId(leadId);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PipelineItem> getAllPipelineItems(final Pageable pageable) {
    return pipelineItemRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PipelineItem> getPipelineItemsByStage(final Long stageId, final Pageable pageable) {
    return pipelineItemRepository.findByStageId(stageId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PipelineItem> getPipelineItemsByStage(final Long stageId) {
    return pipelineItemRepository.findByStageId(stageId);
  }

  @Override
  public PipelineItem updatePipelineItem(final Long id, final PipelineItem item) {
    PipelineItem existing = pipelineItemRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline item", "id", id));

    // Update fields
    if (item.getExpectedValue() != null) {
      existing.setExpectedValue(item.getExpectedValue());
    }
    if (item.getProbability() != null) {
      existing.setProbability(item.getProbability());
    }
    if (item.getUpdatedBy() != null) {
      existing.setUpdatedBy(item.getUpdatedBy());
    }

    return pipelineItemRepository.save(existing);
  }

  @Override
  public PipelineItem moveToStage(final Long id, final Long newStageId) {
    // Validate that the pipeline item exists
    PipelineItem item = pipelineItemRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline item", "id", id));

    // Validate that the target stage exists
    PipelineStage newStage = pipelineStageRepository.findById(newStageId)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage", "id", newStageId));

    // Update the stage
    item.setStageId(newStageId);
    item.setEnteredStageAt(LocalDateTime.now());
    item.setDaysInStage(0);

    return pipelineItemRepository.save(item);
  }

  @Override
  public void removeFromPipeline(final Long id) {
    if (!pipelineItemRepository.existsById(id)) {
      throw new ResourceNotFoundException("Pipeline item", "id", id);
    }
    pipelineItemRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public long countByStage(final Long stageId) {
    return pipelineItemRepository.countByStageId(stageId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByLeadId(final Long leadId) {
    return pipelineItemRepository.existsByLeadId(leadId);
  }
}
