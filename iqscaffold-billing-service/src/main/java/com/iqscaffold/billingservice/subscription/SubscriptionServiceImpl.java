package com.iqscaffold.billingservice.subscription;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.iqscaffold.billingservice.payment.GatewayConfigurationService;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import com.iqscaffold.billingservice.subscription.event.SubscriptionEvent;
import com.iqscaffold.billingservice.subscription.event.SubscriptionEventPublisher;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SubscriptionService for managing tenant subscription lifecycle.
 * <p>
 * This service coordinates subscription operations including:
 * <ul>
 *   <li>Creating new subscriptions with Stripe integration</li>
 *   <li>Managing state transitions (pause, resume, cancel)</li>
 *   <li>Synchronization with Stripe subscription state</li>
 *   <li>Audit trail management</li>
 * </ul>
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

  private final TenantSubscriptionRepository subscriptionRepository;
  private final SubscriptionPlanRepository planRepository;
  private final TenantSubscriptionAuditTrailRepository auditTrailRepository;
  private final GatewayConfigurationService gatewayConfigService;
  private final SubscriptionStateMachine stateMachine;
  private final SubscriptionEventPublisher eventPublisher;

  public SubscriptionServiceImpl(
      final TenantSubscriptionRepository subscriptionRepository,
      final SubscriptionPlanRepository planRepository,
      final TenantSubscriptionAuditTrailRepository auditTrailRepository,
      final GatewayConfigurationService gatewayConfigService,
      final SubscriptionStateMachine stateMachine,
      final SubscriptionEventPublisher eventPublisher) {
    this.subscriptionRepository = subscriptionRepository;
    this.planRepository = planRepository;
    this.auditTrailRepository = auditTrailRepository;
    this.gatewayConfigService = gatewayConfigService;
    this.stateMachine = stateMachine;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public SubscriptionDtos.SubscriptionResponse createSubscription(
      SubscriptionDtos.CreateSubscriptionRequest request) {
    // Validate tenant context exists
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    log.info("Creating subscription for current tenant, planId: {}", request.planId());

    // 1. Validate plan exists and is active
    SubscriptionPlan plan = planRepository.findById(request.planId())
        .orElseThrow(() -> new SubscriptionNotFoundException("Subscription plan not found: " + request.planId()));

    if (!plan.getIsActive()) {
      throw new IllegalArgumentException("Subscription plan is not active: " + request.planId());
    }

    // 2. Check for existing active subscription (schema-scoped)
    var existingActive = subscriptionRepository.findActive();
    if (existingActive.isPresent()) {
      throw new IllegalStateException("Tenant already has an active subscription");
    }

    // 3. Validate initial state transition
    stateMachine.validateTransition(null, SubscriptionStatus.INCOMPLETE);

    // 4. Get payment provider
    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();

    // 5. Create or get Stripe customer
    // In a real implementation, we'd store and retrieve customer ID from tenant metadata
    String stripeCustomerId = createOrGetStripeCustomer(paymentProvider, TenantContext.getCurrentTenantId());

    // 6. Determine trial period
    Integer trialDays = request.trialDays() != null ? request.trialDays() : plan.getTrialPeriodDays();

    // 7. Build metadata
    Map<String, String> metadata = new HashMap<>();
    metadata.put("tenant_id", TenantContext.getCurrentTenantId());
    metadata.put("plan_id", plan.getId().toString());
    if (request.metadata() != null) {
      metadata.putAll(request.metadata());
    }

    // 8. Create Stripe subscription
    String stripeSubscriptionId;
    try {
      stripeSubscriptionId = paymentProvider.createSubscription(
          stripeCustomerId,
          plan.getStripePriceId(),
          trialDays,
          metadata,
          UUID.randomUUID().toString());
    } catch (final Exception e) {
      log.error("Failed to create Stripe subscription", e);
      throw new RuntimeException("Failed to create subscription with payment provider", e);
    }

    // 9. Create local subscription record (no tenant_id needed - schema provides context)
    TenantSubscription subscription = new TenantSubscription();
    subscription.setPlan(plan);
    subscription.setStripeSubscriptionId(stripeSubscriptionId);
    subscription.setStripeCustomerId(stripeCustomerId);
    subscription.setStatus(SubscriptionStatus.ACTIVE); // Default to ACTIVE for now
    subscription.setCurrentPeriodStart(Instant.now());
    subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 86400L)); // 30 days default

    if (trialDays != null && trialDays > 0) {
      subscription.setTrialEnd(Instant.now().plusSeconds(trialDays * 86400L));
    }

    subscription = subscriptionRepository.save(subscription);

    // 10. Create audit trail
    createAuditTrail(subscription, null, subscription.getStatus(), "Subscription created");

    // 11. Publish event
    eventPublisher.publishSubscriptionCreated(
        SubscriptionEvent.created(
            subscription.getId(),
            TenantContext.getCurrentTenantId(),
            plan.getId(),
            plan.getName(),
            stripeSubscriptionId
        )
    );

    log.info("Created subscription: {}", subscription.getId());

    return mapToResponse(subscription);
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionDtos.SubscriptionResponse getSubscription(UUID id) {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    TenantSubscription subscription = subscriptionRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));

    // No tenant_id check needed - schema isolation ensures we can only see our own data
    return mapToResponse(subscription);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.Optional<SubscriptionDtos.SubscriptionResponse> getActiveSubscription() {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    return subscriptionRepository.findActive()
        .map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SubscriptionDtos.SubscriptionResponse> getSubscriptions(Pageable pageable) {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    // Simply findAll - schema routing ensures we only see current tenant's data
    return subscriptionRepository.findAll(pageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional
  public SubscriptionDtos.SubscriptionResponse updateSubscription(UUID id,
      SubscriptionDtos.UpdateSubscriptionRequest request) {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    TenantSubscription subscription = subscriptionRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));

    // No tenant check needed - schema isolation ensures we can only access our own data

    log.info("Updating subscription: {}", id);

    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();
    SubscriptionStatus oldStatus = subscription.getStatus();

    // Handle plan change
    if (request.newPlanId() != null) {
      SubscriptionPlan newPlan = planRepository.findById(request.newPlanId())
          .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found: " + request.newPlanId()));

      if (!newPlan.getIsActive()) {
        throw new IllegalArgumentException("Target plan is not active: " + request.newPlanId());
      }

      try {
        paymentProvider.updateSubscription(
            subscription.getStripeSubscriptionId(),
            newPlan.getStripePriceId(),
            request.metadata());
        subscription.setPlan(newPlan);
      } catch (final Exception e) {
        log.error("Failed to update subscription plan: {}", id, e);
        throw new RuntimeException("Failed to update subscription with payment provider", e);
      }
    }

    subscription = subscriptionRepository.save(subscription);
    createAuditTrail(subscription, oldStatus, subscription.getStatus(), "Subscription updated");

    // Publish event
    eventPublisher.publishSubscriptionUpdated(
        SubscriptionEvent.updated(
            subscription.getId(),
            TenantContext.getCurrentTenantId(),
            subscription.getPlan().getId(),
            subscription.getPlan().getName(),
            subscription.getStatus().name()
        )
    );

    log.info("Updated subscription: {}", id);
    return mapToResponse(subscription);
  }

  @Override
  @Transactional
  public SubscriptionDtos.SubscriptionResponse cancelSubscription(UUID id) {
    return cancelSubscriptionInternal(id, false);
  }

  @Override
  @Transactional
  public SubscriptionDtos.SubscriptionResponse cancelSubscriptionImmediately(UUID id) {
    return cancelSubscriptionInternal(id, true);
  }

  private SubscriptionDtos.SubscriptionResponse cancelSubscriptionInternal(UUID id, boolean immediately) {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    TenantSubscription subscription = subscriptionRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));

    // No tenant check needed - schema isolation ensures we can only access our own data

    log.info("Canceling subscription: {}, immediately: {}", id, immediately);

    SubscriptionStatus oldStatus = subscription.getStatus();
    stateMachine.validateTransition(oldStatus, SubscriptionStatus.CANCELED);

    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();

    try {
      paymentProvider.cancelSubscription(subscription.getStripeSubscriptionId(), !immediately);
      subscription.setStatus(SubscriptionStatus.CANCELED);
      subscription.setCanceledAt(Instant.now());
      subscription = subscriptionRepository.save(subscription);
      createAuditTrail(subscription, oldStatus, SubscriptionStatus.CANCELED,
          "Subscription canceled" + (immediately ? " immediately" : " at period end"));

      // Publish event
      eventPublisher.publishSubscriptionCanceled(
          SubscriptionEvent.canceled(
              subscription.getId(),
              TenantContext.getCurrentTenantId(),
              SubscriptionStatus.CANCELED.name()
          )
      );
    } catch (final Exception e) {
      log.error("Failed to cancel subscription: {}", id, e);
      throw new RuntimeException("Failed to cancel subscription with payment provider", e);
    }

    log.info("Canceled subscription: {}", id);
    return mapToResponse(subscription);
  }

  @Override
  @Transactional
  public SubscriptionDtos.SubscriptionResponse pauseSubscription(UUID id) {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    TenantSubscription subscription = subscriptionRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));

    // No tenant check needed - schema isolation ensures we can only access our own data

    log.info("Pausing subscription: {}", id);

    SubscriptionStatus oldStatus = subscription.getStatus();
    stateMachine.validateTransition(oldStatus, SubscriptionStatus.PAUSED);

    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();

    try {
      paymentProvider.pauseSubscription(subscription.getStripeSubscriptionId());
      subscription.setStatus(SubscriptionStatus.PAUSED);
      subscription = subscriptionRepository.save(subscription);
      createAuditTrail(subscription, oldStatus, SubscriptionStatus.PAUSED, "Subscription paused");

      // Publish event
      eventPublisher.publishSubscriptionPaused(
          SubscriptionEvent.paused(subscription.getId(), TenantContext.getCurrentTenantId())
      );
    } catch (final Exception e) {
      log.error("Failed to pause subscription: {}", id, e);
      throw new RuntimeException("Failed to pause subscription with payment provider", e);
    }

    log.info("Paused subscription: {}", id);
    return mapToResponse(subscription);
  }

  @Override
  @Transactional
  public SubscriptionDtos.SubscriptionResponse resumeSubscription(UUID id) {
    // Validate tenant context
    if (!TenantContext.hasTenantContext()) {
      throw new IllegalStateException("Tenant context is required");
    }

    TenantSubscription subscription = subscriptionRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));

    // No tenant check needed - schema isolation ensures we can only access our own data

    log.info("Resuming subscription: {}", id);

    SubscriptionStatus oldStatus = subscription.getStatus();
    stateMachine.validateTransition(oldStatus, SubscriptionStatus.ACTIVE);

    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();

    try {
      paymentProvider.resumeSubscription(subscription.getStripeSubscriptionId());
      subscription.setStatus(SubscriptionStatus.ACTIVE);
      subscription = subscriptionRepository.save(subscription);
      createAuditTrail(subscription, oldStatus, SubscriptionStatus.ACTIVE, "Subscription resumed");

      // Publish event
      eventPublisher.publishSubscriptionResumed(
          SubscriptionEvent.resumed(subscription.getId(), TenantContext.getCurrentTenantId())
      );
    } catch (Exception e) {
      log.error("Failed to resume subscription: {}", id, e);
      throw new RuntimeException("Failed to resume subscription with payment provider", e);
    }

    log.info("Resumed subscription: {}", id);
    return mapToResponse(subscription);
  }

  @Override
  @Transactional
  public void syncSubscriptionFromStripe(String stripeSubscriptionId) {
    TenantSubscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            "Subscription not found for Stripe ID: " + stripeSubscriptionId));

    log.info("Syncing subscription from Stripe: {}", stripeSubscriptionId);

    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();

    try {
      Object stripeSubscriptionObj = paymentProvider.getSubscription(stripeSubscriptionId);
      
      // In a real implementation, extract details from Stripe subscription object
      // For now, just log the sync
      log.info("Retrieved subscription from Stripe: {}", stripeSubscriptionId);

      // Keep existing status for now
      subscriptionRepository.save(subscription);
      log.info("Synced subscription: {}", subscription.getId());
    } catch (Exception e) {
      log.error("Failed to sync subscription from Stripe: {}", stripeSubscriptionId, e);
      throw new RuntimeException("Failed to sync subscription from Stripe", e);
    }
  }

  private String createOrGetStripeCustomer(PaymentProviderAdapter paymentProvider, String tenantId) {
    // In a real implementation, we'd fetch tenant details from tenant service
    // and check if customer ID already exists
    // For now, return a placeholder customer ID
    log.debug("Creating Stripe customer for tenant: {}", tenantId);
    
    // TODO: Implement actual customer creation via PaymentProviderAdapter
    // The interface doesn't currently have a createCustomer method
    // For now, return a placeholder
    return "cus_" + tenantId.replace("-", "").substring(0, Math.min(14, tenantId.length()));
  }

  private void createAuditTrail(TenantSubscription subscription, SubscriptionStatus oldStatus,
      SubscriptionStatus newStatus, String notes) {
    TenantSubscriptionAuditTrail audit = new TenantSubscriptionAuditTrail();
    audit.setTenantSubscription(subscription);
    audit.setOldStatus(oldStatus);
    audit.setNewStatus(newStatus);
    audit.setReason(notes);
    auditTrailRepository.save(audit);
  }

  private SubscriptionStatus mapStripeStatusToLocal(String stripeStatus) {
    return switch (stripeStatus.toLowerCase()) {
      case "incomplete" -> SubscriptionStatus.INCOMPLETE;
      case "trialing" -> SubscriptionStatus.TRIALING;
      case "active" -> SubscriptionStatus.ACTIVE;
      case "past_due" -> SubscriptionStatus.PAST_DUE;
      case "canceled" -> SubscriptionStatus.CANCELED;
      case "unpaid" -> SubscriptionStatus.UNPAID;
      case "paused" -> SubscriptionStatus.PAUSED;
      default -> {
        log.warn("Unknown Stripe status: {}, defaulting to ACTIVE", stripeStatus);
        yield SubscriptionStatus.ACTIVE;
      }
    };
  }

  private SubscriptionDtos.SubscriptionResponse mapToResponse(TenantSubscription subscription) {
    return new SubscriptionDtos.SubscriptionResponse(
        subscription.getId(),
        TenantContext.getCurrentTenantId(), // Get from context, not entity
        subscription.getPlan().getId(),
        subscription.getPlan().getName(),
        subscription.getStatus().name(),
        subscription.getStripeSubscriptionId(),
        subscription.getStripeCustomerId(),
        subscription.getCurrentPeriodStart(),
        subscription.getCurrentPeriodEnd(),
        subscription.getCanceledAt(),
        null, // trialStart - not in entity
        subscription.getTrialEnd(),
        subscription.getCreatedAt(),
        subscription.getUpdatedAt());
  }
}
