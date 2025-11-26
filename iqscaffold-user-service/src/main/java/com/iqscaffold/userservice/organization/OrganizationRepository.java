package com.iqscaffold.userservice.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Organization entity operations.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByName(String name);

  List<Organization> findByEnabledTrue();

  @Query("""
      SELECT o FROM Organization o 
      WHERE o.enabled = true
      ORDER BY o.createdAt DESC
      """)
  List<Organization> findEnabledOrderByCreatedAtDesc();

  boolean existsByName(String name);

  long countByEnabledTrue();
}
