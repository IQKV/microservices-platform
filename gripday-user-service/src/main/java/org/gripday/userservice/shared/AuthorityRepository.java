package org.gripday.userservice.shared;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Authority entity.
 * Part of the shared kernel.
 */
@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {

  Optional<Authority> findByName(String name);

  boolean existsByName(String name);
}
