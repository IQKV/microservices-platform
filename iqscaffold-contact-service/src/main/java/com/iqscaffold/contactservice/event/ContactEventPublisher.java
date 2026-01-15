package com.iqscaffold.contactservice.event;

import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.contactservice.config.RabbitMQConfig;
import com.iqscaffold.contactservice.contact.Contact;
import com.iqscaffold.contactservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for contact lifecycle events.
 * <p>
 * Publishes events to the CRM events exchange when contacts are created,
 * updated, or deleted. Other services can subscribe to these events to
 * react to contact changes.
 */
@Component
public class ContactEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(ContactEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public ContactEventPublisher(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publishes a contact created event.
   *
   * @param contact The created contact
   */
  public void publishContactCreated(final Contact contact) {
    String tenantId = TenantContext.getCurrentTenant();

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("firstName", contact.getFirstName());
    metadata.put("lastName", contact.getLastName());
    metadata.put("email", contact.getEmail());
    metadata.put("status", contact.getStatus().name());
    metadata.put("leadScore", contact.getLeadScore());
    metadata.put("convertedFromLeadId", contact.getConvertedFromLeadId());
    metadata.put("createdBy", contact.getCreatedBy());

    ContactEvent event = new ContactEvent(
        "CONTACT_CREATED",
        contact.getId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.CONTACT_CREATED_ROUTING_KEY,
          event
      );
      log.info("Published contact created event: contactId={}, tenantId={}, eventId={}",
          contact.getId(), tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish contact created event: contactId={}, tenantId={}",
          contact.getId(), tenantId, e);
      // Don't throw exception - event publishing failure shouldn't break the main flow
    }
  }

  /**
   * Publishes a contact updated event.
   *
   * @param contact The updated contact
   */
  public void publishContactUpdated(final Contact contact) {
    String tenantId = TenantContext.getCurrentTenant();

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("firstName", contact.getFirstName());
    metadata.put("lastName", contact.getLastName());
    metadata.put("email", contact.getEmail());
    metadata.put("status", contact.getStatus().name());
    metadata.put("leadScore", contact.getLeadScore());
    metadata.put("updatedBy", contact.getUpdatedBy());

    ContactEvent event = new ContactEvent(
        "CONTACT_UPDATED",
        contact.getId(),
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.CONTACT_UPDATED_ROUTING_KEY,
          event
      );
      log.info("Published contact updated event: contactId={}, tenantId={}, eventId={}",
          contact.getId(), tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish contact updated event: contactId={}, tenantId={}",
          contact.getId(), tenantId, e);
    }
  }

  /**
   * Publishes a contact deleted event.
   *
   * @param contactId The ID of the deleted contact
   * @param email     The email of the deleted contact
   */
  public void publishContactDeleted(final Long contactId, final String email) {
    String tenantId = TenantContext.getCurrentTenant();

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("email", email);

    ContactEvent event = new ContactEvent(
        "CONTACT_DELETED",
        contactId,
        tenantId,
        metadata
    );

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_NAME,
          RabbitMQConfig.CONTACT_DELETED_ROUTING_KEY,
          event
      );
      log.info("Published contact deleted event: contactId={}, tenantId={}, eventId={}",
          contactId, tenantId, event.getEventId());
    } catch (final Exception e) {
      log.error("Failed to publish contact deleted event: contactId={}, tenantId={}",
          contactId, tenantId, e);
    }
  }
}
