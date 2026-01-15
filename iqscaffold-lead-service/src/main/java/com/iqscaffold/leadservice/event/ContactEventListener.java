package com.iqscaffold.leadservice.event;

import com.iqscaffold.leadservice.activity.ActivityLogService;
import com.iqscaffold.leadservice.config.RabbitMQConfig;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadService;
import com.iqscaffold.leadservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for contact lifecycle events from Contact Service.
 * <p>
 * Processes contact events to:
 * <ul>
 *   <li>Track lead conversions when contacts are created from leads</li>
 *   <li>Log activities for lead-to-contact conversions</li>
 *   <li>Maintain data consistency between Lead and Contact services</li>
 * </ul>
 */
@Component
public class ContactEventListener {

  private static final Logger log = LoggerFactory.getLogger(ContactEventListener.class);

  private final LeadService leadService;
  private final ActivityLogService activityLogService;

  public ContactEventListener(
      final LeadService leadService,
      final ActivityLogService activityLogService) {
    this.leadService = leadService;
    this.activityLogService = activityLogService;
  }

  /**
   * Handles contact created events.
   * <p>
   * When a contact is created from a lead conversion, this listener:
   * <ul>
   *   <li>Updates the lead's conversion tracking</li>
   *   <li>Logs the conversion activity</li>
   * </ul>
   *
   * @param event The contact created event
   */
  @RabbitListener(queues = RabbitMQConfig.CONTACT_CREATED_QUEUE)
  public void handleContactCreated(final ContactEvent event) {
    log.info("Received contact created event: eventId={}, contactId={}, tenantId={}",
        event.getEventId(), event.getContactId(), event.getTenantId());

    // Set tenant context for database operations
    TenantContext.setCurrentTenant(event.getTenantId());

    try {
      // Check if contact was converted from a lead
      Object convertedFromLeadIdObj = event.getMetadata().get("convertedFromLeadId");
      if (convertedFromLeadIdObj != null) {
        Long leadId = convertedFromLeadIdObj instanceof Integer
            ? ((Integer) convertedFromLeadIdObj).longValue()
            : (Long) convertedFromLeadIdObj;

        log.info("Contact {} was converted from lead {}, updating lead conversion tracking",
            event.getContactId(), leadId);

        // Update lead conversion tracking
        try {
          Lead lead = leadService.convertLead(leadId, event.getContactId());

          // Log conversion activity
          String activityDescription = String.format(
              "Lead converted to contact. Contact ID: %d, Contact Email: %s",
              event.getContactId(),
              event.getMetadata().get("email")
          );
          activityLogService.logLeadConverted(leadId, activityDescription);

          log.info("Successfully updated lead {} conversion tracking for contact {}",
              leadId, event.getContactId());
        } catch (final Exception e) {
          log.error("Failed to update lead {} conversion tracking for contact {}: {}",
              leadId, event.getContactId(), e.getMessage(), e);
          // Don't throw - we don't want to requeue the message if lead update fails
        }
      } else {
        log.debug("Contact {} was not converted from a lead, no action needed",
            event.getContactId());
      }
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Handles contact updated events.
   * <p>
   * Currently logs the event for monitoring purposes.
   * Future enhancements could sync contact updates back to leads.
   *
   * @param event The contact updated event
   */
  @RabbitListener(queues = RabbitMQConfig.CONTACT_UPDATED_QUEUE)
  public void handleContactUpdated(final ContactEvent event) {
    log.info("Received contact updated event: eventId={}, contactId={}, tenantId={}",
        event.getEventId(), event.getContactId(), event.getTenantId());

    // Set tenant context
    TenantContext.setCurrentTenant(event.getTenantId());

    try {
      // Future: Sync contact updates back to leads if needed
      log.debug("Contact {} updated, no action needed in Lead Service",
          event.getContactId());
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Handles contact deleted events.
   * <p>
   * Currently logs the event for monitoring purposes.
   * Future enhancements could handle cleanup or notifications.
   *
   * @param event The contact deleted event
   */
  @RabbitListener(queues = RabbitMQConfig.CONTACT_DELETED_QUEUE)
  public void handleContactDeleted(final ContactEvent event) {
    log.info("Received contact deleted event: eventId={}, contactId={}, tenantId={}",
        event.getEventId(), event.getContactId(), event.getTenantId());

    // Set tenant context
    TenantContext.setCurrentTenant(event.getTenantId());

    try {
      // Future: Handle contact deletion (e.g., update lead status, send notifications)
      log.debug("Contact {} deleted, no action needed in Lead Service",
          event.getContactId());
    } finally {
      TenantContext.clear();
    }
  }
}
