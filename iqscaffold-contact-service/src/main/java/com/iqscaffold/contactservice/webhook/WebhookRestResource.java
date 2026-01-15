package com.iqscaffold.contactservice.webhook;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import com.iqscaffold.contactservice.webhook.dto.WebhookDtos;
import com.iqscaffold.contactservice.webhook.dto.WebhookMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for webhook management.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Webhook management for external integrations")
@SecurityRequirement(name = "bearerAuth")
public class WebhookRestResource {

  private final WebhookService webhookService;

  public WebhookRestResource(final WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  /**
   * Creates a new webhook subscription.
   *
   * @param request The webhook creation request
   * @return The created webhook
   */
  @Operation(
      summary = "Create webhook",
      description = "Creates a new webhook subscription for external integrations")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Webhook created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<WebhookDtos.WebhookResponse> createWebhook(
      @Valid @RequestBody WebhookDtos.CreateWebhookRequest request) {
    String userId = getCurrentUserId();

    Webhook webhook = WebhookMapper.toEntity(request, userId);
    Webhook savedWebhook = webhookService.createWebhook(webhook);
    WebhookDtos.WebhookResponse response = WebhookMapper.toResponse(savedWebhook);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves a webhook by ID.
   *
   * @param id The webhook ID
   * @return The webhook details
   */
  @Operation(
      summary = "Get webhook by ID",
      description = "Retrieves a specific webhook by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Webhook found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Webhook not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<WebhookDtos.WebhookResponse> getWebhookById(@PathVariable Long id) {
    Webhook webhook = webhookService.getWebhookById(id);
    WebhookDtos.WebhookResponse response = WebhookMapper.toResponse(webhook);
    return ResponseEntity.ok(response);
  }

  /**
   * Lists all webhooks.
   *
   * @return List of all webhooks
   */
  @Operation(
      summary = "List webhooks",
      description = "Retrieves a list of all webhook subscriptions")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<WebhookDtos.WebhookResponse>> listWebhooks() {
    List<Webhook> webhooks = webhookService.getAllWebhooks();
    List<WebhookDtos.WebhookResponse> response = webhooks.stream()
        .map(WebhookMapper::toResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  /**
   * Updates an existing webhook.
   *
   * @param id      The webhook ID
   * @param request The webhook update request
   * @return The updated webhook
   */
  @Operation(
      summary = "Update webhook",
      description = "Updates an existing webhook subscription")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Webhook updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Webhook not found")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<WebhookDtos.WebhookResponse> updateWebhook(
      @PathVariable Long id,
      @Valid @RequestBody WebhookDtos.UpdateWebhookRequest request) {
    String userId = getCurrentUserId();

    Webhook webhook = webhookService.getWebhookById(id);
    WebhookMapper.updateEntity(webhook, request, userId);
    Webhook updatedWebhook = webhookService.updateWebhook(id, webhook);
    WebhookDtos.WebhookResponse response = WebhookMapper.toResponse(updatedWebhook);

    return ResponseEntity.ok(response);
  }

  /**
   * Deletes a webhook.
   *
   * @param id The webhook ID
   * @return No content
   */
  @Operation(
      summary = "Delete webhook",
      description = "Deletes a webhook subscription by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Webhook deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Webhook not found")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
    webhookService.deleteWebhook(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Extracts the current user ID from the JWT token.
   *
   * @return The user ID, or "system" if not available
   */
  private String getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String userId = jwt.getClaimAsString("userId");
      if (userId != null) {
        return userId;
      }
      // Fallback to subject if userId claim not present
      return jwt.getSubject();
    }
    return "system";
  }
}
