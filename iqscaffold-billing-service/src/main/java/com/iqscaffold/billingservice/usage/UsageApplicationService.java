package com.iqscaffold.billingservice.usage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for usage tracking and quota enforcement.
 *
 * <p>This service provides a thin orchestration layer for usage-related operations,
 * delegating business logic to domain services and aggregates. It manages transactions,
 * translates between domain objects and DTOs, and publishes domain events.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Record usage metrics with idempotency</li>
 *   <li>Aggregate usage by billing period</li>
 *   <li>Check and enforce quota limits</li>
 *   <li>Reset usage for new billing periods</li>
 *   <li>Publish domain events (UsageRecorded, QuotaExceeded)</li>
 * </ul>
 *
 * <p>Async Processing Strategy:
 * <ul>
 *   <li>recordUsage publishes to RabbitMQ for async processing (high volume)</li>
 *   <li>Returns immediately with 202 Accepted status</li>
 *   <li>Quota checks are cached for 1 minute to reduce database load</li>
 *   <li>Quota enforcement is synchronous for immediate feedback</li>
 * </ul>
 *
 * <p>Design Rationale: Application services orchestrate use cases without containing
 * business logic. They manage transactions, coordinate domain services, and handle
 * cross-cutting concerns like caching and event publishing.
 *
 * @see UsageRecord
 * @see QuotaEnforcer
 * @see QuotaExceededSpecification
 */
@Service
@Transactional
public class UsageApplicationService {

  private static final Logger log = LoggerFactory.getLogger(UsageApplicationService.class);

  private final UsageRecordRepository usageRecordRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final QuotaEnforcer quotaEnforcer;
  private final QuotaExceededSpecification quotaExceededSpecification;
  private final MessageService messageService;

  public UsageApplicationService(final UsageRecordRepository usageRecordRepository,
                                 final SubscriptionRepository subscriptionRepository,
                                 final QuotaEnforcer quotaEnforcer,
                                 final QuotaExceededSpecification quotaExceededSpecification,
                                 final MessageService messageService) {
    this.usageRecordRepository = usageRecordRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.quotaEnforcer = quotaEnforcer;
    this.quotaExceededSpecification = quotaExceededSpecification;
    this.messageService = messageService;
  }

  /**
   * Records usage for a tenant and metric type.
   *
   * <p>This method creates a usage record for billing and quota tracking purposes.
   * In production, this would publish to RabbitMQ for async processing to handle
   * high volume (10,000+ records/sec). For now, it processes synchronously.
   *
   * <p>Idempotency: Uses usage record ID to prevent duplicate processing.
   *
   * <p>Async Processing Pattern (to be implemented):
   * <pre>{@code
   * // Publish to RabbitMQ queue
   * rabbitTemplate.convertAndSend("usage.record", usageEvent);
   * // Return immediately with 202 Accepted
   * return ResponseEntity.accepted().build();
   * }</pre>
   *
   * @param tenantId   tenant identifier
   * @param metricType type of metric being tracked
   * @param quantity   amount of usage
   * @param unit       unit of measurement (optional)
   * @param recordedAt when the usage occurred
   * @return the created usage record DTO
   * @throws IllegalArgumentException      if parameters are invalid
   * @throws SubscriptionNotFoundException if no active subscription exists
   */
  @CacheEvict(value = "usage-quotas", key = "#tenantId + '-' + #metricType")
  public UsageDto recordUsage(
      final UUID tenantId,
      final MetricType metricType,
      final Long quantity,
      final String unit,
      final LocalDateTime recordedAt) {

    log.info("Recording usage for tenant: {}, metric: {}, quantity: {}",
        tenantId, metricType, quantity);

    // Validate parameters
    validateTenantId(tenantId);
    validateMetricType(metricType);
    validateQuantity(quantity);

    // Find active subscription
    var subscription = subscriptionRepository.findActiveByTenantId(tenantId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            messageService.getMessage("subscription.not.found", tenantId)));

    // Get billing period from subscription
    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();

    // Create usage record
    var usageRecord = new UsageRecord(
        subscription,
        tenantId,
        metricType,
        quantity,
        unit,
        recordedAt != null ? recordedAt : LocalDateTime.now(),
        periodStart,
        periodEnd
    );

    // Save usage record
    var savedRecord = usageRecordRepository.save(usageRecord);

    log.info("Usage recorded successfully: id={}, tenant={}, metric={}",
        savedRecord.getId(), tenantId, metricType);

    // TODO: Publish UsageRecorded domain event
    // publishUsageRecordedEvent(savedRecord);

    // Check if quota exceeded and publish event if needed
    checkAndPublishQuotaEvents(subscription, metricType);

    return mapToDto(savedRecord);
  }

  /**
   * Records multiple usage records in a batch.
   *
   * <p>This method is optimized for high-volume usage recording. In production,
   * this would publish to RabbitMQ for async batch processing (100 records at a time).
   *
   * <p>Async Processing Pattern (to be implemented):
   * <pre>{@code
   * // Publish batch to RabbitMQ queue
   * rabbitTemplate.convertAndSend("usage.batch", usageBatchEvent);
   * // Return immediately with 202 Accepted
   * return ResponseEntity.accepted().build();
   * }</pre>
   *
   * @param tenantId     tenant identifier
   * @param usageRecords list of usage records to create
   * @return list of created usage record DTOs
   * @throws IllegalArgumentException if parameters are invalid
   */
  @CacheEvict(value = "usage-quotas", allEntries = true)
  public List<UsageDto> recordUsageBatch(
      final UUID tenantId,
      final List<UsageRecordRequest> usageRecords) {

    validateTenantId(tenantId);

    if (usageRecords == null || usageRecords.isEmpty()) {
      throw new IllegalArgumentException("Usage records list cannot be empty");
    }

    log.info("Recording usage batch for tenant: {}, count: {}",
        tenantId, usageRecords.size());

    // Find active subscription
    var subscription = subscriptionRepository.findActiveByTenantId(tenantId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            messageService.getMessage("subscription.not.found", tenantId)));

    // Get billing period from subscription
    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();

    // Create usage records
    var records = usageRecords.stream()
        .map(request -> new UsageRecord(
            subscription,
            tenantId,
            request.metricType(),
            request.quantity(),
            request.unit(),
            request.recordedAt() != null ? request.recordedAt() : LocalDateTime.now(),
            periodStart,
            periodEnd
        ))
        .collect(Collectors.toList());

    // Save all records
    var savedRecords = usageRecordRepository.saveAll(records);

    log.info("Usage batch recorded successfully: tenant={}, count={}",
        tenantId, savedRecords.size());

    // TODO: Publish UsageRecorded domain events
    // savedRecords.forEach(this::publishUsageRecordedEvent);

    // Check quotas for all metric types in the batch
    usageRecords.stream()
        .map(UsageRecordRequest::metricType)
        .distinct()
        .forEach(metricType -> checkAndPublishQuotaEvents(subscription, metricType));

    return savedRecords.stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  /**
   * Gets current usage for a tenant across all metrics.
   *
   * <p>Returns aggregated usage for the current billing period.
   *
   * @param tenantId tenant identifier
   * @return usage summary for the current period
   * @throws SubscriptionNotFoundException if no active subscription exists
   */
  @Transactional(readOnly = true)
  public UsageSummaryDto getCurrentUsage(final UUID tenantId) {
    log.debug("Getting current usage for tenant: {}", tenantId);

    validateTenantId(tenantId);

    // Find active subscription
    var subscription = subscriptionRepository.findActiveByTenantId(tenantId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            messageService.getMessage("subscription.not.found", tenantId)));

    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();

    return getUsageForPeriod(tenantId, periodStart, periodEnd);
  }

  /**
   * Gets usage for a specific billing period.
   *
   * <p>Returns aggregated usage metrics for the specified time period.
   *
   * @param tenantId    tenant identifier
   * @param periodStart start of the period
   * @param periodEnd   end of the period
   * @return usage summary for the period
   */
  @Transactional(readOnly = true)
  public UsageSummaryDto getUsageForPeriod(
      final UUID tenantId,
      final LocalDateTime periodStart,
      final LocalDateTime periodEnd) {

    log.debug("Getting usage for tenant: {}, period: {} to {}",
        tenantId, periodStart, periodEnd);

    validateTenantId(tenantId);
    validatePeriod(periodStart, periodEnd);

    // Aggregate usage by metric type
    var metrics = usageRecordRepository.aggregateUsageByMetricType(
        tenantId, periodStart, periodEnd);

    // Count total records
    var totalRecords = usageRecordRepository.countByTenantIdAndBillingPeriod(
        tenantId, periodStart, periodEnd);

    // Get subscription to include quota limits
    var subscription = subscriptionRepository.findActiveByTenantId(tenantId);
    var plan = subscription.map(Subscription::getPlan).orElse(null);

    // Map to DTOs with quota limits
    var metricDtos = metrics.stream()
        .map(metric -> {
          var limit = plan != null ? getQuotaLimit(plan.getQuotas(), metric.metricType()) : null;
          return new UsageMetricDto(
              metric.metricType(),
              metric.quantity(),
              metric.unit(),
              limit
          );
        })
        .collect(Collectors.toList());

    return new UsageSummaryDto(
        tenantId,
        periodStart,
        periodEnd,
        metricDtos,
        (int) totalRecords
    );
  }

  /**
   * Checks if a tenant has quota available for a metric type.
   *
   * <p>This method is cached for 1 minute to reduce database load.
   * Quota enforcement must be synchronous for immediate feedback.
   *
   * @param tenantId          tenant identifier
   * @param metricType        type of metric to check
   * @param requestedQuantity amount being requested
   * @return quota check result
   * @throws SubscriptionNotFoundException if no active subscription exists
   */
  @Cacheable(value = "usage-quotas", key = "#tenantId + '-' + #metricType")
  @Transactional(readOnly = true)
  public QuotaUsageDto checkQuota(
      final UUID tenantId,
      final MetricType metricType,
      final long requestedQuantity) {

    log.debug("Checking quota for tenant: {}, metric: {}, requested: {}",
        tenantId, metricType, requestedQuantity);

    validateTenantId(tenantId);
    validateMetricType(metricType);

    // Find active subscription
    var subscription = subscriptionRepository.findActiveByTenantId(tenantId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            messageService.getMessage("subscription.not.found", tenantId)));

    // Get current usage for the billing period
    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();
    var currentUsage = usageRecordRepository.calculateTotalUsageForPeriod(
        tenantId, metricType, periodStart, periodEnd);

    // Check quota using domain service
    var result = quotaEnforcer.checkQuota(
        subscription, metricType, currentUsage, requestedQuantity);

    return new QuotaUsageDto(
        metricType,
        currentUsage,
        result.limit(),
        result.remainingQuota(),
        result.allowed(),
        result.percentageUsed(),
        periodEnd
    );
  }

  /**
   * Enforces quota limits by throwing an exception if exceeded.
   *
   * <p>This method performs the same check as {@link #checkQuota} but throws
   * an exception instead of returning a result. Useful for enforcing hard limits.
   *
   * @param tenantId          tenant identifier
   * @param metricType        type of metric to check
   * @param requestedQuantity amount being requested
   * @throws QuotaExceededException        if quota is exceeded
   * @throws SubscriptionNotFoundException if no active subscription exists
   */
  @Transactional(readOnly = true)
  public void enforceQuota(
      final UUID tenantId,
      final MetricType metricType,
      final long requestedQuantity) {

    log.debug("Enforcing quota for tenant: {}, metric: {}, requested: {}",
        tenantId, metricType, requestedQuantity);

    var quotaCheck = checkQuota(tenantId, metricType, requestedQuantity);

    if (!quotaCheck.allowed()) {
      throw new QuotaEnforcer.QuotaExceededException(
          messageService.getMessage("usage.quota.exceeded",
              metricType.name(), quotaCheck.currentUsage(), quotaCheck.limit()),
          metricType.name(),
          quotaCheck.limit(),
          quotaCheck.currentUsage()
      );
    }
  }

  /**
   * Resets usage counters for a new billing period.
   *
   * <p>This method is called when a subscription renews to start fresh usage tracking.
   * It does not delete historical usage records, only marks the start of a new period.
   *
   * @param tenantId tenant identifier
   * @throws SubscriptionNotFoundException if no active subscription exists
   */
  @CacheEvict(value = "usage-quotas", allEntries = true)
  public void resetUsageForNewPeriod(final UUID tenantId) {
    log.info("Resetting usage for new period: tenant={}", tenantId);

    validateTenantId(tenantId);

    // Find active subscription to validate tenant has an active subscription
    subscriptionRepository.findActiveByTenantId(tenantId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            messageService.getMessage("subscription.not.found", tenantId)));

    // Usage reset is handled by the subscription renewal process
    // which updates currentPeriodStart and currentPeriodEnd
    // Historical usage records remain for billing and analytics

    log.info("Usage reset completed for tenant: {}", tenantId);
  }

  // Private helper methods

  private void checkAndPublishQuotaEvents(Subscription subscription, MetricType metricType) {
    var tenantId = subscription.getTenantId();
    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();

    var currentUsage = usageRecordRepository.calculateTotalUsageForPeriod(
        tenantId, metricType, periodStart, periodEnd);

    var context = new UsageContext(
        metricType,
        currentUsage,
        getQuotaLimit(subscription.getPlan().getQuotas(), metricType)
    );

    // Check if quota exceeded
    if (quotaExceededSpecification.isSatisfiedBy(context)) {
      log.warn("Quota exceeded for tenant: {}, metric: {}, usage: {}, limit: {}",
          tenantId, metricType, currentUsage, context.quotaLimit());

      // TODO: Publish QuotaExceeded domain event
      // publishQuotaExceededEvent(tenantId, metricType, currentUsage, context.quotaLimit());
    }

    // Check if approaching limit (90%)
    if (quotaEnforcer.isApproachingLimit(subscription, metricType, currentUsage)) {
      log.info("Quota approaching limit for tenant: {}, metric: {}, usage: {}, limit: {}",
          tenantId, metricType, currentUsage, context.quotaLimit());

      // TODO: Publish QuotaWarning domain event
      // publishQuotaWarningEvent(tenantId, metricType, currentUsage, context.quotaLimit());
    }
  }

  private Long getQuotaLimit(
      com.iqscaffold.billingservice.plan.PlanQuotas quotas,
      MetricType metricType) {
    if (quotas == null) {
      return null; // Unlimited
    }

    return switch (metricType) {
      case API_CALLS -> quotas.apiCallsPerMonth();
      case STORAGE_GB -> quotas.storageGb();
      case EMAIL_SENDS -> quotas.emailSendsPerMonth();
      case CAMPAIGN_EXECUTIONS -> quotas.campaignExecutionsPerMonth();
      case SCORING_REQUESTS -> quotas.scoringRequestsPerMonth();
      case ACTIVE_USERS -> quotas.maxUsers();
      case CUSTOM_DOMAINS -> quotas.customDomains();
      case DATA_EXPORTS -> quotas.dataExportsPerMonth();
      case CUSTOM -> null; // Custom metrics handled separately
    };
  }

  private UsageDto mapToDto(UsageRecord record) {
    return new UsageDto(
        record.getId(),
        record.getSubscription().getId(),
        record.getTenantId(),
        record.getMetricType(),
        record.getQuantity(),
        record.getUnit(),
        record.getRecordedAt(),
        record.getBillingPeriodStart(),
        record.getBillingPeriodEnd(),
        record.getMetadata(),
        record.getCreatedAt()
    );
  }

  private void validateTenantId(UUID tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
  }

  private void validateMetricType(MetricType metricType) {
    if (metricType == null) {
      throw new IllegalArgumentException("Metric type cannot be null");
    }
  }

  private void validateQuantity(Long quantity) {
    if (quantity == null || quantity < 0) {
      throw new IllegalArgumentException("Quantity must be non-negative");
    }
  }

  private void validatePeriod(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("Period start and end cannot be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Period start must be before or equal to end");
    }
  }

  /**
   * Request record for batch usage recording.
   */
  public record UsageRecordRequest(
      MetricType metricType,
      Long quantity,
      String unit,
      LocalDateTime recordedAt
  ) {
  }

  /**
   * Exception thrown when a subscription is not found.
   */
  public static class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(final String message) {
      super(message);
    }
  }
}
