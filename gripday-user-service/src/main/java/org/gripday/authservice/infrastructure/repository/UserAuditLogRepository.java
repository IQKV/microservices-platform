package org.gripday.authservice.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.gripday.authservice.infrastructure.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UserAuditLog entity operations. Provides data access methods for security audit trail functionality.
 */
@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

  /**
   * Find audit logs for a specific user.
   *
   * @param userId   the user ID to search audit logs for
   * @param pageable pagination information
   * @return Page of audit logs for the user
   */
  Page<UserAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * Find audit logs for a specific tenant.
   *
   * @param tenantId the tenant identifier
   * @param pageable pagination information
   * @return Page of audit logs for the tenant
   */
  Page<UserAuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

  /**
   * Find audit logs by action type.
   *
   * @param action   the action type to search for
   * @param pageable pagination information
   * @return Page of audit logs with the specified action
   */
  Page<UserAuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

  /**
   * Find audit logs for a user within a tenant.
   *
   * @param userId   the user ID
   * @param tenantId the tenant identifier
   * @param pageable pagination information
   * @return Page of audit logs for the user in the tenant
   */
  Page<UserAuditLog> findByUserIdAndTenantIdOrderByCreatedAtDesc(Long userId, String tenantId, Pageable pageable);

  /**
   * Find audit logs by tenant and action type.
   *
   * @param tenantId the tenant identifier
   * @param action   the action type
   * @param pageable pagination information
   * @return Page of audit logs for the tenant and action
   */
  Page<UserAuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(String tenantId, String action, Pageable pageable);

  /**
   * Find audit logs within a date range for a tenant.
   *
   * @param tenantId  the tenant identifier
   * @param startDate the start date
   * @param endDate   the end date
   * @param pageable  pagination information
   * @return Page of audit logs within the date range
   */
  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.tenantId = :tenantId 
        AND al.createdAt >= :startDate 
        AND al.createdAt <= :endDate
      ORDER BY al.createdAt DESC
      """)
  Page<UserAuditLog> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  /**
   * Find security-related audit logs for a tenant.
   *
   * @param tenantId the tenant identifier
   * @param pageable pagination information
   * @return Page of security audit logs
   */
  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.tenantId = :tenantId 
        AND (al.action LIKE 'LOGIN_%' 
          OR al.action LIKE 'LOGOUT_%' 
          OR al.action LIKE 'PASSWORD_%'
          OR al.action LIKE 'ACCOUNT_%')
      ORDER BY al.createdAt DESC
      """)
  Page<UserAuditLog> findSecurityAuditLogsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

  /**
   * Find failed login attempts for a user within a time window.
   *
   * @param userId   the user ID
   * @param tenantId the tenant identifier
   * @param since    the time threshold
   * @return List of failed login attempts
   */
  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.userId = :userId 
        AND al.tenantId = :tenantId 
        AND al.action = 'LOGIN_FAILURE'
        AND al.createdAt >= :since
      ORDER BY al.createdAt DESC
      """)
  List<UserAuditLog> findFailedLoginAttempts(@Param("userId") Long userId,
      @Param("tenantId") String tenantId,
      @Param("since") LocalDateTime since);

  /**
   * Count audit logs by action type for a tenant.
   *
   * @param tenantId the tenant identifier
   * @param action   the action type
   * @return count of audit logs
   */
  long countByTenantIdAndAction(String tenantId, String action);

  /**
   * Find recent audit logs for a user.
   *
   * @param userId   the user ID
   * @param tenantId the tenant identifier
   * @param limit    the maximum number of records to return
   * @return List of recent audit logs
   */
  @Query(value = """
      SELECT al FROM UserAuditLog al 
      WHERE al.userId = :userId 
        AND al.tenantId = :tenantId
      ORDER BY al.createdAt DESC
      LIMIT :limit
      """)
  List<UserAuditLog> findRecentAuditLogs(@Param("userId") Long userId,
      @Param("tenantId") String tenantId,
      @Param("limit") int limit);

  /**
   * Find latest audit log for a user.
   *
   * @param userId the user ID
   * @return Optional containing the latest audit log if found
   */
  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.userId = :userId
      ORDER BY al.createdAt DESC
      LIMIT 1
      """)
  java.util.Optional<UserAuditLog> findLatestByUserId(@Param("userId") Long userId);

  /**
   * Delete old audit logs before a specific date for a tenant.
   *
   * @param tenantId   the tenant identifier
   * @param beforeDate the cutoff date
   * @return number of deleted records
   */
  @Query("""
      DELETE FROM UserAuditLog al 
      WHERE al.tenantId = :tenantId 
        AND al.createdAt < :beforeDate
      """)
  int deleteOldAuditLogs(@Param("tenantId") String tenantId,
      @Param("beforeDate") LocalDateTime beforeDate);
}