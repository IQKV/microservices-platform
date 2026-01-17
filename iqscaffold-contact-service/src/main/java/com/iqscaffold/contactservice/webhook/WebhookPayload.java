package com.iqscaffold.contactservice.webhook;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payload sent to webhook endpoints.
 *
 * @param webhookId The ID of the webhook subscription
 * @param event     The event type (contact.created, contact.updated, contact.deleted)
 * @param timestamp When the event occurred
 * @param tenantId  The tenant ID
 * @param data      The event data (contact information)
 */
public record WebhookPayload(
    Long webhookId,
    String event,
    LocalDateTime timestamp,
    String tenantId,
    Map<String, Object> data
) {
}
