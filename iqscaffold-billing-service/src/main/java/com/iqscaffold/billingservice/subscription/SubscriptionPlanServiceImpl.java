package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.UUID;

import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link SubscriptionPlanService}.
 * <p>
 * Manages subscription plans with Stripe synchronization capabilities.
 */
@Service
@Transactional
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanServiceImpl.class);

  private final SubscriptionPlanRepository planRepository;
  private final com.iqscaffold.billingservice.payment.PaymentProviderFactory paymentProviderFactory;

  public SubscriptionPlanServiceImpl(
      final SubscriptionPlanRepository planRepository,
      final com.iqscaffold.billingservice.payment.PaymentProviderFactory paymentProviderFactory) {
    this.planRepository = planRepository;
    this.paymentProviderFactory = paymentProviderFactory;
  }

  @Override
  public SubscriptionDtos.PlanResponse createPlan(final SubscriptionDtos.UpsertPlanRequest request) {
    log.debug("Creating subscription plan: {}", request.name());

    // Validate unique name
    if (planRepository.existsByName(request.name())) {
      throw new IllegalArgumentException("Plan with name '" + request.name() + "' already exists");
    }

    // Create entity from request
    SubscriptionPlan plan = new SubscriptionPlan();
    plan.setName(request.name());
    plan.setDescription(request.description());
    plan.setAmount(request.priceAmount());
    plan.setCurrency(request.currency());
    plan.setInterval(SubscriptionInterval.valueOf(request.interval().toUpperCase()));
    plan.setIntervalCount(request.intervalCount() != null ? request.intervalCount() : 1);
    plan.setTrialPeriodDays(request.trialDays() != null ? request.trialDays() : 0);
    plan.setIsActive(request.isActive() != null ? request.isActive() : true);

    if (request.features() != null) {
      plan.setFeatures(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(request.features()).toString());
    }
    
    if (request.metadata() != null) {
      plan.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(request.metadata()).toString());
    }

    SubscriptionPlan savedPlan = planRepository.save(plan);
    log.info("Created subscription plan: {} with ID: {}", savedPlan.getName(), savedPlan.getId());

    return mapToResponse(savedPlan);
  }

  @Override
  public SubscriptionDtos.PlanResponse updatePlan(final UUID id, final SubscriptionDtos.UpsertPlanRequest request) {
    log.debug("Updating subscription plan: {}", id);

    SubscriptionPlan existingPlan = planRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with id: " + id));

    // Update mutable fields
    if (request.name() != null) {
      existingPlan.setName(request.name());
    }
    if (request.description() != null) {
      existingPlan.setDescription(request.description());
    }
    if (request.isActive() != null) {
      existingPlan.setIsActive(request.isActive());
    }
    if (request.features() != null) {
      existingPlan.setFeatures(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(request.features()).toString());
    }
    if (request.metadata() != null) {
      existingPlan.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(request.metadata()).toString());
    }

    // Note: Price and interval are immutable once set (Stripe limitation)
    // To change pricing, create a new plan

    SubscriptionPlan updatedPlan = planRepository.save(existingPlan);
    log.info("Updated subscription plan: {}", updatedPlan.getId());

    return mapToResponse(updatedPlan);
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionDtos.PlanResponse getPlan(final UUID id) {
    SubscriptionPlan plan = planRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with id: " + id));
    return mapToResponse(plan);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubscriptionDtos.PlanResponse> getActivePlans() {
    return planRepository.findByIsActiveTrue().stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SubscriptionDtos.PlanResponse> getAllPlans(Pageable pageable) {
    return planRepository.findAll(pageable)
        .map(this::mapToResponse);
  }

  @Override
  public void deactivatePlan(final UUID id) {
    log.debug("Deactivating subscription plan: {}", id);

    SubscriptionPlan plan = planRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with id: " + id));
    plan.setIsActive(false);
    planRepository.save(plan);

    log.info("Deactivated subscription plan: {}", id);
  }

  @Override
  public void activatePlan(final UUID id) {
    log.debug("Activating subscription plan: {}", id);

    SubscriptionPlan plan = planRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with id: " + id));
    plan.setIsActive(true);
    planRepository.save(plan);

    log.info("Activated subscription plan: {}", id);
  }

  @Override
  public void syncPlanWithStripe(final UUID id) {
    log.debug("Syncing subscription plan with Stripe: {}", id);

    SubscriptionPlan plan = planRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with id: " + id));

    // Get the Stripe payment provider
    var provider = paymentProviderFactory.getProvider(com.iqscaffold.billingservice.shared.PaymentGatewayProvider.STRIPE);

    try {
      // Create Stripe Product if not exists
      if (plan.getStripeProductId() == null) {
        log.info("Creating Stripe Product for plan: {}", plan.getName());
        
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("plan_id", plan.getId().toString());
        
        String productId = provider.createProduct(
            plan.getName(),
            plan.getDescription(),
            metadata
        );
        
        plan.setStripeProductId(productId);
        log.info("Created Stripe Product: {}", productId);
      }

      // Create Stripe Price if not exists
      if (plan.getStripePriceId() == null) {
        log.info("Creating Stripe Price for plan: {}", plan.getName());
        
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("plan_id", plan.getId().toString());
        
        String priceId = provider.createPrice(
            plan.getStripeProductId(),
            plan.getAmount(),
            plan.getCurrency(),
            plan.getInterval().name(),
            plan.getIntervalCount(),
            metadata
        );
        
        plan.setStripePriceId(priceId);
        log.info("Created Stripe Price: {}", priceId);
      }

      planRepository.save(plan);
      log.info("Successfully synced plan {} with Stripe", id);
      
    } catch (final Exception e) {
      log.error("Failed to sync plan {} with Stripe: {}", id, e.getMessage(), e);
      throw new RuntimeException("Failed to sync plan with Stripe: " + e.getMessage(), e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionPlan findByStripePriceId(final String stripePriceId) {
    return planRepository.findByStripePriceId(stripePriceId)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with Stripe price ID: " + stripePriceId));
  }

  private SubscriptionDtos.PlanResponse mapToResponse(SubscriptionPlan plan) {
    return new SubscriptionDtos.PlanResponse(
        plan.getId(),
        plan.getName(),
        plan.getDescription(),
        plan.getAmount(),
        plan.getCurrency(),
        plan.getInterval().name(),
        plan.getIntervalCount(),
        plan.getTrialPeriodDays(),
        plan.getIsActive(),
        plan.getStripePriceId(),
        plan.getStripeProductId());
  }
}
