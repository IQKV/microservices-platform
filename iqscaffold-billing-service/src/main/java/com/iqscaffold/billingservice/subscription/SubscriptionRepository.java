package com.iqscaffold.billingservice.subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Subscription aggregate.
 * 
 * <p>This repository provides methods to query and persist subscription aggregates.
 * All queries return fully reconstituted aggregates with their associated plan data.
 * 
 * <p>Uses text blocks (Java 21) for multi-line JPQL queries to improve readability.
 * Complex queries use the specification pattern for dynamic filtering.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long>, JpaSpecificationExecutor<Subscription> {

  /**
   * Finds the active subscription for a tenant.
   * Returns the subscription with ACTIVE, TRIAL, or PAST_DUE status.
   * 
   * @param tenantId tenant identifier
   * @return optional containing the active subscription, or empty if none exists
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.tenantId = :tenantId
        AND s.status IN ('ACTIVE', 'TRIAL', 'PAST_DUE')
      ORDER BY s.createdAt DESC
      """)
  Optional<Subscription> findActiveByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds all subscriptions for a tenant.
   * 
   * @param tenantId tenant identifier
   * @return list of subscriptions ordered by creation date descending
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.tenantId = :tenantId
      ORDER BY s.createdAt DESC
      """)
  List<Subscription> findByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds subscriptions by status.
   * 
   * @param status subscription status
   * @param pageable pagination information
   * @return page of subscriptions with the specified status
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status = :status
      ORDER BY s.createdAt DESC
      """)
  Page<Subscription> findByStatus(
      @Param("status") SubscriptionStatus status,
      Pageable pageable
  );

  /**
   * Finds subscriptions by tenant and status.
   * 
   * @param tenantId tenant identifier
   * @param status subscription status
   * @return list of subscriptions matching criteria
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.tenantId = :tenantId
        AND s.status = :status
      ORDER BY s.createdAt DESC
      """)
  List<Subscription> findByTenantIdAndStatus(
      @Param("tenantId") UUID tenantId,
      @Param("status") SubscriptionStatus status
  );

  /**
   * Finds subscriptions with trial ending soon.
   * Returns subscriptions in TRIAL status where trial end is within the specified days.
   * 
   * @param daysAhead number of days to look ahead
   * @param now current timestamp
   * @return list of subscriptions with trial ending soon
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status = 'TRIAL'
        AND s.trialEnd IS NOT NULL
        AND s.trialEnd > :now
        AND s.trialEnd <= :endDate
      ORDER BY s.trialEnd ASC
      """)
  List<Subscription> findTrialsEndingSoon(
      @Param("daysAhead") int daysAhead,
      @Param("now") LocalDateTime now,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Finds subscriptions with expired trials.
   * Returns subscriptions in TRIAL status where trial end has passed.
   * 
   * @param now current timestamp
   * @return list of subscriptions with expired trials
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status = 'TRIAL'
        AND s.trialEnd IS NOT NULL
        AND s.trialEnd <= :now
      ORDER BY s.trialEnd ASC
      """)
  List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);

  /**
   * Finds subscriptions with period ending soon.
   * Returns active subscriptions where current period end is within the specified days.
   * 
   * @param now current timestamp
   * @param endDate end of the look-ahead period
   * @return list of subscriptions with period ending soon
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status IN ('ACTIVE', 'PAST_DUE')
        AND s.currentPeriodEnd IS NOT NULL
        AND s.currentPeriodEnd > :now
        AND s.currentPeriodEnd <= :endDate
      ORDER BY s.currentPeriodEnd ASC
      """)
  List<Subscription> findPeriodsEndingSoon(
      @Param("now") LocalDateTime now,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Finds subscriptions with expired periods.
   * Returns subscriptions where current period end has passed.
   * 
   * @param now current timestamp
   * @return list of subscriptions with expired periods
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status IN ('ACTIVE', 'PAST_DUE')
        AND s.currentPeriodEnd IS NOT NULL
        AND s.currentPeriodEnd <= :now
      ORDER BY s.currentPeriodEnd ASC
      """)
  List<Subscription> findExpiredPeriods(@Param("now") LocalDateTime now);

  /**
   * Finds canceled subscriptions scheduled for expiration.
   * Returns subscriptions in CANCELED status with cancelAtPeriodEnd flag set.
   * 
   * @param now current timestamp
   * @return list of canceled subscriptions ready to expire
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status = 'CANCELED'
        AND s.cancelAtPeriodEnd = true
        AND s.currentPeriodEnd IS NOT NULL
        AND s.currentPeriodEnd <= :now
      ORDER BY s.currentPeriodEnd ASC
      """)
  List<Subscription> findCanceledSubscriptionsToExpire(@Param("now") LocalDateTime now);

  /**
   * Finds subscriptions by plan.
   * 
   * @param planId plan identifier
   * @param pageable pagination information
   * @return page of subscriptions for the specified plan
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan p
      WHERE p.id = :planId
      ORDER BY s.createdAt DESC
      """)
  Page<Subscription> findByPlanId(
      @Param("planId") Long planId,
      Pageable pageable
  );

  /**
   * Counts active subscriptions for a tenant.
   * 
   * @param tenantId tenant identifier
   * @return number of active subscriptions
   */
  @Query("""
      SELECT COUNT(s) FROM Subscription s
      WHERE s.tenantId = :tenantId
        AND s.status IN ('ACTIVE', 'TRIAL', 'PAST_DUE')
      """)
  long countActiveByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Counts subscriptions by status.
   * 
   * @param status subscription status
   * @return number of subscriptions with the specified status
   */
  @Query("""
      SELECT COUNT(s) FROM Subscription s
      WHERE s.status = :status
      """)
  long countByStatus(@Param("status") SubscriptionStatus status);

  /**
   * Checks if a tenant has an active subscription.
   * 
   * @param tenantId tenant identifier
   * @return true if tenant has an active subscription
   */
  @Query("""
      SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
      FROM Subscription s
      WHERE s.tenantId = :tenantId
        AND s.status IN ('ACTIVE', 'TRIAL', 'PAST_DUE')
      """)
  boolean existsActiveByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Finds subscriptions by user.
   * 
   * @param userId user identifier
   * @return list of subscriptions created by the user
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.userId = :userId
      ORDER BY s.createdAt DESC
      """)
  List<Subscription> findByUserId(@Param("userId") UUID userId);

  /**
   * Finds subscriptions by status list.
   * 
   * @param statuses list of subscription statuses
   * @return list of subscriptions with any of the specified statuses
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.status IN :statuses
      ORDER BY s.createdAt DESC
      """)
  List<Subscription> findByStatusIn(@Param("statuses") List<SubscriptionStatus> statuses);

  /**
   * Counts subscriptions by status list.
   * 
   * @param statuses list of subscription statuses
   * @return number of subscriptions with any of the specified statuses
   */
  @Query("""
      SELECT COUNT(s) FROM Subscription s
      WHERE s.status IN :statuses
      """)
  long countByStatusIn(@Param("statuses") List<SubscriptionStatus> statuses);

  /**
   * Finds subscriptions canceled between dates.
   * 
   * @param startDate start of date range
   * @param endDate end of date range
   * @return list of subscriptions canceled in the date range
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.canceledAt BETWEEN :startDate AND :endDate
      ORDER BY s.canceledAt DESC
      """)
  List<Subscription> findByCanceledAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Finds subscriptions created between dates.
   * 
   * @param startDate start of date range
   * @param endDate end of date range
   * @return list of subscriptions created in the date range
   */
  @Query("""
      SELECT s FROM Subscription s
      JOIN FETCH s.plan
      WHERE s.createdAt BETWEEN :startDate AND :endDate
      ORDER BY s.createdAt DESC
      """)
  List<Subscription> findByCreatedAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );
}
