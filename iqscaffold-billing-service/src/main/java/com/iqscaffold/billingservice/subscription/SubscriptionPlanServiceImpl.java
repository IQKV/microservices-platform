package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  // Note: Stripe integration will be added when PaymentProviderAdapter is extended

  public SubscriptionPlanServiceImpl(final SubscriptionPlanRepository planRepository) {
    this.planRepository = planRepository;
  }

  @Override
  public SubscriptionPlan createPlan(final SubscriptionPlan plan) {
    log.debug("Creating subscription plan: {}", plan.getName());

    // Validate unique name
    if (planRepository.existsByName(plan.getName())) {
      throw new IllegalArgumentException("Plan with name '" + plan.getName() + "' already exists");
    }

    // Validate pricing
    if (plan.getAmount() == null || plan.getAmount().signum() < 0) {
      throw new IllegalArgumentException("Plan amount must be a positive value");
    }

    SubscriptionPlan savedPlan = planRepository.save(plan);
    log.info("Created subscription plan: {} with ID: {}", savedPlan.getName(), savedPlan.getId());

    return savedPlan;
  }

  @Override
  public SubscriptionPlan updatePlan(final UUID id, final SubscriptionPlan plan) {
    log.debug("Updating subscription plan: {}", id);

    SubscriptionPlan existingPlan = getPlan(id);

    // Update mutable fields
    if (plan.getName() != null) {
      existingPlan.setName(plan.getName());
    }
    if (plan.getDescription() != null) {
      existingPlan.setDescription(plan.getDescription());
    }
    if (plan.getFeatures() != null) {
      existingPlan.setFeatures(plan.getFeatures());
    }
    if (plan.getMaxUsers() != null) {
      existingPlan.setMaxUsers(plan.getMaxUsers());
    }
    if (plan.getMaxStorageGb() != null) {
      existingPlan.setMaxStorageGb(plan.getMaxStorageGb());
    }
    if (plan.getMaxApiCalls() != null) {
      existingPlan.setMaxApiCalls(plan.getMaxApiCalls());
    }
    if (plan.getMetadata() != null) {
      existingPlan.setMetadata(plan.getMetadata());
    }

    // Note: Price and interval are immutable once set (Stripe limitation)
    // To change pricing, create a new plan

    SubscriptionPlan updatedPlan = planRepository.save(existingPlan);
    log.info("Updated subscription plan: {}", updatedPlan.getId());

    return updatedPlan;
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionPlan getPlan(final UUID id) {
    return planRepository.findById(id)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with id: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubscriptionPlan> getActivePlans() {
    return planRepository.findByIsActiveTrue();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubscriptionPlan> getAllPlans() {
    return planRepository.findAll();
  }

  @Override
  public void deactivatePlan(final UUID id) {
    log.debug("Deactivating subscription plan: {}", id);

    SubscriptionPlan plan = getPlan(id);
    plan.setIsActive(false);
    planRepository.save(plan);

    log.info("Deactivated subscription plan: {}", id);
  }

  @Override
  public void activatePlan(final UUID id) {
    log.debug("Activating subscription plan: {}", id);

    SubscriptionPlan plan = getPlan(id);
    plan.setIsActive(true);
    planRepository.save(plan);

    log.info("Activated subscription plan: {}", id);
  }

  @Override
  public SubscriptionPlan syncPlanWithStripe(final UUID id) {
    log.debug("Syncing subscription plan with Stripe: {}", id);

    SubscriptionPlan plan = getPlan(id);

    // TODO: Implement Stripe Product and Price creation
    // This will be implemented once PaymentProviderAdapter is extended with subscription methods
    // 
    // Steps:
    // 1. Check if stripeProductId exists, if not create Stripe Product
    // 2. Check if stripePriceId exists, if not create Stripe Price
    // 3. Update plan with Stripe IDs
    //
    // Example:
    // if (plan.getStripeProductId() == null) {
    //   String productId = stripeProvider.createProduct(plan.getName(), plan.getDescription());
    //   plan.setStripeProductId(productId);
    // }
    // if (plan.getStripePriceId() == null) {
    //   String priceId = stripeProvider.createPrice(
    //     plan.getStripeProductId(), 
    //     plan.getAmount(), 
    //     plan.getCurrency(), 
    //     plan.getInterval()
    //   );
    //   plan.setStripePriceId(priceId);
    // }

    log.warn("Stripe sync not yet implemented for plan: {}", id);
    
    return planRepository.save(plan);
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionPlan findByStripePriceId(final String stripePriceId) {
    return planRepository.findByStripePriceId(stripePriceId)
        .orElseThrow(() -> new SubscriptionNotFoundException("Plan not found with Stripe price ID: " + stripePriceId));
  }
}
