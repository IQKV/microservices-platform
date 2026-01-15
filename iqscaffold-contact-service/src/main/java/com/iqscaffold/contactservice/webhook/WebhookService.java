package com.iqscaffold.contactservice.webhook;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.iqscaffold.contactservice.contact.Contact;
import com.iqscaffold.contactservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

/**
 * Service for managing and triggering webhooks.
 */
@Service
@Transactional
public class WebhookService {

  private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
  private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
  private static final String EVENT_HEADER = "X-Webhook-Event";

  private final WebhookRepository webhookRepository;
  private final WebClient.Builder webClientBuilder;

  public WebhookService(
      final WebhookRepository webhookRepository,
      final WebClient.Builder webClientBuilder) {
    this.webhookRepository = webhookRepository;
    this.webClientBuilder = webClientBuilder;
  }

  /**
   * Triggers webhooks for a contact event.
   *
   * @param event   The event type
   * @param contact The contact data
   */
  @Async
  public void triggerWebhooks(final String event, final Contact contact) {
    String tenantId = TenantContext.getCurrentTenant();
    log.debug("Triggering webhooks for event: {} in tenant: {}", event, tenantId);

    List<Webhook> webhooks = webhookRepository.findActiveWebhooksByEvent(event);

    if (webhooks.isEmpty()) {
      log.debug("No active webhooks found for event: {}", event);
      return;
    }

    Map<String, Object> contactData = buildContactData(contact);

    for (final Webhook webhook : webhooks) {
      try {
        sendWebhook(webhook, event, tenantId, contactData);
      } catch (final Exception e) {
        log.error("Failed to send webhook {} for event {}", webhook.getId(), event, e);
      }
    }
  }

  /**
   * Sends a webhook notification.
   *
   * @param webhook     The webhook configuration
   * @param event       The event type
   * @param tenantId    The tenant ID
   * @param contactData The contact data
   */
  private void sendWebhook(
      final Webhook webhook,
      final String event,
      final String tenantId,
      final Map<String, Object> contactData) {

    WebhookPayload payload = new WebhookPayload(
        webhook.getId(),
        event,
        LocalDateTime.now(),
        tenantId,
        contactData
    );

    String signature = generateSignature(payload, webhook.getSecret());

    WebClient webClient = webClientBuilder
        .baseUrl(webhook.getUrl())
        .build();

    try {
      webClient.post()
          .contentType(MediaType.APPLICATION_JSON)
          .header(SIGNATURE_HEADER, signature)
          .header(EVENT_HEADER, event)
          .bodyValue(payload)
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(webhook.getTimeoutSeconds()))
          .retryWhen(Retry.backoff(webhook.getRetryCount(), Duration.ofSeconds(1))
              .maxBackoff(Duration.ofSeconds(10)))
          .doOnSuccess(response -> {
            webhook.incrementSuccessCount();
            webhookRepository.save(webhook);
            log.info("Successfully sent webhook {} for event {}", webhook.getId(), event);
          })
          .doOnError(error -> {
            webhook.incrementFailureCount();
            webhookRepository.save(webhook);
            log.error("Failed to send webhook {} for event {}", webhook.getId(), event, error);
          })
          .block();
    } catch (final Exception e) {
      webhook.incrementFailureCount();
      webhookRepository.save(webhook);
      log.error("Exception sending webhook {} for event {}", webhook.getId(), event, e);
    }
  }

  /**
   * Generates HMAC-SHA256 signature for webhook payload.
   *
   * @param payload The webhook payload
   * @param secret  The webhook secret
   * @return The signature
   */
  private String generateSignature(final WebhookPayload payload, final String secret) {
    if (secret == null || secret.isEmpty()) {
      return "";
    }

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(
          secret.getBytes(StandardCharsets.UTF_8),
          "HmacSHA256"
      );
      mac.init(secretKeySpec);

      String payloadString = payload.toString();
      byte[] hash = mac.doFinal(payloadString.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (final Exception e) {
      log.error("Failed to generate webhook signature", e);
      return "";
    }
  }

  /**
   * Builds contact data map for webhook payload.
   *
   * @param contact The contact entity
   * @return Map of contact data
   */
  private Map<String, Object> buildContactData(final Contact contact) {
    Map<String, Object> data = new HashMap<>();
    data.put("id", contact.getId());
    data.put("firstName", contact.getFirstName());
    data.put("lastName", contact.getLastName());
    data.put("email", contact.getEmail());
    data.put("phone", contact.getPhone());
    data.put("jobTitle", contact.getJobTitle());
    data.put("companyId", contact.getCompanyId());
    data.put("status", contact.getStatus().toString());
    data.put("leadScore", contact.getLeadScore());
    data.put("convertedFromLeadId", contact.getConvertedFromLeadId());
    data.put("convertedAt", contact.getConvertedAt());
    data.put("createdAt", contact.getCreatedAt());
    data.put("updatedAt", contact.getUpdatedAt());
    return data;
  }

  /**
   * Creates a new webhook subscription.
   *
   * @param webhook The webhook to create
   * @return The created webhook
   */
  public Webhook createWebhook(final Webhook webhook) {
    return webhookRepository.save(webhook);
  }

  /**
   * Updates an existing webhook subscription.
   *
   * @param id      The webhook ID
   * @param webhook The updated webhook data
   * @return The updated webhook
   */
  public Webhook updateWebhook(final Long id, final Webhook webhook) {
    Webhook existing = webhookRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Webhook not found with id: " + id));

    existing.setName(webhook.getName());
    existing.setUrl(webhook.getUrl());
    existing.setSecret(webhook.getSecret());
    existing.setEvents(webhook.getEvents());
    existing.setActive(webhook.getActive());
    existing.setRetryCount(webhook.getRetryCount());
    existing.setTimeoutSeconds(webhook.getTimeoutSeconds());
    existing.setDescription(webhook.getDescription());
    existing.setUpdatedBy(webhook.getUpdatedBy());

    return webhookRepository.save(existing);
  }

  /**
   * Deletes a webhook subscription.
   *
   * @param id The webhook ID
   */
  public void deleteWebhook(final Long id) {
    webhookRepository.deleteById(id);
  }

  /**
   * Gets all webhooks.
   *
   * @return List of all webhooks
   */
  @Transactional(readOnly = true)
  public List<Webhook> getAllWebhooks() {
    return webhookRepository.findAll();
  }

  /**
   * Gets a webhook by ID.
   *
   * @param id The webhook ID
   * @return The webhook
   */
  @Transactional(readOnly = true)
  public Webhook getWebhookById(final Long id) {
    return webhookRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Webhook not found with id: " + id));
  }
}
