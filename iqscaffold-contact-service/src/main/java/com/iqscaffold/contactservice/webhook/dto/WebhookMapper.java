package com.iqscaffold.contactservice.webhook.dto;

import com.iqscaffold.contactservice.webhook.Webhook;

/**
 * Mapper for converting between Webhook entities and DTOs.
 */
public final class WebhookMapper {

  private WebhookMapper() {
    // Utility class
  }

  /**
   * Converts a Webhook entity to a response DTO.
   *
   * @param webhook The webhook entity
   * @return The response DTO
   */
  public static WebhookDtos.WebhookResponse toResponse(final Webhook webhook) {
    return new WebhookDtos.WebhookResponse(
        webhook.getId(),
        webhook.getName(),
        webhook.getUrl(),
        webhook.getEventSet(),
        webhook.getActive(),
        webhook.getRetryCount(),
        webhook.getTimeoutSeconds(),
        webhook.getDescription(),
        webhook.getCreatedAt(),
        webhook.getUpdatedAt(),
        webhook.getCreatedBy(),
        webhook.getUpdatedBy(),
        webhook.getLastTriggeredAt(),
        webhook.getSuccessCount(),
        webhook.getFailureCount()
    );
  }

  /**
   * Converts a create request DTO to a Webhook entity.
   *
   * @param request   The create request
   * @param createdBy The user creating the webhook
   * @return The webhook entity
   */
  public static Webhook toEntity(
      final WebhookDtos.CreateWebhookRequest request,
      final String createdBy) {
    Webhook webhook = new Webhook();
    webhook.setName(request.name());
    webhook.setUrl(request.url());
    webhook.setSecret(request.secret());
    webhook.setEventSet(request.events());
    webhook.setActive(request.active() != null ? request.active() : true);
    webhook.setRetryCount(request.retryCount() != null ? request.retryCount() : 3);
    webhook.setTimeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : 30);
    webhook.setDescription(request.description());
    webhook.setCreatedBy(createdBy);
    webhook.setUpdatedBy(createdBy);
    return webhook;
  }

  /**
   * Updates a webhook entity from an update request DTO.
   *
   * @param webhook   The webhook entity to update
   * @param request   The update request
   * @param updatedBy The user updating the webhook
   */
  public static void updateEntity(
      final Webhook webhook,
      final WebhookDtos.UpdateWebhookRequest request,
      final String updatedBy) {
    webhook.setName(request.name());
    webhook.setUrl(request.url());
    webhook.setSecret(request.secret());
    webhook.setEventSet(request.events());
    webhook.setActive(request.active() != null ? request.active() : true);
    webhook.setRetryCount(request.retryCount() != null ? request.retryCount() : 3);
    webhook.setTimeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : 30);
    webhook.setDescription(request.description());
    webhook.setUpdatedBy(updatedBy);
  }
}
