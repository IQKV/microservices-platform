package org.gripday.userservice.usermanagement;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for UserPreference entity operations.
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

  Optional<UserPreference> findByUserId(Long userId);

  boolean existsByUserId(Long userId);

  void deleteByUserId(Long userId);
}
