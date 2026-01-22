package com.iqscaffold.pipelineservice.event;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.pipelineservice.config.RabbitMQConfig;
import com.iqscaffold.pipelineservice.followup.FollowUp;
import com.iqscaffold.pipelineservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Publisher for follow-up lifecycle events.
 * <p>
 * Publishes events to the CRM events exchange when follow-ups are scheduled
 * or completed. Other services can subscribe to these events for notifications,
 * analytics, or integration with external systems.
 * <p>
 * Event publishing failures are logged but do not throw exceptions to
 * prevent disrupting the main business flow.
 */
@Component
@ConditionalOnBean(RabbitTemplate.class)
public class FollowUpEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(FollowUpEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public FollowUpEventPublisher(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publishes a followup.scheduled event.
   * <p>
   * This event can be consumed by notification services to send reminders,
   * or by analytics services to track follow-up activity.
   *
   * @param followUp The scheduled follow-up
   */
  public void publishFollowUpScheduled(final FollowUp followUp) {
    final String tenantId = TenantContext.getCurrentTenantId();

    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("title", followUp.getTitle());
    metadata.put("description", followUp.getDescription());
    metadata.put("dueDate", followUp.getDueDate().atZone(ZoneId.systemDefault()).toInstant().toString());
    metadata.put("priority", followUp.getPriority().name());
    metadata.put("status", followUp.getStatus().name());
    metadata.put("assignedTo", followUp.getAssignedTo());
    metadata.put("createdBy", followUp.getCreatedBy());

    final FollowUpEvent event = new FollowUpEvent(
        "FOLLOWUP_SCHEDULED",
        followUp.getId(),
        followUp.getLeadId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.FOLLOWUP_SCHEDULED_ROUTING_KEY,
          event
      );
      log.info("Published followup.scheduled event: followUpId={}, leadId={}, tenantId={}, eventId={}",
          followUp.getId(), followUp.getLeadId(), tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish followup.scheduled event: followUpId={}, leadId={}, tenantId={}",
          followUp.getId(), followUp.getLeadId(), tenantId, e);
      // Don't throw exception - event publishing failure shouldn't break the main flow
    }
  }

  /**
   * Publishes a followup.completed event.
   * <p>
   * This event can be consumed by analytics services to track completion rates,
   * or by notification services to update dashboards.
   *
   * @param followUp The completed follow-up
   */
  public void publishFollowUpCompleted(final FollowUp followUp) {
    final String tenantId = TenantContext.getCurrentTenantId();

    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("title", followUp.getTitle());
    metadata.put("description", followUp.getDescription());
    metadata.put("dueDate", followUp.getDueDate().atZone(ZoneId.systemDefault()).toInstant().toString());
    metadata.put("completedAt", followUp.getCompletedAt().atZone(ZoneId.systemDefault()).toInstant().toString());
    metadata.put("priority", followUp.getPriority().name());
    metadata.put("assignedTo", followUp.getAssignedTo());
    metadata.put("wasOverdue", followUp.isOverdue());

    final FollowUpEvent event = new FollowUpEvent(
        "FOLLOWUP_COMPLETED",
        followUp.getId(),
        followUp.getLeadId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.FOLLOWUP_COMPLETED_ROUTING_KEY,
          event
      );
      log.info("Published followup.completed event: followUpId={}, leadId={}, tenantId={}, eventId={}",
          followUp.getId(), followUp.getLeadId(), tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish followup.completed event: followUpId={}, leadId={}, tenantId={}",
          followUp.getId(), followUp.getLeadId(), tenantId, e);
      // Don't throw exception - event publishing failure shouldn't break the main flow
    }
  }
}
