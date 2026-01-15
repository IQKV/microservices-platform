package com.iqscaffold.pipelineservice.event;

import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.pipelineservice.config.RabbitMQConfig;
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
 * Event listener for contact lifecycle events.
 * <p>
 * Handles contact.created events to update pipeline items when leads are
 * converted to contacts. When a contact is created from a lead conversion,
 * the pipeline item is moved to the "Won" stage.
 */
@Component
public class ContactEventListener {

  private static final Logger log = LoggerFactory.getLogger(ContactEventListener.class);
  private static final String WON_STAGE_NAME = "Won";

  private final PipelineItemRepository pipelineItemRepository;
  private final PipelineStageRepository pipelineStageRepository;

  public ContactEventListener(
      final PipelineItemRepository pipelineItemRepository,
      final PipelineStageRepository pipelineStageRepository) {
    this.pipelineItemRepository = pipelineItemRepository;
    this.pipelineStageRepository = pipelineStageRepository;
  }

  /**
   * Handles contact.created events.
   * <p>
   * When a contact is created from a lead conversion, this listener:
   * <ul>
   *   <li>Finds the pipeline item for the converted lead</li>
   *   <li>Moves the pipeline item to the "Won" stage</li>
   *   <li>Sets the conversion timestamp</li>
   * </ul>
   *
   * @param event the contact created event
   */
  @RabbitListener(queues = RabbitMQConfig.CONTACT_CREATED_QUEUE)
  @Transactional
  public void handleContactCreated(final ContactEvent event) {
    log.info("Received contact.created event: eventId={}, contactId={}, tenantId={}",
        event.getEventId(), event.getContactId(), event.getTenantId());

    try {
      // Set tenant context from event
      if (event.getTenantId() != null) {
        TenantContext.setTenantId(event.getTenantId());
      }

      // Check if contact was converted from a lead
      final Object convertedFromLeadIdObj = event.getMetadata().get("convertedFromLeadId");
      if (convertedFromLeadIdObj == null) {
        log.debug("Contact {} was not converted from a lead, no pipeline action needed",
            event.getContactId());
        return;
      }

      // Convert to Long (handle both Integer and Long from JSON)
      final Long leadId = convertedFromLeadIdObj instanceof Integer
          ? ((Integer) convertedFromLeadIdObj).longValue()
          : (Long) convertedFromLeadIdObj;

      log.info("Contact {} was converted from lead {}, moving pipeline item to Won stage",
          event.getContactId(), leadId);

      // Find the pipeline item for this lead
      pipelineItemRepository.findByLeadId(leadId)
          .ifPresentOrElse(
              pipelineItem -> {
                try {
                  // Find the "Won" stage
                  final List<PipelineStage> stages = pipelineStageRepository.findAllByOrderByDisplayOrderAsc();
                  final PipelineStage wonStage = stages.stream()
                      .filter(stage -> WON_STAGE_NAME.equalsIgnoreCase(stage.getName()))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException("'Won' stage not found"));

                  // Update pipeline item to Won stage
                  final Long oldStageId = pipelineItem.getStageId();
                  pipelineItem.setStageId(wonStage.getId());
                  pipelineItem.setEnteredStageAt(LocalDateTime.now());
                  pipelineItem.setDaysInStage(0);
                  pipelineItem.setConvertedAt(LocalDateTime.now());
                  pipelineItem.setUpdatedBy("system");

                  pipelineItemRepository.save(pipelineItem);

                  log.info("Moved pipeline item for lead {} from stage {} to Won stage {} (contact {})",
                      leadId, oldStageId, wonStage.getId(), event.getContactId());

                } catch (final Exception e) {
                  log.error("Failed to move pipeline item for lead {} to Won stage: {}",
                      leadId, e.getMessage(), e);
                  // Don't throw - we don't want to requeue the message
                }
              },
              () -> log.warn("No pipeline item found for converted lead ID: {}", leadId)
          );

    } catch (final Exception e) {
      log.error("Error handling contact.created event for contact ID: {}", event.getContactId(), e);
      // Don't throw - we don't want to requeue the message
    } finally {
      TenantContext.clear();
    }
  }
}
