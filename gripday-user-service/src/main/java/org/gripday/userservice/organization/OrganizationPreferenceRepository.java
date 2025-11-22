package org.gripday.userservice.organization;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for OrganizationPreference entity operations.
 */
@Repository
public interface OrganizationPreferenceRepository extends JpaRepository<OrganizationPreference, Long> {

  Optional<OrganizationPreference> findByOrganizationId(Long organizationId);

  boolean existsByOrganizationId(Long organizationId);

  void deleteByOrganizationId(Long organizationId);
}
