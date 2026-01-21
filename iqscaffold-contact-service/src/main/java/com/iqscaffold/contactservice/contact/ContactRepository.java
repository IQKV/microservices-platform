package com.iqscaffold.contactservice.contact;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

  // Entity Graph Methods for optimized queries

  /**
   * Find contact by ID with company details loaded.
   * Use this when you need contact and company information together.
   *
   * @param id the contact ID
   * @return Optional containing contact with company if found
   */
  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE c.id = :id")
  Optional<Contact> findByIdWithCompany(@Param("id") Long id);

  /**
   * Find contacts by company ID with company details loaded.
   * Use this when displaying contacts for a specific company.
   *
   * @param companyId the company ID
   * @return List of contacts with company details loaded
   */
  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE c.companyId = :companyId")
  List<Contact> findByCompanyIdWithCompany(@Param("companyId") Long companyId);

  /**
   * Find contacts by status with company details loaded.
   * Use this when filtering contacts by status and need company information.
   *
   * @param status the contact status
   * @param pageable pagination information
   * @return Page of contacts with company details loaded
   */
  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE c.status = :status")
  Page<Contact> findByStatusWithCompany(@Param("status") ContactStatus status, Pageable pageable);

  /**
   * Search contacts with company details loaded.
   * Use this when searching contacts and need company information.
   *
   * @param searchTerm the search term
   * @param pageable pagination information
   * @return Page of contacts with company details loaded
   */
  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE " +
         "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
         "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
         "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  Page<Contact> searchContactsWithCompany(@Param("searchTerm") String searchTerm, Pageable pageable);

  // Batch Query Methods for bulk operations

  /**
   * Find multiple contacts by IDs in a single query.
   * Use this for bulk operations to avoid N+1 query problems.
   *
   * @param ids list of contact IDs
   * @return List of contacts
   */
  @Query("SELECT c FROM Contact c WHERE c.id IN :ids")
  List<Contact> findAllByIdIn(@Param("ids") List<Long> ids);

  /**
   * Find multiple contacts by IDs with company details loaded.
   * Use this for bulk operations that need company information.
   *
   * @param ids list of contact IDs
   * @return List of contacts with company details loaded
   */
  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE c.id IN :ids")
  List<Contact> findAllByIdInWithCompany(@Param("ids") List<Long> ids);

  /**
   * Find contacts by lead score range with company details loaded.
   * Use this for lead scoring operations that need company context.
   *
   * @param minScore minimum lead score
   * @return List of contacts with company details loaded
   */
  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE c.leadScore >= :minScore")
  List<Contact> findByLeadScoreGreaterThanEqualWithCompany(@Param("minScore") Integer minScore);
}