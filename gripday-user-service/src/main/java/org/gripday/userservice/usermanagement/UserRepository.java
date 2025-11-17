package org.gripday.userservice.usermanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for User aggregate.
 * Part of User Management bounded context.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  @Query("""
      SELECT u FROM User u 
      WHERE u.username = :username 
         OR u.email = :email
      """)
  Optional<User> findByUsernameOrEmail(@Param("username") String username,
      @Param("email") String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  List<User> findByTenantId(String tenantId);

  @Query("""
      SELECT u FROM User u 
      WHERE u.tenantId = :tenantId 
        AND u.enabled = true
      ORDER BY u.createdAt DESC
      """)
  List<User> findEnabledUsersByTenantId(@Param("tenantId") String tenantId);

  @Query("""
      SELECT DISTINCT u FROM User u 
      JOIN u.authorities a 
      WHERE u.tenantId = :tenantId 
        AND a.name = :authorityName
      ORDER BY u.createdAt DESC
      """)
  List<User> findByTenantIdAndAuthorityName(@Param("tenantId") String tenantId,
      @Param("authorityName") String authorityName);

  long countByTenantId(String tenantId);

  @Query("""
      SELECT COUNT(u) FROM User u 
      WHERE u.tenantId = :tenantId 
        AND u.enabled = true
      """)
  long countEnabledUsersByTenantId(@Param("tenantId") String tenantId);

  long countByTenantIdAndEnabledTrue(String tenantId);

  @Query("""
      SELECT u FROM User u 
      WHERE u.tenantId = :tenantId 
        AND u.emailVerified = false 
        AND u.enabled = true
      ORDER BY u.createdAt ASC
      """)
  List<User> findUnverifiedUsersByTenantId(@Param("tenantId") String tenantId);
}
