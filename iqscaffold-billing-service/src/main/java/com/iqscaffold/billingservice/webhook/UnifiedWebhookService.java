package com.iqscaffold.billingservice.webhook;

import java.util.List;

import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Unified webhook service that processes normalized webhook events from all payment providers.
 * <p>
 * This service acts as the central orchestrator for webhook event processing, delegating
 * to specialized event handlers based on the event type. It provides:
 * <ul>
 *   <li>Provider-agnostic webhook processing using {@link WebhookEvent}</li>
 *   <li>Tenant context resolution and management</li>
 *   <li>Automatic routing to appropriate event handlers</li>
 *   <li>Idempotent event processing guarantees</li>
 * </ul>
 * <p>
 * Processing Flow:
 * <ol>
 *   <li>Receive normalized {@link WebhookEvent} from provider adapter</li>
 *   <li>Resolve and set tenant context</li>
 *   <li>Find an appropriate event handler(s) that support the event</li>
 *   <li>Delegate event processing to handler(s)</li>
 *   <li>Clean up tenant context</li>
 * </ol>
 * <p>
 * This replaces the provider-specific {@code StripeWebhookService} with a unified
 * approach that works across all payment gateways.
 */
@Service
public class UnifiedWebhookService {
  private static final Logger logger = LoggerFactory.getLogger(UnifiedWebhookService.class);

  private final List<WebhookEventHandler> eventHandlers;
  private final MerchantStripeConfigRepository merchantConfigRepository;

  public UnifiedWebhookService(
      final List<WebhookEventHandler> eventHandlers,
      final MerchantStripeConfigRepository merchantConfigRepository) {
    this.eventHandlers = eventHandlers;
    this.merchantConfigRepository = merchantConfigRepository;
  }

  /**
   * Process a normalized webhook event from any payment provider.
   * <p>
   * This method:
   * <ol>
   *   <li>Resolves tenant context from the event</li>
   *   <li>Routes the event to appropriate handler(s)</li>
   *   <li>Ensures tenant context is cleaned up</li>
   * </ol>
   * <p>
   * The method is designed to be idempotent - processing the same event multiple
   * times should result in the same final state.
   *
   * @param event The normalized webhook event to process
   * @throws IllegalArgumentException If tenant resolution fails or no handler supports the event
   */
  public void processWebhookEvent(WebhookEvent event) {
    logger.info("Processing webhook event: id={}, type={}, provider={}, resourceId={}",
        event.eventId(), event.eventType(), event.provider(), event.resourceId());

    // Resolve and set tenant context
    resolveAndSetTenant(event);

    try {
      // Find handlers that support this event
      List<WebhookEventHandler> supportingHandlers = eventHandlers.stream()
          .filter(handler -> handler.supports(event))
          .toList();

      if (supportingHandlers.isEmpty()) {
        logger.debug("No handler found for event type: {}", event.eventType());
        return;
      }

      // Delegate to each supporting handler
      for (final WebhookEventHandler handler : supportingHandlers) {
        try {
          handler.handleEvent(event);
          logger.debug("Handler {} successfully processed event: {}",
              handler.getClass().getSimpleName(), event.eventId());
        } catch (final Exception e) {
          logger.error("Handler {} failed to process event: {}",
              handler.getClass().getSimpleName(), event.eventId(), e);
          // Continue processing with other handlers
        }
      }
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Resolves tenant context from the webhook event and sets it in TenantContext.
   * <p>
   * Tenant resolution strategies:
   * <ol>
   *   <li>Direct: Tenant ID present in event metadata (payment/payout events)</li>
   *   <li>Lookup: Resolve tenant from account ID (account events)</li>
   *   <li>Global: Some events don't require tenant context</li>
   * </ol>
   *
   * @param event The webhook event to resolve tenant from
   * @throws IllegalArgumentException If tenant resolution fails for non-global events
   */
  private void resolveAndSetTenant(WebhookEvent event) {
    // Try to get tenant from event first
    if (event.tenantId().isPresent()) {
      String tenantId = event.tenantId().get();
      TenantContext.setCurrentTenantId(tenantId);
      logger.debug("Resolved tenant {} from event metadata", tenantId);
      return;
    }

    // For account events, try to resolve from account ID
    if (event.isAccountEvent() && event.provider() == com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE) {
      String accountId = event.resourceId();
      var configOpt = merchantConfigRepository.findByStripeAccountId(accountId);
      if (configOpt.isPresent()) {
        String tenantId = configOpt.get().getTenantId();
        TenantContext.setCurrentTenantId(tenantId);
        logger.debug("Resolved tenant {} from Stripe account {}", tenantId, accountId);
        return;
      }
    }

    // Check if this is a global event that doesn't require tenant context
    if (isGlobalEvent(event)) {
      logger.debug("Processing global event: {}", event.eventType());
      return;
    }

    // Tenant resolution failed for non-global event
    logger.error("Rejecting webhook: Could not resolve tenant for event type={}, provider={}, resourceId={}",
        event.eventType(), event.provider(), event.resourceId());
    throw new IllegalArgumentException("Tenant resolution failed for event: " + event.eventType());
  }

  /**
   * Checks if an event is global and doesn't require tenant context.
   * Currently, account.updated events are considered global as they can be
   * resolved after initial processing.
   */
  private boolean isGlobalEvent(WebhookEvent event) {
    return WebhookEvent.EventType.ACCOUNT_UPDATED.equals(event.eventType());
  }
}
