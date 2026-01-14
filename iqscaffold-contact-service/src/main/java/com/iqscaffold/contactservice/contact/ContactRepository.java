package com.iqscaffold.contactservice.contact;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

  Optional<Contact> findByEmail(String email);

  List<Contact> findByCompanyId(Long companyId);

  List<Contact> findByStatus(ContactStatus status);

  Page<Contact> findByStatus(ContactStatus status, Pageable pageable);

  @Query("SELECT c FROM Contact c WHERE " +
         "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
         "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
         "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  Page<Contact> searchContacts(@Param("searchTerm") String searchTerm, Pageable pageable);

  @Query("SELECT c FROM Contact c WHERE c.leadScore >= :minScore")
  List<Contact> findByLeadScoreGreaterThanEqual(@Param("minScore") Integer minScore);

  boolean existsByEmail(String email);

  long countByStatus(ContactStatus status);
}
