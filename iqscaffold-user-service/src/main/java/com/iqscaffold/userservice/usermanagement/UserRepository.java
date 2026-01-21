package com.iqscaffold.userservice.usermanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
   * Find user by username with authorities loaded for authentication.
   *
   * @param username the username to search for
   * @return Optional containing the user with authorities if found
   */
  @EntityGraph("user-with-authorities")
  @Query("SELECT u FROM User u WHERE u.username = :username")
  Optional<User> findByUsernameWithAuthorities(@Param("username") String username);

  /**
   * Find user by email address with authorities loaded for authentication.
   *
   * @param email the email to search for
   * @return Optional containing the user with authorities if found
   */
  @EntityGraph("user-with-authorities")
  @Query("SELECT u FROM User u WHERE u.email = :email")
  Optional<User> findByEmailWithAuthorities(@Param("email") String email);

  /**
   * Find user by username or email with complete profile (authorities and preferences).
   *
   * @param username the username to search for
   * @param email    the email to search for
   * @return Optional containing the user with complete profile if found
   */
  @EntityGraph("user-complete")
  @Query("""
      SELECT u FROM User u 
      WHERE u.username = :username 
         OR u.email = :email
      """)
  Optional<User> findByUsernameOrEmailWithComplete(@Param("username") String username,
                                                   @Param("email") String email);

  /**
   * Find user by ID with preferences loaded for profile operations.
   *
   * @param id the user ID to search for
   * @return Optional containing the user with preferences if found
   */
  @EntityGraph("user-with-preferences")
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findByIdWithPreferences(@Param("id") Long id);

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
   * Find users by authority name with authorities loaded.
   */
  @EntityGraph("user-with-authorities")
  @Query("""
      SELECT DISTINCT u FROM User u 
      JOIN u.authorities a 
      WHERE a.name = :authorityName
      ORDER BY u.createdAt DESC
      """)
  List<User> findByAuthorityNameWithAuthorities(@Param("authorityName") String authorityName);

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
   * Find users excluding those with a specific authority (for ADMIN filtering out SUPER_ADMIN).
   */
  @Query("""
      SELECT DISTINCT u FROM User u 
      WHERE u.id NOT IN (
        SELECT u2.id FROM User u2 
        JOIN u2.authorities a 
        WHERE a.name = :excludedAuthorityName
      )
      ORDER BY u.createdAt DESC
      """)
  Page<User> findByAuthoritiesNameNot(@Param("excludedAuthorityName") String excludedAuthorityName, Pageable pageable);

  /**
   * Count users in current tenant schema.
   */
  long count();

  /**
   * Count enabled users in current tenant schema.
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

  /**
   * Find users who have any of the specified authorities.
   * Useful for finding users with CRM access or admin privileges.
   *
   * @param authorityNames list of authority names to search for
   * @return list of users who have at least one of the specified authorities
   */
  @Query("""
      SELECT DISTINCT u FROM User u 
      JOIN u.authorities a 
      WHERE a.name IN :authorityNames
      ORDER BY u.createdAt DESC
      """)
  List<User> findUsersWithAnyAuthority(@Param("authorityNames") List<String> authorityNames);

}
