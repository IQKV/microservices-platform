package org.gripday.userservice.security;

import java.time.Instant;
import java.util.List;

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

  @Query("""
      SELECT al FROM UserAuditLog al 
      ORDER BY al.createdAt DESC
      """)
  Page<UserAuditLog> findAllOrderByCreatedAtDesc(Pageable pageable);

  /**
   * Find audit logs by action type.
   *
   * @param action   the action type to search for
   * @param pageable pagination information
   * @return Page of audit logs with the specified action
   */
  Page<UserAuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);


  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.createdAt >= :startDate 
        AND al.createdAt <= :endDate
      ORDER BY al.createdAt DESC
      """)
  Page<UserAuditLog> findByDateRange(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate,
                                     Pageable pageable);

  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE (al.action LIKE 'LOGIN_%' 
          OR al.action LIKE 'LOGOUT_%' 
          OR al.action LIKE 'PASSWORD_%'
          OR al.action LIKE 'ACCOUNT_%')
      ORDER BY al.createdAt DESC
      """)
  Page<UserAuditLog> findSecurityAuditLogs(Pageable pageable);

  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.userId = :userId 
        AND al.action = 'LOGIN_FAILURE'
        AND al.createdAt >= :since
      ORDER BY al.createdAt DESC
      """)
  List<UserAuditLog> findFailedLoginAttempts(@Param("userId") Long userId,
                                             @Param("since") Instant since);

  long countByAction(String action);

  @Query("""
      SELECT al FROM UserAuditLog al 
      WHERE al.userId = :userId 
      ORDER BY al.createdAt DESC
      """)
  Page<UserAuditLog> findRecentAuditLogs(@Param("userId") Long userId, Pageable pageable);

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

  @org.springframework.data.jpa.repository.Modifying
  @Query("""
      DELETE FROM UserAuditLog al 
      WHERE al.createdAt < :beforeDate
      """)
  int deleteOldAuditLogs(@Param("beforeDate") Instant beforeDate);
}
