package com.iqscaffold.leadservice.lead;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

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
  Page<Lead> findLeadsWithFilters(
      @Param("searchTerm") String searchTerm,
      @Param("source") String source,
      @Param("status") LeadStatus status,
      @Param("assignedTo") String assignedTo,
      Pageable pageable
  );
}
