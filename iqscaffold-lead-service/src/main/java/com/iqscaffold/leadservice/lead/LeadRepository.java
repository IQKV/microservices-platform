package com.iqscaffold.leadservice.lead;

import java.time.LocalDateTime;
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
public interface LeadRepository extends JpaRepository<Lead, Long> {

  // Entity Graph Methods - Optimized fetching strategies
  
  /**
   * Find lead by ID with notes loaded eagerly.
   * Use this when you need to display lead details with note history.
   */
  @EntityGraph("Lead.withNotes")
  Optional<Lead> findWithNotesById(Long id);

  /**
   * Find lead by ID with activities loaded eagerly.
   * Use this when you need to display lead timeline/audit trail.
   */
  @EntityGraph("Lead.withActivities")
  Optional<Lead> findWithActivitiesById(Long id);

  /**
   * Find lead by ID with both notes and activities loaded eagerly.
   * Use this for comprehensive lead view with complete history.
   */
  @EntityGraph("Lead.withNotesAndActivities")
  Optional<Lead> findWithNotesAndActivitiesById(Long id);

  /**
   * Find lead by email with notes loaded eagerly.
   * Useful for lead lookup with note context.
   */
  @EntityGraph("Lead.withNotes")
  Optional<Lead> findWithNotesByEmail(String email);

  /**
   * Find leads by status with notes loaded eagerly.
   * Useful for status-based lead management with note context.
   */
  @EntityGraph("Lead.withNotes")
  List<Lead> findWithNotesByStatus(LeadStatus status);

  /**
   * Find leads by assigned user with basic entity graph.
   * Optimized for lead listing without heavy collections.
   */
  @EntityGraph("Lead.basic")
  Page<Lead> findBasicByAssignedTo(String assignedTo, Pageable pageable);

  // Standard Repository Methods (without entity graphs for basic operations)

  Optional<Lead> findByEmail(String email);

  List<Lead> findBySource(String source);

  List<Lead> findByStatus(LeadStatus status);

  Page<Lead> findByStatus(LeadStatus status, Pageable pageable);

  List<Lead> findByAssignedTo(String assignedTo);

  Page<Lead> findByAssignedTo(String assignedTo, Pageable pageable);

  @Query("SELECT l FROM Lead l WHERE "
         + "LOWER(l.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.company) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  Page<Lead> searchLeads(@Param("searchTerm") String searchTerm, Pageable pageable);

  @Query("SELECT l FROM Lead l WHERE l.score >= :minScore")
  List<Lead> findByScoreGreaterThanEqual(@Param("minScore") Integer minScore);

  @Query("SELECT l FROM Lead l WHERE l.qualified = true AND l.status = :status")
  List<Lead> findQualifiedLeadsByStatus(@Param("status") LeadStatus status);

  @Query("SELECT l FROM Lead l WHERE l.createdAt BETWEEN :startDate AND :endDate")
  List<Lead> findLeadsCreatedBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  boolean existsByEmail(String email);

  long countByStatus(LeadStatus status);

  long countBySource(String source);

  long countByQualified(Boolean qualified);

  @Query("SELECT COUNT(l) FROM Lead l WHERE l.createdAt >= :date")
  long countLeadsCreatedSince(@Param("date") LocalDateTime date);

  @Query("SELECT l FROM Lead l WHERE "
         + "(:searchTerm IS NULL OR "
         + "LOWER(l.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
         + "AND (:source IS NULL OR l.source = :source) "
         + "AND (:status IS NULL OR l.status = :status) "
         + "AND (:assignedTo IS NULL OR l.assignedTo = :assignedTo)")
  @EntityGraph("Lead.basic")
  Page<Lead> findLeadsWithFilters(
      @Param("searchTerm") String searchTerm,
      @Param("source") String source,
      @Param("status") LeadStatus status,
      @Param("assignedTo") String assignedTo,
      Pageable pageable
  );

  /**
   * Advanced search with notes loaded for detailed view.
   * Use when search results need note context.
   */
  @Query("SELECT l FROM Lead l WHERE "
         + "(:searchTerm IS NULL OR "
         + "LOWER(l.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
         + "LOWER(l.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
         + "AND (:source IS NULL OR l.source = :source) "
         + "AND (:status IS NULL OR l.status = :status) "
         + "AND (:assignedTo IS NULL OR l.assignedTo = :assignedTo)")
  @EntityGraph("Lead.withNotes")
  Page<Lead> findLeadsWithFiltersAndNotes(
      @Param("searchTerm") String searchTerm,
      @Param("source") String source,
      @Param("status") LeadStatus status,
      @Param("assignedTo") String assignedTo,
      Pageable pageable
  );

  @Query("SELECT l.source as source, COUNT(l) as count FROM Lead l WHERE "
         + "(:startDate IS NULL OR l.createdAt >= :startDate) "
         + "AND (:endDate IS NULL OR l.createdAt <= :endDate) "
         + "GROUP BY l.source")
  List<LeadSourceCount> countLeadsBySource(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Projection interface for lead source counts.
   */
  interface LeadSourceCount {
    String getSource();

    Long getCount();
  }
}
