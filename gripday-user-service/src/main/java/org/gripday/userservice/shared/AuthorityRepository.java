package org.gripday.userservice.shared;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.gripday.userservice.shared.Authority;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Authority entity operations. Provides data access methods for role-based access control.
 */
@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {

  /**
   * Find authority by name. Cached since authorities rarely change.
   *
   * @param name the authority name to search for
   * @return Optional containing the authority if found
   */
  @Cacheable(value = "authorities", key = "#name")
  Optional<Authority> findByName(String name);

  /**
   * Find all authorities for a specific user.
   *
   * @param userId the user ID to search authorities for
   * @return List of authorities assigned to the user
   */
  @Query("""
      SELECT a FROM Authority a 
      JOIN a.users u 
      WHERE u.id = :userId
      ORDER BY a.name ASC
      """)
  List<Authority> findByUserId(@Param("userId") Long userId);

  /**
   * Find authorities by names. Cached with composite key based on authority names.
   *
   * @param names the set of authority names to search for
   * @return List of authorities matching the names
   */
  @Cacheable(value = "authorities", key = "#names.toString()")
  @Query("""
      SELECT a FROM Authority a 
      WHERE a.name IN :names
      ORDER BY a.name ASC
      """)
  List<Authority> findByNameIn(@Param("names") Set<String> names);

  /**
   * Check if authority name exists.
   *
   * @param name the authority name to check
   * @return true if authority exists, false otherwise
   */
  boolean existsByName(String name);

  /**
   * Find all authorities ordered by name. Cached since authorities are relatively static.
   *
   * @return List of all authorities sorted by name
   */
  @Cacheable(value = "authorities", key = "'all_authorities'")
  @Query("""
      SELECT a FROM Authority a 
      ORDER BY a.name ASC
      """)
  List<Authority> findAllOrderByName();

  /**
   * Count users with a specific authority.
   *
   * @param authorityName the authority name
   * @return number of users with the authority
   */
  @Query("""
      SELECT COUNT(u) FROM Authority a 
      JOIN a.users u 
      WHERE a.name = :authorityName
      """)
  long countUsersByAuthorityName(@Param("authorityName") String authorityName);

  /**
   * Find authorities that have no users assigned.
   *
   * @return List of authorities with no users
   */
  @Query("""
      SELECT a FROM Authority a 
      WHERE a.users IS EMPTY
      ORDER BY a.name ASC
      """)
  List<Authority> findAuthoritiesWithNoUsers();

  /**
   * Find default user authorities (typically USER role).
   *
   * @return List of default authorities for new users
   */
  @Query("""
      SELECT a FROM Authority a 
      WHERE a.name IN ('USER', 'BASIC_USER', 'STANDARD_USER')
      ORDER BY a.name ASC
      """)
  List<Authority> findDefaultUserAuthorities();
}