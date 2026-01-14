package com.iqscaffold.pipelineservice.pipeline;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.pipelineservice.config.RabbitMQConfig;
import com.iqscaffold.pipelineservice.event.StageChangeEvent;
import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import com.iqscaffold.pipelineservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

  private static final Logger log = LoggerFactory.getLogger(PipelineItemServiceImpl.class);

  private final PipelineItemRepository pipelineItemRepository;
  private final PipelineStageRepository pipelineStageRepository;
  private final RabbitTemplate rabbitTemplate;

  public PipelineItemServiceImpl(
      final PipelineItemRepository pipelineItemRepository,
      final PipelineStageRepository pipelineStageRepository,
      final RabbitTemplate rabbitTemplate) {
    this.pipelineItemRepository = pipelineItemRepository;
    this.pipelineStageRepository = pipelineStageRepository;
    this.rabbitTemplate = rabbitTemplate;
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

    // Capture old stage ID for event publishing
    final Long oldStageId = item.getStageId();

    // Update the stage
    item.setStageId(newStageId);
    item.setEnteredStageAt(LocalDateTime.now());
    item.setDaysInStage(0);

    // Handle Won stage - set converted_at timestamp
    if ("Won".equalsIgnoreCase(newStage.getName()) && newStage.getIsFinalStage()) {
      item.setConvertedAt(LocalDateTime.now());
    } else {
      // Clear converted_at if moving away from Won stage
      item.setConvertedAt(null);
    }

    final PipelineItem savedItem = pipelineItemRepository.save(item);

    // Publish stage.changed event
    publishStageChangeEvent(item.getLeadId(), oldStageId, newStageId);

    return savedItem;
  }

  /**
   * Publishes a stage.changed event to RabbitMQ.
   *
   * @param leadId the lead ID
   * @param oldStageId the old stage ID
   * @param newStageId the new stage ID
   */
  private void publishStageChangeEvent(final Long leadId, final Long oldStageId, final Long newStageId) {
    try {
      final String tenantId = TenantContext.getTenantId();
      final StageChangeEvent event = new StageChangeEvent(leadId, oldStageId, newStageId, tenantId);

      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.STAGE_CHANGED_ROUTING_KEY,
          event
      );

      log.info("Published stage.changed event for lead ID: {} from stage {} to stage {}",
          leadId, oldStageId, newStageId);

    } catch (final Exception e) {
      log.error("Error publishing stage.changed event for lead ID: {}", leadId, e);
      // Don't throw exception - event publishing failure shouldn't fail the operation
    }
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
