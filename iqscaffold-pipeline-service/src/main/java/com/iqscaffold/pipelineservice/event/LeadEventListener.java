package com.iqscaffold.pipelineservice.event;

import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.pipelineservice.config.RabbitMQConfig;
import com.iqscaffold.pipelineservice.pipeline.PipelineItem;
import com.iqscaffold.pipelineservice.pipeline.PipelineItemRepository;
import com.iqscaffold.pipelineservice.pipeline.PipelineStage;
import com.iqscaffold.pipelineservice.pipeline.PipelineStageRepository;
import com.iqscaffold.pipelineservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener for lead lifecycle events.
 * Handles lead.created and lead.deleted events to manage pipeline items.
 */
@Component
public class LeadEventListener {

  private static final Logger log = LoggerFactory.getLogger(LeadEventListener.class);
  private static final String NEW_STAGE_NAME = "New";

  private final PipelineItemRepository pipelineItemRepository;
  private final PipelineStageRepository pipelineStageRepository;

  public LeadEventListener(
      final PipelineItemRepository pipelineItemRepository,
      final PipelineStageRepository pipelineStageRepository) {
    this.pipelineItemRepository = pipelineItemRepository;
    this.pipelineStageRepository = pipelineStageRepository;
  }

  /**
   * Handles lead.created events by creating a pipeline item in the "New" stage.
   *
   * @param event the lead created event
   */
  @RabbitListener(queues = RabbitMQConfig.LEAD_CREATED_QUEUE)
  @Transactional
  public void handleLeadCreated(final LeadEvent event) {
    log.info("Received lead.created event: {}", event);

    try {
      // Set tenant context from event
      if (event.getTenantId() != null) {
        TenantContext.setTenantId(event.getTenantId());
      }

      // Check if pipeline item already exists for this lead
      if (pipelineItemRepository.existsByLeadId(event.getLeadId())) {
        log.warn("Pipeline item already exists for lead ID: {}", event.getLeadId());
        return;
      }

      // Find the "New" stage
      final List<PipelineStage> stages = pipelineStageRepository.findAllByOrderByDisplayOrderAsc();
      final PipelineStage newStage = stages.stream()
          .filter(stage -> NEW_STAGE_NAME.equalsIgnoreCase(stage.getName()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Default 'New' stage not found"));

      // Create pipeline item
      final PipelineItem pipelineItem = new PipelineItem();
      pipelineItem.setLeadId(event.getLeadId());
      pipelineItem.setStageId(newStage.getId());
      pipelineItem.setEnteredStageAt(LocalDateTime.now());
      pipelineItem.setDaysInStage(0);
      pipelineItem.setCreatedBy("system");
      pipelineItem.setUpdatedBy("system");

      pipelineItemRepository.save(pipelineItem);

      log.info("Created pipeline item for lead ID: {} in stage: {}", event.getLeadId(), newStage.getName());

    } catch (final Exception e) {
      log.error("Error handling lead.created event for lead ID: {}", event.getLeadId(), e);
      throw e;
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Handles lead.deleted events by removing the pipeline item.
   *
   * @param event the lead deleted event
   */
  @RabbitListener(queues = RabbitMQConfig.LEAD_DELETED_QUEUE)
  @Transactional
  public void handleLeadDeleted(final LeadEvent event) {
    log.info("Received lead.deleted event: {}", event);

    try {
      // Set tenant context from event
      if (event.getTenantId() != null) {
        TenantContext.setTenantId(event.getTenantId());
      }

      // Find and delete the pipeline item
      pipelineItemRepository.findByLeadId(event.getLeadId())
          .ifPresentOrElse(
              pipelineItem -> {
                pipelineItemRepository.delete(pipelineItem);
                log.info("Deleted pipeline item for lead ID: {}", event.getLeadId());
              },
              () -> log.warn("No pipeline item found for lead ID: {}", event.getLeadId())
          );

    } catch (final Exception e) {
      log.error("Error handling lead.deleted event for lead ID: {}", event.getLeadId(), e);
      throw e;
    } finally {
      TenantContext.clear();
    }
  }
}
