package com.iqscaffold.leadservice.event;

import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.leadservice.config.RabbitMQConfig;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for lead lifecycle events.
 * <p>
 * Publishes events to the CRM events exchange when leads are created,
 * updated, deleted, or converted. Other services can subscribe to these
 * events to react to lead changes.
 * <p>
 * Event publishing failures are logged but do not throw exceptions to
 * prevent disrupting the main business flow.
 */
@Component
public class LeadEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LeadEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public LeadEventPublisher(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publishes a lead.created event.
   * <p>
   * This event is consumed by Pipeline Service to create a pipeline item
   * in the "New" stage.
   *
   * @param lead The created lead
   */
  public void publishLeadCreated(final Lead lead) {
    final String tenantId = TenantContext.getCurrentTenant();

    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("firstName", lead.getFirstName());
    metadata.put("lastName", lead.getLastName());
    metadata.put("email", lead.getEmail());
    metadata.put("phone", lead.getPhone());
    metadata.put("company", lead.getCompany());
    metadata.put("jobTitle", lead.getJobTitle());
    metadata.put("source", lead.getSource());
    metadata.put("status", lead.getStatus().name());
    metadata.put("score", lead.getScore());
    metadata.put("qualified", lead.isQualified());
    metadata.put("createdBy", lead.getCreatedBy());

    final LeadEvent event = new LeadEvent(
        "LEAD_CREATED",
        lead.getId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.LEAD_CREATED_ROUTING_KEY,
          event
      );
      log.info("Published lead.created event: leadId={}, tenantId={}, eventId={}",
          lead.getId(), tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish lead.created event: leadId={}, tenantId={}",
          lead.getId(), tenantId, e);
      // Don't throw exception - event publishing failure shouldn't break the main flow
    }
  }

  /**
   * Publishes a lead.updated event.
   * <p>
   * This event can be consumed by other services that need to react to
   * lead updates (currently no consumers, but available for future use).
   *
   * @param lead The updated lead
   */
  public void publishLeadUpdated(final Lead lead) {
    final String tenantId = TenantContext.getCurrentTenant();

    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("firstName", lead.getFirstName());
    metadata.put("lastName", lead.getLastName());
    metadata.put("email", lead.getEmail());
    metadata.put("phone", lead.getPhone());
    metadata.put("company", lead.getCompany());
    metadata.put("jobTitle", lead.getJobTitle());
    metadata.put("source", lead.getSource());
    metadata.put("status", lead.getStatus().name());
    metadata.put("score", lead.getScore());
    metadata.put("qualified", lead.isQualified());
    metadata.put("updatedBy", lead.getUpdatedBy());

    final LeadEvent event = new LeadEvent(
        "LEAD_UPDATED",
        lead.getId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.LEAD_UPDATED_ROUTING_KEY,
          event
      );
      log.info("Published lead.updated event: leadId={}, tenantId={}, eventId={}",
          lead.getId(), tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish lead.updated event: leadId={}, tenantId={}",
          lead.getId(), tenantId, e);
    }
  }

  /**
   * Publishes a lead.deleted event.
   * <p>
   * This event is consumed by Pipeline Service to remove the associated
   * pipeline item.
   *
   * @param leadId The ID of the deleted lead
   * @param email  The email of the deleted lead (for logging/tracking)
   */
  public void publishLeadDeleted(final Long leadId, final String email) {
    final String tenantId = TenantContext.getCurrentTenant();

    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("email", email);

    final LeadEvent event = new LeadEvent(
        "LEAD_DELETED",
        leadId,
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.LEAD_DELETED_ROUTING_KEY,
          event
      );
      log.info("Published lead.deleted event: leadId={}, tenantId={}, eventId={}",
          leadId, tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish lead.deleted event: leadId={}, tenantId={}",
          leadId, tenantId, e);
    }
  }

  /**
   * Publishes a lead.converted event.
   * <p>
   * This event is published when a lead is successfully converted to a contact.
   * Can be consumed by other services for analytics or notifications.
   *
   * @param lead      The converted lead
   * @param contactId The ID of the created contact
   */
  public void publishLeadConverted(final Lead lead, final Long contactId) {
    final String tenantId = TenantContext.getCurrentTenant();

    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("email", lead.getEmail());
    metadata.put("contactId", contactId);
    metadata.put("convertedAt", lead.getConvertedAt());
    metadata.put("firstName", lead.getFirstName());
    metadata.put("lastName", lead.getLastName());
    metadata.put("company", lead.getCompany());

    final LeadEvent event = new LeadEvent(
        "LEAD_CONVERTED",
        lead.getId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.LEAD_CONVERTED_ROUTING_KEY,
          event
      );
      log.info("Published lead.converted event: leadId={}, contactId={}, tenantId={}, eventId={}",
          lead.getId(), contactId, tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish lead.converted event: leadId={}, contactId={}, tenantId={}",
          lead.getId(), contactId, tenantId, e);
    }
  }
}
