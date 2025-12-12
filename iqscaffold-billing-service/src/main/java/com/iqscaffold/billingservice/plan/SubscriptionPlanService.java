package com.iqscaffold.billingservice.plan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for subscription plan orchestration.
 *
 * <p>This service acts as a thin orchestration layer that:
 * <ul>
 *   <li>Delegates business logic to domain aggregates and services</li>
 *   <li>Manages transactions at the application service level</li>
 *   <li>Translates between domain objects and DTOs</li>
 *   <li>Provides internationalized messages via MessageService</li>
 *   <li>Implements caching for plan lookups with 1-hour TTL</li>
 * </ul>
 *
 * <p>Following DDD principles, this service does not contain business logic.
 * All business rules and invariants are enforced by the SubscriptionPlan aggregate.
 */
@Service
public class SubscriptionPlanService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanService.class);

  private final SubscriptionPlanRepository subscriptionPlanRepository;
  private final MessageService messageService;
  private final ValidPlanTransitionSpecification validPlanTransitionSpecification;

  public SubscriptionPlanService(final SubscriptionPlanRepository subscriptionPlanRepository,
                                 final MessageService messageService,
                                 final ValidPlanTransitionSpecification validPlanTransitionSpecification) {
    this.subscriptionPlanRepository = subscriptionPlanRepository;
    this.messageService = messageService;
    this.validPlanTransitionSpecification = validPlanTransitionSpecification;
  }

  /**
   * Creates a new subscription plan.
   *
   * <p>Delegates to the SubscriptionPlan aggregate factory method for creation
   * and validation. Returns an internationalized success message.
   *
   * @param planCode     unique plan identifier
   * @param name         display name
   * @param description  detailed description
   * @param tier         plan tier (FREE, PRO, ENTERPRISE)
   * @param billingCycle billing frequency (MONTHLY, YEARLY, LIFETIME)
   * @param basePrice    price per billing cycle
   * @param currency     currency code (e.g., USD)
   * @param features     feature flags map
   * @param quotas       quota limits
   * @param trialDays    trial period in days
   * @param publicPlan   whether publicly visible
   * @return DTO representation of the created plan
   * @throws IllegalArgumentException if validation fails
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.PLANS, allEntries = true)
  public SubscriptionPlanDto createPlan(
      String planCode,
      String name,
      String description,
      PlanTier tier,
      BillingCycle billingCycle,
      BigDecimal basePrice,
      String currency,
      Map<String, Object> features,
      PlanQuotas quotas,
      Integer trialDays,
      Boolean publicPlan) {

    log.info("Creating subscription plan with code: {}", planCode);

    // Check if plan code already exists
    if (subscriptionPlanRepository.existsByPlanCode(planCode)) {
      var errorMessage = messageService.getMessage("error.conflict");
      log.error("Plan code already exists: {}", planCode);
      throw new IllegalArgumentException(errorMessage);
    }

    // Delegate to aggregate factory method for creation and validation
    var plan = SubscriptionPlan.create(
        planCode,
        name,
        description,
        tier,
        billingCycle,
        basePrice,
        currency,
        features,
        quotas,
        trialDays,
        publicPlan
    );

    // Persist the aggregate
    var savedPlan = subscriptionPlanRepository.save(plan);

    log.info("Successfully created plan: {} (ID: {})", savedPlan.getPlanCode(), savedPlan.getId());

    // Return DTO with success message logged
    return toDto(savedPlan);
  }

  /**
   * Updates an existing subscription plan.
   *
   * <p>Updates plan details, features, quotas, and pricing. Price changes only
   * affect new subscriptions (existing subscriptions maintain their original price).
   *
   * @param planId      plan identifier
   * @param name        new display name
   * @param description new description
   * @param features    new feature flags
   * @param quotas      new quota limits
   * @param basePrice   new price
   * @param currency    currency code
   * @return DTO representation of the updated plan
   * @throws PlanException.PlanNotFoundException if plan not found
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.PLANS, allEntries = true)
  public SubscriptionPlanDto updatePlan(
      Long planId,
      String name,
      String description,
      Map<String, Object> features,
      PlanQuotas quotas,
      BigDecimal basePrice,
      String currency) {

    log.info("Updating subscription plan: {}", planId);

    // Retrieve the aggregate
    var plan = subscriptionPlanRepository.findById(planId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", planId);
          return new PlanException.PlanNotFoundException(planId.toString());
        });

    // Delegate updates to aggregate methods
    plan.updateDetails(name, description);
    plan.updateFeatures(features);
    plan.updateQuotas(quotas);
    plan.updatePricing(basePrice, currency);

    // Persist changes
    var updatedPlan = subscriptionPlanRepository.save(plan);

    log.info("Successfully updated plan: {} (ID: {})", updatedPlan.getPlanCode(), updatedPlan.getId());

    return toDto(updatedPlan);
  }

  /**
   * Archives (deactivates) a subscription plan.
   *
   * <p>Archived plans are no longer available for new subscriptions but
   * existing subscriptions remain active. Plans with active subscriptions
   * can still be archived.
   *
   * @param planId plan identifier
   * @throws PlanException.PlanNotFoundException if plan not found
   */
  @Transactional
  @CacheEvict(value = BillingConstants.CacheNames.PLANS, allEntries = true)
  public void archivePlan(Long planId) {
    log.info("Archiving subscription plan: {}", planId);

    // Retrieve the aggregate
    var plan = subscriptionPlanRepository.findById(planId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", planId);
          return new PlanException.PlanNotFoundException(planId.toString());
        });

    // Delegate to aggregate method
    plan.deactivate();

    // Persist changes
    subscriptionPlanRepository.save(plan);

    log.info("Successfully archived plan: {} (ID: {})", plan.getPlanCode(), plan.getId());
  }

  /**
   * Retrieves a subscription plan by ID.
   *
   * <p>Results are cached with 1-hour TTL to reduce database load.
   *
   * @param planId plan identifier
   * @return DTO representation of the plan
   * @throws PlanException.PlanNotFoundException if plan not found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.PLANS, key = "#planId")
  public SubscriptionPlanDto getPlan(Long planId) {
    log.debug("Retrieving subscription plan: {}", planId);

    var plan = subscriptionPlanRepository.findById(planId)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", planId);
          return new PlanException.PlanNotFoundException(planId.toString());
        });

    return toDto(plan);
  }

  /**
   * Retrieves a subscription plan by plan code.
   *
   * <p>Results are cached with 1-hour TTL to reduce database load.
   *
   * @param planCode unique plan identifier
   * @return DTO representation of the plan
   * @throws PlanException.PlanNotFoundException if plan not found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.PLANS, key = "#planCode")
  public SubscriptionPlanDto getPlanByCode(String planCode) {
    log.debug("Retrieving subscription plan by code: {}", planCode);

    var plan = subscriptionPlanRepository.findByPlanCode(planCode)
        .orElseThrow(() -> {
          var errorMessage = messageService.getMessage("plan.not.found");
          log.error("Plan not found: {}", planCode);
          return new PlanException.PlanNotFoundException(planCode);
        });

    return toDto(plan);
  }

  /**
   * Retrieves all active public subscription plans.
   *
   * <p>Returns plans that are both active and publicly visible, ordered by
   * tier and price. Results are cached with 1-hour TTL.
   *
   * @return list of public plan DTOs
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.PLANS, key = "'public-plans'")
  public List<SubscriptionPlanDto> getPublicPlans() {
    log.debug("Retrieving all public subscription plans");

    var plans = subscriptionPlanRepository.findActivePublicPlans();

    log.debug("Found {} public plans", plans.size());

    return plans.stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Retrieves all active subscription plans (public and private).
   *
   * <p>Used for admin purposes. Returns all active plans ordered by tier
   * and price. Results are cached with 1-hour TTL.
   *
   * @return list of all active plan DTOs
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.PLANS, key = "'all-active-plans'")
  public List<SubscriptionPlanDto> getAllPlans() {
    log.debug("Retrieving all active subscription plans");

    var plans = subscriptionPlanRepository.findActivePlans();

    log.debug("Found {} active plans", plans.size());

    return plans.stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Retrieves plans filtered by billing cycle.
   *
   * <p>Returns active plans with the specified billing cycle, ordered by
   * tier and price. Results are cached with 1-hour TTL.
   *
   * @param billingCycle billing frequency filter
   * @return list of filtered plan DTOs
   */
  @Transactional(readOnly = true)
  @Cacheable(value = BillingConstants.CacheNames.PLANS, key = "'billing-cycle-' + #billingCycle")
  public List<SubscriptionPlanDto> getPlansByBillingCycle(BillingCycle billingCycle) {
    log.debug("Retrieving plans by billing cycle: {}", billingCycle);

    var plans = subscriptionPlanRepository.findByBillingCycle(billingCycle);

    log.debug("Found {} plans with billing cycle {}", plans.size(), billingCycle);

    return plans.stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Validates a plan transition using the ValidPlanTransitionSpecification.
   *
   * <p>Checks if transitioning from one plan to another is valid according
   * to business rules. Throws an exception if the transition is invalid.
   *
   * @param fromPlanId source plan identifier
   * @param toPlanId   target plan identifier
   * @throws PlanException.PlanNotFoundException          if either plan not found
   * @throws PlanException.InvalidPlanTransitionException if transition invalid
   */
  @Transactional(readOnly = true)
  public void validatePlanTransition(Long fromPlanId, Long toPlanId) {
    log.debug("Validating plan transition from {} to {}", fromPlanId, toPlanId);

    var fromPlan = subscriptionPlanRepository.findById(fromPlanId)
        .orElseThrow(() -> {
          log.error("Source plan not found: {}", fromPlanId);
          return new PlanException.PlanNotFoundException(fromPlanId.toString());
        });

    var toPlan = subscriptionPlanRepository.findById(toPlanId)
        .orElseThrow(() -> {
          log.error("Target plan not found: {}", toPlanId);
          return new PlanException.PlanNotFoundException(toPlanId.toString());
        });

    var transition = new PlanTransition(fromPlan, toPlan);

    // Use specification for validation
    if (!validPlanTransitionSpecification.isSatisfiedBy(transition)) {
      log.error("Invalid plan transition from {} to {}", fromPlan.getPlanCode(), toPlan.getPlanCode());
      throw new PlanException.InvalidPlanTransitionException(
          fromPlan.getPlanCode(),
          toPlan.getPlanCode()
      );
    }

    log.debug("Plan transition validated successfully");
  }

  /**
   * Translates a SubscriptionPlan aggregate to a DTO.
   *
   * <p>This method handles the translation between the domain layer and
   * the presentation layer, ensuring proper separation of concerns.
   *
   * @param plan domain aggregate
   * @return DTO representation
   */
  private SubscriptionPlanDto toDto(SubscriptionPlan plan) {
    return new SubscriptionPlanDto(
        plan.getId(),
        plan.getPlanCode(),
        plan.getName(),
        plan.getDescription(),
        plan.getTier(),
        plan.getBillingCycle(),
        plan.getBasePrice(),
        plan.getCurrency(),
        plan.getFeatures(),
        plan.getQuotas(),
        plan.getTrialDays(),
        plan.getActive(),
        plan.getPublicPlan(),
        plan.getCreatedAt(),
        plan.getUpdatedAt()
    );
  }
}
