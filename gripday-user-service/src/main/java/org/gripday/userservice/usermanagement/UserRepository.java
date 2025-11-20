package org.gripday.userservice.usermanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for User entity operations. Provides data access methods for user authentication and management.
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
   * Find user by username or email address. Useful for authentication where users can login with either credential.
   *
   * @param username the username to search for
   * @param email    the email to search for
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
   * Find all users in current tenant schema.
   */
  List<User> findAll();

  /**
   * Find enabled users ordered by creation time.
   */
  @Query("""
      SELECT u FROM User u 
      WHERE u.enabled = true
      ORDER BY u.createdAt DESC
      """)
  List<User> findEnabledUsersOrderByCreatedAtDesc();

  /**
   * Find users by authority name.
   */
  @Query("""
      SELECT DISTINCT u FROM User u 
      JOIN u.authorities a 
      WHERE a.name = :authorityName
      ORDER BY u.createdAt DESC
      """)
  List<User> findByAuthorityName(@Param("authorityName") String authorityName);

  /**
   * Count users in current tenant schema.
   */
  long count();

  /**
   * Count enabled users in current tenant schema.
   */
  @Query("""
      SELECT COUNT(u) FROM User u 
      WHERE u.enabled = true
      """)
  long countEnabledUsers();

  /**
   * Count enabled users.
   */
  long countByEnabledTrue();

  /**
   * Find users with unverified emails.
   */
  @Query("""
      SELECT u FROM User u 
      WHERE u.emailVerified = false 
        AND u.enabled = true
      ORDER BY u.createdAt ASC
      """)
  List<User> findUnverifiedUsers();

  default List<User> findByTenantId(String tenantId) {
    return findAll();
  }

  default List<User> findEnabledUsersByTenantId(String tenantId) {
    return findEnabledUsersOrderByCreatedAtDesc();
  }

  default List<User> findByTenantIdAndAuthorityName(String tenantId, String authorityName) {
    return findByAuthorityName(authorityName);
  }

  default long countByTenantId(String tenantId) {
    return count();
  }

  default long countEnabledUsersByTenantId(String tenantId) {
    return countEnabledUsers();
  }

  default long countByTenantIdAndEnabledTrue(String tenantId) {
    return countByEnabledTrue();
  }

  default List<User> findUnverifiedUsersByTenantId(String tenantId) {
    return findUnverifiedUsers();
  }
}
