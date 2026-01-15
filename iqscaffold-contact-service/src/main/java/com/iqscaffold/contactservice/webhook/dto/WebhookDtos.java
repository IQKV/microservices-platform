package com.iqscaffold.contactservice.webhook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTOs for Webhook REST API operations.
 */
public final class WebhookDtos {

  private WebhookDtos() {
    // Utility class
  }

  /**
   * Request DTO for creating a webhook.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateWebhookRequest(
      @NotBlank(message = "Name is required")
      @Size(max = 100, message = "Name must not exceed 100 characters")
      String name,

      @NotBlank(message = "URL is required")
      @Size(max = 500, message = "URL must not exceed 500 characters")
      String url,

      @Size(max = 255, message = "Secret must not exceed 255 characters")
      String secret,

      @NotEmpty(message = "At least one event is required")
      Set<String> events,

      Boolean active,

      @Min(value = 0, message = "Retry count must be at least 0")
      @Max(value = 10, message = "Retry count must not exceed 10")
      Integer retryCount,

      @Min(value = 1, message = "Timeout must be at least 1 second")
      @Max(value = 300, message = "Timeout must not exceed 300 seconds")
      Integer timeoutSeconds,

      @Size(max = 500, message = "Description must not exceed 500 characters")
      String description
  ) {
  }

  /**
   * Request DTO for updating a webhook.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateWebhookRequest(
      @NotBlank(message = "Name is required")
      @Size(max = 100, message = "Name must not exceed 100 characters")
      String name,

      @NotBlank(message = "URL is required")
      @Size(max = 500, message = "URL must not exceed 500 characters")
      String url,

      @Size(max = 255, message = "Secret must not exceed 255 characters")
      String secret,

      @NotEmpty(message = "At least one event is required")
      Set<String> events,

      Boolean active,

      @Min(value = 0, message = "Retry count must be at least 0")
      @Max(value = 10, message = "Retry count must not exceed 10")
      Integer retryCount,

      @Min(value = 1, message = "Timeout must be at least 1 second")
      @Max(value = 300, message = "Timeout must not exceed 300 seconds")
      Integer timeoutSeconds,

      @Size(max = 500, message = "Description must not exceed 500 characters")
      String description
  ) {
  }

  /**
   * Response DTO for webhook information.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record WebhookResponse(
      Long id,
      String name,
      String url,
      Set<String> events,
      Boolean active,
      Integer retryCount,
      Integer timeoutSeconds,
      String description,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy,
      String updatedBy,
      LocalDateTime lastTriggeredAt,
      Long successCount,
      Long failureCount
  ) {
  }
}
