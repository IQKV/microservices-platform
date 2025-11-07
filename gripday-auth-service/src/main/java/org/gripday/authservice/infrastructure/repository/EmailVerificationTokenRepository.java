package org.gripday.authservice.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.gripday.authservice.infrastructure.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for EmailVerificationToken entity operations. Provides data access methods for email verification token management.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

  /**
   * Find an unused verification token by token string.
   *
   * @param token the token string to search for
   * @return Optional containing the token if found and unused
   */
  Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);

  /**
   * Find all unused verification tokens for a specific user.
   *
   * @param userId the user ID to search for
   * @return List of unused tokens for the user
   */
  List<EmailVerificationToken> findByUserIdAndUsedFalse(Long userId);

  /**
   * Find all verification tokens for a specific user within a tenant.
   *
   * @param userId   the user ID to search for
   * @param tenantId the tenant identifier
   * @return List of tokens for the user in the tenant
   */
  List<EmailVerificationToken> findByUserIdAndTenantId(Long userId, String tenantId);

  /**
   * Find unused verification tokens for a specific user within a tenant.
   *
   * @param userId   the user ID to search for
   * @param tenantId the tenant identifier
   * @return List of unused tokens for the user in the tenant
   */
  @Query("""
      SELECT evt FROM EmailVerificationToken evt 
      WHERE evt.userId = :userId 
        AND evt.tenantId = :tenantId 
        AND evt.used = false
      ORDER BY evt.createdAt DESC
      """)
  List<EmailVerificationToken> findUnusedTokensByUserIdAndTenantId(@Param("userId") Long userId,
      @Param("tenantId") String tenantId);

  /**
   * Find the most recent unused token for a user within a tenant.
   *
   * @param userId   the user ID to search for
   * @param tenantId the tenant identifier
   * @return Optional containing the most recent unused token
   */
  @Query("""
      SELECT evt FROM EmailVerificationToken evt 
      WHERE evt.userId = :userId 
        AND evt.tenantId = :tenantId 
        AND evt.used = false
      ORDER BY evt.createdAt DESC
      LIMIT 1
      """)
  Optional<EmailVerificationToken> findMostRecentUnusedTokenByUserIdAndTenantId(@Param("userId") Long userId,
      @Param("tenantId") String tenantId);

  /**
   * Delete all tokens that have expired before the specified date. Used for cleanup of old verification tokens.
   *
   * @param dateTime the cutoff date for deletion
   * @return number of deleted tokens
   */
  @Modifying
  @Query("""
      DELETE FROM EmailVerificationToken evt 
      WHERE evt.expiresAt < :dateTime
      """)
  int deleteByExpiresAtBefore(@Param("dateTime") LocalDateTime dateTime);

  /**
   * Mark all unused tokens for a user as used. Used when invalidating existing tokens before generating new ones.
   *
   * @param userId   the user ID
   * @param tenantId the tenant identifier
   * @return number of updated tokens
   */
  @Modifying
  @Query("""
      UPDATE EmailVerificationToken evt 
      SET evt.used = true 
      WHERE evt.userId = :userId 
        AND evt.tenantId = :tenantId 
        AND evt.used = false
      """)
  int markAllUnusedTokensAsUsedByUserIdAndTenantId(@Param("userId") Long userId,
      @Param("tenantId") String tenantId);

  /**
   * Count unused tokens for a specific user within a tenant.
   *
   * @param userId   the user ID
   * @param tenantId the tenant identifier
   * @return number of unused tokens
   */
  @Query("""
      SELECT COUNT(evt) FROM EmailVerificationToken evt 
      WHERE evt.userId = :userId 
        AND evt.tenantId = :tenantId 
        AND evt.used = false
      """)
  long countUnusedTokensByUserIdAndTenantId(@Param("userId") Long userId,
      @Param("tenantId") String tenantId);

  /**
   * Count tokens created within a time period for a user (for rate limiting).
   *
   * @param userId   the user ID
   * @param tenantId the tenant identifier
   * @param since    the start time for counting
   * @return number of tokens created since the specified time
   */
  @Query("""
      SELECT COUNT(evt) FROM EmailVerificationToken evt 
      WHERE evt.userId = :userId 
        AND evt.tenantId = :tenantId 
        AND evt.createdAt >= :since
      """)
  long countTokensCreatedSince(@Param("userId") Long userId,
      @Param("tenantId") String tenantId,
      @Param("since") LocalDateTime since);

  /**
   * Find all expired tokens within a tenant.
   *
   * @param tenantId    the tenant identifier
   * @param currentTime the current time for comparison
   * @return List of expired tokens in the tenant
   */
  @Query("""
      SELECT evt FROM EmailVerificationToken evt 
      WHERE evt.tenantId = :tenantId 
        AND evt.expiresAt < :currentTime
      ORDER BY evt.expiresAt ASC
      """)
  List<EmailVerificationToken> findExpiredTokensByTenantId(@Param("tenantId") String tenantId,
      @Param("currentTime") LocalDateTime currentTime);

  /**
   * Check if a token exists and is valid (unused and not expired).
   *
   * @param token       the token string
   * @param currentTime the current time for expiration check
   * @return true if token exists and is valid
   */
  @Query("""
      SELECT COUNT(evt) > 0 FROM EmailVerificationToken evt 
      WHERE evt.token = :token 
        AND evt.used = false 
        AND evt.expiresAt > :currentTime
      """)
  boolean existsByTokenAndValidAt(@Param("token") String token,
      @Param("currentTime") LocalDateTime currentTime);

  /**
   * Count all tokens that have expired before the specified date. Used for monitoring expired tokens before cleanup.
   *
   * @param dateTime the cutoff date for counting
   * @return number of expired tokens
   */
  @Query("""
      SELECT COUNT(evt) FROM EmailVerificationToken evt 
      WHERE evt.expiresAt < :dateTime
      """)
  long countByExpiresAtBefore(@Param("dateTime") LocalDateTime dateTime);
}