package com.iqscaffold.billingservice.admin;

import java.util.UUID;

import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionDto;
import com.iqscaffold.billingservice.subscription.SubscriptionLifecycleManager;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for admin subscription operations.
 *
 * <p>This service provides administrative capabilities for managing subscriptions
 * across all tenants, including:
 * <ul>
 *   <li>Listing all subscriptions with filtering</li>
 *   <li>Force canceling subscriptions</li>
 *   <li>Viewing subscription details across tenants</li>
 * </ul>
 *
 * <p>All operations in this service bypass normal tenant isolation and require
 * ADMIN or SUPER_ADMIN authority.
 */
@Service
public class AdminSubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(AdminSubscriptionService.class);

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionLifecycleManager subscriptionLifecycleManager;
  private final MessageService messageService;

  public AdminSubscriptionService(
      SubscriptionRepository subscriptionRepository,
      SubscriptionLifecycleManager subscriptionLifecycleManager,
      MessageService messageService) {
    this.subscriptionRepository = subscriptionRepository;
    this.subscriptionLifecycleManager = subscriptionLifecycleManager;
    this.messageService = messageService;
  }

  /**
   * Lists all subscriptions with optional filtering and pagination.
   *
   * <p>This method allows administrators to view subscriptions across all tenants
   * with optional filtering by status, plan code, and tenant ID.
   *
   * @param status   optional status filter
   * @param planCode optional plan code filter
   * @param tenantId optional tenant ID filter
   * @param pageable pagination parameters
   * @return page of subscription DTOs
   */
  @Transactional(readOnly = true)
  public Page<SubscriptionDto> listSubscriptions(
      SubscriptionStatus status,
      String planCode,
      String tenantId,
      Pageable pageable) {

    log.debug(
        "Admin listing subscriptions - status: {}, planCode: {}, tenantId: {}",
        status,
        planCode,
        tenantId
    );

    // Build dynamic specification based on filters
    Specification<Subscription> spec = Specification.where(null);

    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    if (planCode != null && !planCode.isBlank()) {
      spec = spec.and((root, query, cb) ->
          cb.equal(root.get("plan").get("planCode"), planCode));
    }

    if (tenantId != null && !tenantId.isBlank()) {
      spec = spec.and((root, query, cb) ->
          cb.equal(root.get("tenantId"), UUID.fromString(tenantId)));
    }

    // Execute query with pagination
    var subscriptions = subscriptionRepository.findAll(spec, pageable);

    log.debug(
        "Found {} subscriptions (page {} of {})",
        subscriptions.getNumberOfElements(),
        subscriptions.getNumber() + 1,
        subscriptions.getTotalPages()
    );

    // Map to DTOs
    return subscriptions.map(this::toDto);
  }

  /**
   * Force cancels a subscription immediately.
   *
   * <p>This method bypasses normal cancellation rules and immediately cancels
   * a subscription. It should be used with caution and only for administrative
   * purposes such as policy violations or fraud cases.
   *
   * @param subscriptionId subscription identifier
   * @param reason         reason for the cancellation (required for audit trail)
   * @return DTO representation of the canceled subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if subscription not found
   */
  @Transactional
  public SubscriptionDto forceCancelSubscription(Long subscriptionId, String reason) {
    log.info("Admin force canceling subscription: {}, reason: {}", subscriptionId, reason);

    // Retrieve the subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("Subscription not found: {}", subscriptionId);
          return new SubscriptionException.SubscriptionNotFoundException(subscriptionId.toString());
        });

    // Add admin cancellation metadata
    subscription.addMetadata("admin_canceled", "true");
    subscription.addMetadata("admin_cancel_reason", reason);
    subscription.addMetadata("admin_cancel_timestamp", java.time.LocalDateTime.now().toString());

    // Delegate to lifecycle manager for state transition
    // Force immediate cancellation
    subscriptionLifecycleManager.cancelImmediately(subscription, reason);

    // Persist changes
    var canceledSubscription = subscriptionRepository.save(subscription);

    // TODO: Publish SubscriptionCanceled domain event
    // Event should include:
    // - subscriptionId
    // - tenantId
    // - canceledBy: "ADMIN"
    // - reason
    // - immediate: true
    // Event will be published using BillingConstants.BillingEvents constants

    log.info(
        "Successfully force canceled subscription: {} for tenant: {}",
        canceledSubscription.getId(),
        canceledSubscription.getTenantId()
    );

    var successMessage = messageService.getMessage(
        "subscription.canceled.admin",
        canceledSubscription.getId()
    );
    log.info(successMessage);

    return toDto(canceledSubscription);
  }

  /**
   * Translates a Subscription aggregate to a DTO.
   *
   * <p>This method handles the translation between the domain layer and
   * the presentation layer, ensuring proper separation of concerns.
   *
   * @param subscription domain aggregate
   * @return DTO representation
   */
  private SubscriptionDto toDto(Subscription subscription) {
    return new SubscriptionDto(
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getUserId(),
        subscription.getPlan().getPlanCode(),
        subscription.getPlan().getName(),
        subscription.getPlan().getTier(),
        subscription.getPlan().getBillingCycle(),
        subscription.getPlan().getBasePrice(),
        subscription.getPlan().getCurrency(),
        subscription.getStatus(),
        subscription.getCurrentPeriodStart(),
        subscription.getCurrentPeriodEnd(),
        subscription.getTrialStart(),
        subscription.getTrialEnd(),
        subscription.getCanceledAt(),
        subscription.getCancelAtPeriodEnd(),
        subscription.getMetadata(),
        subscription.getCreatedAt(),
        subscription.getUpdatedAt()
    );
  }
}
