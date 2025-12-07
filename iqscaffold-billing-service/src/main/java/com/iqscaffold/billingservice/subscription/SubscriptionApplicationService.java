package com.iqscaffold.billingservice.subscription;

import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodRepository;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.plan.SubscriptionPlanRepository;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for subscription lifecycle orchestration.
 * 
 * <p>This service acts as a thin orchestration layer that:
 * <ul>
 *   <li>Delegates business logic to domain aggregates, factories, and services</li>
 *   <li>Manages transactions at the application service level</li>
 *   <li>Translates between domain objects and DTOs (records)</li>
 *   <li>Provides internationalized messages via MessageService</li>
 *   <li>Implements caching for subscription lookups with 5-minute TTL</li>
 *   <li>Publishes domain events for eventual consistency</li>
 * </ul>
 * 
 * <p>Following DDD principles, this service does not contain business logic.
 * All business rules and invariants are enforced by the Subscription aggregate,
 * SubscriptionFactory, and domain services.
 * 
 * <p>This implementation focuses on subscription creation and trial management.
 * Upgrade/downgrade and cancellation/reactivation are handled by separate methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionApplicationService {

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionPlanRepository subscriptionPlanRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final TenantTrialHistoryRepository tenantTrialHistoryRepository;
  private final SubscriptionFactory subscriptionFactory;
  private final SubscriptionLifecycleManager subscriptionLifecycleManager;
  private final TrialEligibilitySpecification trialEligibilitySpecification;
  private final MessageService messageService;
  private final com.iqscaffold.billingservice.billing.ProrationCalculator prorationCalculator;
  private final com.iqscaffold.billingservice.plan.ValidPlanTransitionSpecification validPlanTransitionSpecification;
  private final com.iqscaffold.billingservice.usage.QuotaExceededSpecification quotaExceededSpecification;
  private final com.iqscaffold.billingservice.usage.UsageRecordRepository usageRecordRepository;

  /**
   * Creates a new subscription for a tenant.
   * 
   * <p>This method handles both trial and paid subscription creation:
   * <ul>
   *   <li>If startTrial is true and plan offers trial, creates trial subscription</li>
   *   <li>If startTrial is false or plan doesn't offer trial, creates paid subscription</li>
   *   <li>Validates trial eligibility for trial subscriptions</li>
   *   <li>Validates payment method for paid subscriptions</li>
   *   <li>Ensures only one active subscription per tenant</li>
   *   <li>Publishes SubscriptionCreated and TrialStarted events</li>
   * </ul>
   * 
   * @param request subscription creation request
   * @return DTO representation of the created subscription
   * @throws SubscriptionException.SubscriptionAlreadyExistsException if tenant has active subscription
   * @throws PlanException.PlanNotFoundException if plan not found
   * @throws SubscriptionException.TrialNotEligibleException if trial not eligible
   * @throws com.iqscaffold.billingservice.shared.exception.PaymentException.PaymentMethodNotFoundException if payment method not found
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.SUBSCRIPTIONS, key = "#request.tenantId()")
  public SubscriptionDto createSubscription(CreateSubscriptionRequest request) {
    log.info(
        "Creating subscription for tenant: {}, plan: {}, startTrial: {}",
        request.tenantId(),
        request.planCode(),
        request.startTrial()
    );

    // Check if tenant already has an active subscription
    if (subscriptionRepository.existsActiveByTenantId(request.tenantId())) {
      var errorMessage = messageService.getMessage("subscription.already.exists");
      log.error("Tenant {} already has an active subscription", request.tenantId());
      throw new SubscriptionException.SubscriptionAlreadyExistsException(
          "Tenant already has an active subscription"
      );
    }

    // Retrieve the subscription plan
    var plan = subscriptionPlanRepository.findByPlanCode(request.planCode())
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", request.planCode());
          return new PlanException.PlanNotFoundException(request.planCode());
        });

    // Determine if this should be a trial subscription
    var shouldStartTrial = Boolean.TRUE.equals(request.startTrial()) && plan.hasTrial();

    Subscription subscription;

    if (shouldStartTrial) {
      // Create trial subscription
      subscription = createTrialSubscription(request.tenantId(), request.userId(), plan);
      log.info("Created trial subscription for tenant: {}", request.tenantId());
    } else {
      // Create paid subscription
      subscription = createPaidSubscription(
          request.tenantId(),
          request.userId(),
          plan,
          request.paymentMethodId()
      );
      log.info("Created paid subscription for tenant: {}", request.tenantId());
    }

    // Add metadata if provided
    if (request.metadata() != null && !request.metadata().isEmpty()) {
      request.metadata().forEach(subscription::addMetadata);
    }

    // Persist the subscription
    var savedSubscription = subscriptionRepository.save(subscription);

    // TODO: Publish domain events
    // - SubscriptionCreated event (always)
    // - TrialStarted event (if trial subscription)
    // Events will be published using BillingConstants.BillingEvents constants

    log.info(
        "Successfully created subscription: {} for tenant: {}, status: {}",
        savedSubscription.getId(),
        savedSubscription.getTenantId(),
        savedSubscription.getStatus()
    );

    // Return DTO
    return toDto(savedSubscription);
  }

  /**
   * Extends the trial period for a subscription.
   * 
   * <p>This is typically an admin action to give customers more time to evaluate.
   * Uses SubscriptionLifecycleManager domain service for state transition logic.
   * 
   * @param subscriptionId subscription identifier
   * @param additionalDays number of days to extend
   * @param reason reason for the extension
   * @return DTO representation of the updated subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if subscription not found
   * @throws IllegalStateException if subscription is not in TRIAL status
   * @throws IllegalArgumentException if additionalDays is negative
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.SUBSCRIPTIONS, allEntries = true)
  public SubscriptionDto extendTrial(Long subscriptionId, int additionalDays, String reason) {
    log.info(
        "Extending trial for subscription: {}, additionalDays: {}, reason: {}",
        subscriptionId,
        additionalDays,
        reason
    );

    // Retrieve the subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("Subscription not found: {}", subscriptionId);
          return new SubscriptionException.SubscriptionNotFoundException(subscriptionId.toString());
        });

    // Delegate to lifecycle manager for validation and state transition
    subscriptionLifecycleManager.extendTrial(subscription, additionalDays, reason);

    // Persist changes
    var updatedSubscription = subscriptionRepository.save(subscription);

    log.info(
        "Successfully extended trial for subscription: {}, new trial end: {}",
        updatedSubscription.getId(),
        updatedSubscription.getTrialEnd()
    );

    return toDto(updatedSubscription);
  }

  /**
   * Converts a trial subscription to active status.
   * 
   * <p>This is typically called when:
   * <ul>
   *   <li>Trial period ends and payment method is on file</li>
   *   <li>Customer manually converts trial to paid</li>
   * </ul>
   * 
   * <p>Uses SubscriptionLifecycleManager domain service for state transition logic.
   * 
   * @param subscriptionId subscription identifier
   * @return DTO representation of the updated subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if subscription not found
   * @throws IllegalStateException if subscription is not in TRIAL status
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.SUBSCRIPTIONS, allEntries = true)
  public SubscriptionDto convertTrialToActive(Long subscriptionId) {
    log.info("Converting trial to active for subscription: {}", subscriptionId);

    // Retrieve the subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("Subscription not found: {}", subscriptionId);
          return new SubscriptionException.SubscriptionNotFoundException(subscriptionId.toString());
        });

    // Delegate to lifecycle manager for validation and state transition
    subscriptionLifecycleManager.convertTrialToActive(subscription);

    // Persist changes
    var updatedSubscription = subscriptionRepository.save(subscription);

    // TODO: Publish TrialConverted domain event
    // Event will be published using BillingConstants.BillingEvents constants

    log.info(
        "Successfully converted trial to active for subscription: {}, new period end: {}",
        updatedSubscription.getId(),
        updatedSubscription.getCurrentPeriodEnd()
    );

    return toDto(updatedSubscription);
  }

  /**
   * Retrieves the active subscription for a tenant.
   * 
   * <p>Returns the subscription with ACTIVE, TRIAL, or PAST_DUE status.
   * Results are cached with 5-minute TTL to reduce database load.
   * 
   * @param tenantId tenant identifier
   * @return DTO representation of the active subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if no active subscription found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.SUBSCRIPTIONS, key = "#tenantId")
  public SubscriptionDto getActiveSubscription(UUID tenantId) {
    log.debug("Retrieving active subscription for tenant: {}", tenantId);

    var subscription = subscriptionRepository.findActiveByTenantId(tenantId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("No active subscription found for tenant: {}", tenantId);
          return new SubscriptionException.SubscriptionNotFoundException(
              "No active subscription for tenant: " + tenantId
          );
        });

    return toDto(subscription);
  }

  /**
   * Retrieves a subscription by ID.
   * 
   * <p>Results are cached with 5-minute TTL to reduce database load.
   * 
   * @param subscriptionId subscription identifier
   * @return DTO representation of the subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if subscription not found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.SUBSCRIPTIONS, key = "#subscriptionId")
  public SubscriptionDto getSubscription(Long subscriptionId) {
    log.debug("Retrieving subscription: {}", subscriptionId);

    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("Subscription not found: {}", subscriptionId);
          return new SubscriptionException.SubscriptionNotFoundException(subscriptionId.toString());
        });

    return toDto(subscription);
  }

  /**
   * Upgrades a subscription to a higher-tier plan with immediate proration.
   * 
   * <p>This method handles subscription upgrades by:
   * <ul>
   *   <li>Validating the plan transition using ValidPlanTransitionSpecification</li>
   *   <li>Calculating proration using ProrationCalculator domain service</li>
   *   <li>Applying the plan change immediately</li>
   *   <li>Publishing SubscriptionUpgraded domain event</li>
   * </ul>
   * 
   * <p>The upgrade is always immediate - the customer is charged the prorated
   * amount for the new plan for the remainder of the current period.
   * 
   * @param subscriptionId subscription identifier
   * @param request upgrade request containing new plan code and metadata
   * @return DTO representation of the upgraded subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if subscription not found
   * @throws PlanException.PlanNotFoundException if new plan not found
   * @throws PlanException.InvalidPlanTransitionException if transition is invalid
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.SUBSCRIPTIONS, allEntries = true)
  public SubscriptionDto upgradeSubscription(Long subscriptionId, UpdateSubscriptionRequest request) {
    log.info(
        "Upgrading subscription: {} to plan: {}",
        subscriptionId,
        request.newPlanCode()
    );

    // Retrieve the subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("Subscription not found: {}", subscriptionId);
          return new SubscriptionException.SubscriptionNotFoundException(subscriptionId.toString());
        });

    // Retrieve the new plan
    var newPlan = subscriptionPlanRepository.findByPlanCode(request.newPlanCode())
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", request.newPlanCode());
          return new PlanException.PlanNotFoundException(request.newPlanCode());
        });

    // Validate plan transition
    var currentPlan = subscription.getPlan();
    var transition = new com.iqscaffold.billingservice.plan.PlanTransition(currentPlan, newPlan);
    
    if (!validPlanTransitionSpecification.isSatisfiedBy(transition)) {
      var errorMessage = messageService.getMessage(
          "subscription.upgrade.invalid.transition",
          currentPlan.getName(),
          newPlan.getName()
      );
      log.error(
          "Invalid plan transition from {} to {} for subscription: {}",
          currentPlan.getName(),
          newPlan.getName(),
          subscriptionId
      );
      throw new PlanException.InvalidPlanTransitionException(
          currentPlan.getPlanCode(),
          newPlan.getPlanCode()
      );
    }

    // Verify this is actually an upgrade (new plan price > current plan price)
    if (newPlan.getBasePrice().compareTo(currentPlan.getBasePrice()) <= 0) {
      var errorMessage = messageService.getMessage("subscription.upgrade.not.higher.tier");
      log.error(
          "Cannot upgrade to lower or same tier plan. Current: {}, New: {}",
          currentPlan.getBasePrice(),
          newPlan.getBasePrice()
      );
      throw new PlanException.InvalidPlanTransitionException(
          currentPlan.getPlanCode(),
          newPlan.getPlanCode()
      );
    }

    // Calculate proration for immediate upgrade
    var prorationResult = prorationCalculator.calculateUpgrade(subscription, newPlan);
    
    log.info(
        "Proration calculated for subscription {}: credit={}, charge={}, net={}",
        subscriptionId,
        prorationResult.creditAmount(),
        prorationResult.chargeAmount(),
        prorationResult.netAmount()
    );

    // Apply the plan change
    subscription.changePlan(newPlan);

    // Add metadata if provided
    if (request.metadata() != null && !request.metadata().isEmpty()) {
      request.metadata().forEach(subscription::addMetadata);
    }

    // Add proration metadata
    subscription.addMetadata("last_upgrade_proration_credit", prorationResult.creditAmount().toString());
    subscription.addMetadata("last_upgrade_proration_charge", prorationResult.chargeAmount().toString());
    subscription.addMetadata("last_upgrade_proration_net", prorationResult.netAmount().toString());
    subscription.addMetadata("last_upgrade_from_plan", currentPlan.getPlanCode());
    subscription.addMetadata("last_upgrade_to_plan", newPlan.getPlanCode());

    // Persist changes
    var updatedSubscription = subscriptionRepository.save(subscription);

    // TODO: Publish SubscriptionUpgraded domain event
    // Event should include:
    // - subscriptionId
    // - tenantId
    // - fromPlanCode
    // - toPlanCode
    // - prorationAmount (net amount)
    // - effectiveDate (now)
    // Event will be published using BillingConstants.BillingEvents constants

    log.info(
        "Successfully upgraded subscription: {} from {} to {}, net charge: {}",
        updatedSubscription.getId(),
        currentPlan.getName(),
        newPlan.getName(),
        prorationResult.netAmount()
    );

    var successMessage = messageService.getMessage(
        "subscription.upgraded",
        newPlan.getName()
    );
    log.info(successMessage);

    return toDto(updatedSubscription);
  }

  /**
   * Downgrades a subscription to a lower-tier plan.
   * 
   * <p>This method handles subscription downgrades by:
   * <ul>
   *   <li>Validating the plan transition using ValidPlanTransitionSpecification</li>
   *   <li>Checking quota usage to ensure downgrade is safe using QuotaExceededSpecification</li>
   *   <li>Calculating proration using ProrationCalculator domain service</li>
   *   <li>Applying the plan change immediately or scheduling for period end</li>
   *   <li>Publishing SubscriptionDowngraded domain event</li>
   * </ul>
   * 
   * <p>Downgrades can be:
   * <ul>
   *   <li>Immediate: Applied right away with proration credit</li>
   *   <li>Scheduled: Applied at the end of the current billing period (no proration)</li>
   * </ul>
   * 
   * <p>Before allowing a downgrade, the method validates that current usage
   * doesn't exceed the new plan's quotas. If usage exceeds quotas, the downgrade
   * is rejected to prevent service disruption.
   * 
   * @param subscriptionId subscription identifier
   * @param request downgrade request containing new plan code, immediate flag, and metadata
   * @return DTO representation of the downgraded subscription
   * @throws SubscriptionException.SubscriptionNotFoundException if subscription not found
   * @throws PlanException.PlanNotFoundException if new plan not found
   * @throws PlanException.InvalidPlanTransitionException if transition is invalid
   * @throws com.iqscaffold.billingservice.shared.exception.UsageException.QuotaExceededException if current usage exceeds new plan quotas
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.SUBSCRIPTIONS, allEntries = true)
  public SubscriptionDto downgradeSubscription(Long subscriptionId, UpdateSubscriptionRequest request) {
    log.info(
        "Downgrading subscription: {} to plan: {}, immediate: {}",
        subscriptionId,
        request.newPlanCode(),
        request.immediate()
    );

    // Retrieve the subscription
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("subscription.not.found");
          log.error("Subscription not found: {}", subscriptionId);
          return new SubscriptionException.SubscriptionNotFoundException(subscriptionId.toString());
        });

    // Retrieve the new plan
    var newPlan = subscriptionPlanRepository.findByPlanCode(request.newPlanCode())
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", request.newPlanCode());
          return new PlanException.PlanNotFoundException(request.newPlanCode());
        });

    // Validate plan transition
    var currentPlan = subscription.getPlan();
    var transition = new com.iqscaffold.billingservice.plan.PlanTransition(currentPlan, newPlan);
    
    if (!validPlanTransitionSpecification.isSatisfiedBy(transition)) {
      var errorMessage = messageService.getMessage(
          "subscription.downgrade.invalid.transition",
          currentPlan.getName(),
          newPlan.getName()
      );
      log.error(
          "Invalid plan transition from {} to {} for subscription: {}",
          currentPlan.getName(),
          newPlan.getName(),
          subscriptionId
      );
      throw new PlanException.InvalidPlanTransitionException(
          currentPlan.getPlanCode(),
          newPlan.getPlanCode()
      );
    }

    // Verify this is actually a downgrade (new plan price < current plan price)
    if (newPlan.getBasePrice().compareTo(currentPlan.getBasePrice()) >= 0) {
      var errorMessage = messageService.getMessage("subscription.downgrade.not.lower.tier");
      log.error(
          "Cannot downgrade to higher or same tier plan. Current: {}, New: {}",
          currentPlan.getBasePrice(),
          newPlan.getBasePrice()
      );
      throw new PlanException.InvalidPlanTransitionException(
          currentPlan.getPlanCode(),
          newPlan.getPlanCode()
      );
    }

    // Validate that current usage doesn't exceed new plan quotas
    validateQuotasForDowngrade(subscription, newPlan);

    // Determine if immediate or scheduled
    var isImmediate = Boolean.TRUE.equals(request.immediate());

    if (isImmediate) {
      // Immediate downgrade with proration
      return applyImmediateDowngrade(subscription, currentPlan, newPlan, request);
    } else {
      // Scheduled downgrade at period end
      return scheduleDowngradeAtPeriodEnd(subscription, currentPlan, newPlan, request);
    }
  }

  // Private helper methods

  /**
   * Applies an immediate downgrade with proration.
   */
  private SubscriptionDto applyImmediateDowngrade(
      Subscription subscription,
      SubscriptionPlan currentPlan,
      SubscriptionPlan newPlan,
      UpdateSubscriptionRequest request) {

    // Calculate proration for immediate downgrade
    var prorationResult = prorationCalculator.calculateDowngrade(subscription, newPlan);
    
    log.info(
        "Proration calculated for subscription {}: credit={}, charge={}, net={}",
        subscription.getId(),
        prorationResult.creditAmount(),
        prorationResult.chargeAmount(),
        prorationResult.netAmount()
    );

    // Apply the plan change
    subscription.changePlan(newPlan);

    // Add metadata if provided
    if (request.metadata() != null && !request.metadata().isEmpty()) {
      request.metadata().forEach(subscription::addMetadata);
    }

    // Add proration metadata
    subscription.addMetadata("last_downgrade_proration_credit", prorationResult.creditAmount().toString());
    subscription.addMetadata("last_downgrade_proration_charge", prorationResult.chargeAmount().toString());
    subscription.addMetadata("last_downgrade_proration_net", prorationResult.netAmount().toString());
    subscription.addMetadata("last_downgrade_from_plan", currentPlan.getPlanCode());
    subscription.addMetadata("last_downgrade_to_plan", newPlan.getPlanCode());
    subscription.addMetadata("downgrade_type", "immediate");

    // Persist changes
    var updatedSubscription = subscriptionRepository.save(subscription);

    // TODO: Publish SubscriptionDowngraded domain event
    // Event should include:
    // - subscriptionId
    // - tenantId
    // - fromPlanCode
    // - toPlanCode
    // - creditAmount (absolute value of net amount)
    // - immediate (true)
    // Event will be published using BillingConstants.BillingEvents constants

    log.info(
        "Successfully downgraded subscription: {} from {} to {}, credit: {}",
        updatedSubscription.getId(),
        currentPlan.getName(),
        newPlan.getName(),
        prorationResult.getAbsoluteNetAmount()
    );

    var successMessage = messageService.getMessage(
        "subscription.downgraded.immediate",
        newPlan.getName(),
        prorationResult.getAbsoluteNetAmount()
    );
    log.info(successMessage);

    return toDto(updatedSubscription);
  }

  /**
   * Schedules a downgrade to occur at the end of the current billing period.
   */
  private SubscriptionDto scheduleDowngradeAtPeriodEnd(
      Subscription subscription,
      SubscriptionPlan currentPlan,
      SubscriptionPlan newPlan,
      UpdateSubscriptionRequest request) {

    // Store the scheduled plan change in metadata
    subscription.addMetadata("scheduled_plan_change", newPlan.getPlanCode());
    subscription.addMetadata("scheduled_plan_change_from", currentPlan.getPlanCode());
    subscription.addMetadata("scheduled_plan_change_at", subscription.getCurrentPeriodEnd().toString());
    subscription.addMetadata("downgrade_type", "scheduled");

    // Add additional metadata if provided
    if (request.metadata() != null && !request.metadata().isEmpty()) {
      request.metadata().forEach(subscription::addMetadata);
    }

    // Persist changes (plan change will be applied by scheduled job at period end)
    var updatedSubscription = subscriptionRepository.save(subscription);

    // TODO: Publish SubscriptionDowngraded domain event
    // Event should include:
    // - subscriptionId
    // - tenantId
    // - fromPlanCode
    // - toPlanCode
    // - creditAmount (0 for scheduled)
    // - immediate (false)
    // Event will be published using BillingConstants.BillingEvents constants

    log.info(
        "Successfully scheduled downgrade for subscription: {} from {} to {} at period end: {}",
        updatedSubscription.getId(),
        currentPlan.getName(),
        newPlan.getName(),
        subscription.getCurrentPeriodEnd()
    );

    var successMessage = messageService.getMessage(
        "subscription.downgraded.scheduled",
        newPlan.getName(),
        subscription.getCurrentPeriodEnd()
    );
    log.info(successMessage);

    return toDto(updatedSubscription);
  }

  /**
   * Validates that current usage doesn't exceed the new plan's quotas.
   * 
   * <p>This prevents downgrades that would immediately put the customer
   * over their quota limits, which would disrupt service.
   * 
   * @param subscription current subscription
   * @param newPlan target plan for downgrade
   * @throws com.iqscaffold.billingservice.shared.exception.UsageException.QuotaExceededException if usage exceeds new plan quotas
   */
  private void validateQuotasForDowngrade(Subscription subscription, SubscriptionPlan newPlan) {
    var tenantId = subscription.getTenantId();
    var newQuotas = newPlan.getQuotas();

    // Get current period usage for all metrics
    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();

    // Check each quota in the new plan
    for (var metricType : com.iqscaffold.billingservice.usage.MetricType.values()) {
      // Map MetricType to the appropriate quota field
      var quotaLimit = getQuotaLimitForMetric(newQuotas, metricType);
      
      // Skip unlimited quotas
      if (quotaLimit == null || quotaLimit == Long.MAX_VALUE) {
        continue;
      }

      // Get current usage for this metric
      var currentUsage = usageRecordRepository.calculateTotalUsageForPeriod(
          tenantId,
          metricType,
          periodStart,
          periodEnd
      );

      // Check if usage exceeds new quota
      var usageContext = new com.iqscaffold.billingservice.usage.UsageContext(
          metricType,
          currentUsage,
          quotaLimit
      );

      if (quotaExceededSpecification.isSatisfiedBy(usageContext)) {
        var errorMessage = messageService.getMessage(
            "subscription.downgrade.quota.exceeded",
            metricType.name(),
            String.valueOf(currentUsage),
            String.valueOf(quotaLimit)
        );
        log.error(
            "Cannot downgrade subscription {}: current usage {} exceeds new plan quota {} for metric {}",
            subscription.getId(),
            currentUsage,
            quotaLimit,
            metricType
        );
        throw new com.iqscaffold.billingservice.shared.exception.UsageException.QuotaExceededException(
            metricType.name(),
            quotaLimit,
            currentUsage
        );
      }
    }

    log.info(
        "Quota validation passed for downgrade of subscription: {}",
        subscription.getId()
    );
  }

  /**
   * Maps a MetricType to the corresponding quota limit in PlanQuotas.
   * 
   * @param quotas the plan quotas
   * @param metricType the metric type
   * @return the quota limit for the metric, or null if unlimited
   */
  private Long getQuotaLimitForMetric(com.iqscaffold.billingservice.plan.PlanQuotas quotas, com.iqscaffold.billingservice.usage.MetricType metricType) {
    return switch (metricType) {
      case ACTIVE_USERS -> quotas.maxUsers();
      case STORAGE_GB -> quotas.storageGb();
      case API_CALLS -> quotas.apiCallsPerMonth();
      case EMAIL_SENDS -> quotas.emailSendsPerMonth();
      case CAMPAIGN_EXECUTIONS -> quotas.campaignExecutionsPerMonth();
      case SCORING_REQUESTS -> quotas.scoringRequestsPerMonth();
      case CUSTOM_DOMAINS -> quotas.customDomains();
      case DATA_EXPORTS -> quotas.dataExportsPerMonth();
      case CUSTOM -> null; // Custom metrics don't have predefined quotas
    };
  }

  /**
   * Creates a trial subscription using the SubscriptionFactory.
   * 
   * <p>Validates trial eligibility and creates/updates trial history.
   * 
   * @param tenantId tenant identifier
   * @param userId user identifier
   * @param plan subscription plan
   * @return created trial subscription
   * @throws SubscriptionException.TrialNotEligibleException if trial not eligible
   */
  private Subscription createTrialSubscription(UUID tenantId, UUID userId, SubscriptionPlan plan) {
    // Get or create trial history for tenant
    var trialHistory = tenantTrialHistoryRepository.findByTenantId(tenantId)
        .orElseGet(() -> {
          var newHistory = TenantTrialHistory.create(tenantId);
          return tenantTrialHistoryRepository.save(newHistory);
        });

    // Validate trial eligibility using specification
    if (!trialEligibilitySpecification.isSatisfiedBy(trialHistory)) {
      var errorMessage = messageService.getMessage("subscription.trial.not.eligible");
      log.error("Tenant {} is not eligible for trial", tenantId);
      throw new SubscriptionException.TrialNotEligibleException(
          "Tenant has already used their trial period"
      );
    }

    // Create trial subscription using factory
    var subscription = subscriptionFactory.createTrialSubscription(
        tenantId,
        userId,
        plan,
        trialHistory
    );

    // Mark trial as used in history
    trialHistory.markTrialUsed();
    tenantTrialHistoryRepository.save(trialHistory);

    return subscription;
  }

  /**
   * Creates a paid subscription using the SubscriptionFactory.
   * 
   * <p>Validates payment method and creates active subscription.
   * 
   * @param tenantId tenant identifier
   * @param userId user identifier
   * @param plan subscription plan
   * @param paymentMethodId payment method identifier (optional)
   * @return created paid subscription
   * @throws com.iqscaffold.billingservice.shared.exception.PaymentException.PaymentMethodNotFoundException if payment method not found
   */
  private Subscription createPaidSubscription(
      UUID tenantId,
      UUID userId,
      SubscriptionPlan plan,
      Long paymentMethodId) {

    // If payment method ID is provided, validate it
    if (paymentMethodId != null) {
      var paymentMethod = paymentMethodRepository.findById(paymentMethodId)
          .orElseThrow(() -> {
            var errorMessage = messageService.getMessage("payment.method.not.found");
            log.error("Payment method not found: {}", paymentMethodId);
            return new com.iqscaffold.billingservice.shared.exception.PaymentException.PaymentMethodNotFoundException(
                paymentMethodId.toString()
            );
          });

      // Create paid subscription with payment method
      return subscriptionFactory.createPaidSubscription(tenantId, userId, plan, paymentMethod);
    } else {
      // Create active subscription without payment method
      // This is allowed for FREE plans or manual billing
      return subscriptionFactory.createPaidSubscription(
          tenantId,
          userId,
          plan,
          null // No payment method required for FREE plans
      );
    }
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
    var plan = subscription.getPlan();

    return new SubscriptionDto(
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getUserId(),
        plan.getPlanCode(),
        plan.getName(),
        plan.getTier(),
        plan.getBillingCycle(),
        plan.getBasePrice(),
        plan.getCurrency(),
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
