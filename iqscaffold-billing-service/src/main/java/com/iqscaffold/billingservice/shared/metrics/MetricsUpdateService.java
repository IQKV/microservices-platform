package com.iqscaffold.billingservice.shared.metrics;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for updating business metrics periodically.
 * 
 * <p>Calculates and updates metrics like MRR, ARR, subscription counts,
 * and other business KPIs on a scheduled basis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsUpdateService {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingMetrics billingMetrics;

    /**
     * Update all business metrics.
     * 
     * <p>Runs every 5 minutes to keep metrics current.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional(readOnly = true)
    public void updateBusinessMetrics() {
        try {
            log.debug("Starting business metrics update");

            updateSubscriptionCounts();
            updateRevenueMetrics();

            log.debug("Business metrics update completed");
        } catch (Exception e) {
            log.error("Error updating business metrics", e);
        }
    }

    /**
     * Update subscription count metrics.
     */
    private void updateSubscriptionCounts() {
        var activeCount = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        var trialCount = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
        var canceledCount = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);

        billingMetrics.updateActiveSubscriptions(activeCount);
        billingMetrics.updateTrialSubscriptions(trialCount);
        billingMetrics.updateCanceledSubscriptions(canceledCount);

        log.debug("Updated subscription counts - Active: {}, Trial: {}, Canceled: {}", 
                activeCount, trialCount, canceledCount);
    }

    /**
     * Update revenue metrics (MRR and ARR).
     * 
     * <p>Calculates MRR from all active and trial subscriptions,
     * normalizing to monthly values. ARR is calculated as MRR * 12.
     */
    private void updateRevenueMetrics() {
        var activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        var trialSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.TRIAL);

        long totalMrrInCents = 0;

        // Calculate MRR from active subscriptions
        for (var subscription : activeSubscriptions) {
            var plan = subscription.getPlan();
            if (plan != null) {
                var monthlyAmount = normalizeToMonthly(
                        plan.getBasePrice(),
                        plan.getBillingCycle()
                );
                totalMrrInCents += monthlyAmount;
            }
        }

        // Include trial subscriptions (potential MRR)
        for (var subscription : trialSubscriptions) {
            var plan = subscription.getPlan();
            if (plan != null) {
                var monthlyAmount = normalizeToMonthly(
                        plan.getBasePrice(),
                        plan.getBillingCycle()
                );
                totalMrrInCents += monthlyAmount;
            }
        }

        long totalArrInCents = totalMrrInCents * 12;

        billingMetrics.updateMrr(totalMrrInCents);
        billingMetrics.updateArr(totalArrInCents);

        log.debug("Updated revenue metrics - MRR: {} cents, ARR: {} cents", 
                totalMrrInCents, totalArrInCents);
    }

    /**
     * Normalize price to monthly amount based on billing cycle.
     *
     * @param price the price in cents
     * @param billingCycle the billing cycle
     * @return monthly amount in cents
     */
    private long normalizeToMonthly(BigDecimal price, BillingCycle billingCycle) {
        if (price == null || billingCycle == null) {
            return 0;
        }

        return switch (billingCycle) {
            case MONTHLY -> price.longValue();
            case YEARLY -> price.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).longValue();
            case LIFETIME -> 0; // Lifetime subscriptions don't contribute to recurring revenue
        };
    }

    /**
     * Calculate churn rate.
     * 
     * <p>Churn rate = (Canceled subscriptions / Total subscriptions at start of period) * 100
     * 
     * @return churn rate as percentage
     */
    public double calculateChurnRate() {
        var totalActive = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        var totalCanceled = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);
        var totalSubscriptions = totalActive + totalCanceled;

        if (totalSubscriptions == 0) {
            return 0.0;
        }

        return (totalCanceled * 100.0) / totalSubscriptions;
    }

    /**
     * Calculate trial conversion rate.
     * 
     * <p>Conversion rate = (Converted trials / Total trials) * 100
     * 
     * @return trial conversion rate as percentage
     */
    public double calculateTrialConversionRate() {
        // This would require tracking trial conversions in the database
        // For now, return 0 as placeholder
        // TODO: Implement trial conversion tracking
        return 0.0;
    }

    /**
     * Calculate Average Revenue Per User (ARPU).
     * 
     * @return ARPU in cents
     */
    public long calculateArpu() {
        var activeCount = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        if (activeCount == 0) {
            return 0;
        }

        var activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        long totalRevenue = 0;

        for (var subscription : activeSubscriptions) {
            var plan = subscription.getPlan();
            if (plan != null) {
                totalRevenue += normalizeToMonthly(
                        plan.getBasePrice(),
                        plan.getBillingCycle()
                );
            }
        }

        return totalRevenue / activeCount;
    }

    /**
     * Calculate Customer Lifetime Value (CLV).
     * 
     * <p>Simplified CLV = ARPU * Average customer lifetime in months
     * <p>Assuming average lifetime of 24 months for this calculation
     * 
     * @return CLV in cents
     */
    public long calculateClv() {
        var arpu = calculateArpu();
        var averageLifetimeMonths = 24; // Assumption
        return arpu * averageLifetimeMonths;
    }
}
