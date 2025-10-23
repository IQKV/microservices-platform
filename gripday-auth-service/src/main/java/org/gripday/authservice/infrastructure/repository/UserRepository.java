package org.gripday.authservice.infrastructure.repository;

import org.gripday.authservice.infrastructure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides data access methods for user authentication and management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email address.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email address.
     * Useful for authentication where users can login with either credential.
     * 
     * @param username the username to search for
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.username = :username 
           OR u.email = :email
        """)
    Optional<User> findByUsernameOrEmail(@Param("username") String username, 
                                        @Param("email") String email);

    /**
     * Check if username already exists.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if email already exists.
     * 
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all users within a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return List of users in the tenant
     */
    List<User> findByTenantId(String tenantId);

    /**
     * Find enabled users within a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return List of enabled users in the tenant
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
          AND u.enabled = true
        ORDER BY u.createdAt DESC
        """)
    List<User> findEnabledUsersByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find users by tenant and authority name.
     * 
     * @param tenantId the tenant identifier
     * @param authorityName the authority name to filter by
     * @return List of users with the specified authority in the tenant
     */
    @Query("""
        SELECT DISTINCT u FROM User u 
        JOIN u.authorities a 
        WHERE u.tenantId = :tenantId 
          AND a.name = :authorityName
        ORDER BY u.createdAt DESC
        """)
    List<User> findByTenantIdAndAuthorityName(@Param("tenantId") String tenantId, 
                                             @Param("authorityName") String authorityName);

    /**
     * Count users in a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return number of users in the tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count enabled users in a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return number of enabled users in the tenant
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE u.tenantId = :tenantId 
          AND u.enabled = true
        """)
    long countEnabledUsersByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count enabled users in a specific tenant (alternative method name).
     * 
     * @param tenantId the tenant identifier
     * @return number of enabled users in the tenant
     */
    long countByTenantIdAndEnabledTrue(String tenantId);

    /**
     * Find users with unverified emails in a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return List of users with unverified emails
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.tenantId = :tenantId 
          AND u.emailVerified = false 
          AND u.enabled = true
        ORDER BY u.createdAt ASC
        """)
    List<User> findUnverifiedUsersByTenantId(@Param("tenantId") String tenantId);
}