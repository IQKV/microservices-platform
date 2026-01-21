package com.iqscaffold.pipelineservice.followup;

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
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

  Page<FollowUp> findByLeadId(Long leadId, Pageable pageable);

  @EntityGraph("FollowUp.basic")
  Page<FollowUp> findBasicByLeadId(Long leadId, Pageable pageable);

  List<FollowUp> findByLeadId(Long leadId);

  @EntityGraph("FollowUp.basic")
  List<FollowUp> findBasicByLeadId(Long leadId);

  @EntityGraph("FollowUp.basic")
  Optional<FollowUp> findBasicById(Long id);

  Page<FollowUp> findByStatus(FollowUpStatus status, Pageable pageable);

  @EntityGraph("FollowUp.basic")
  Page<FollowUp> findBasicByStatus(FollowUpStatus status, Pageable pageable);

  @Query("SELECT f FROM FollowUp f WHERE f.status = :status AND f.dueDate BETWEEN :startDate AND :endDate")
  List<FollowUp> findByStatusAndDueDateBetween(
      @Param("status") FollowUpStatus status,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  @EntityGraph("FollowUp.basic")
  @Query("SELECT f FROM FollowUp f WHERE f.status = :status AND f.dueDate BETWEEN :startDate AND :endDate")
  List<FollowUp> findBasicByStatusAndDueDateBetween(
      @Param("status") FollowUpStatus status,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT f FROM FollowUp f WHERE f.status = :status AND f.dueDate < :currentDate")
  List<FollowUp> findOverdueFollowUps(
      @Param("status") FollowUpStatus status,
      @Param("currentDate") LocalDateTime currentDate
  );

  @EntityGraph("FollowUp.basic")
  @Query("SELECT f FROM FollowUp f WHERE f.status = :status AND f.dueDate < :currentDate")
  List<FollowUp> findBasicOverdueFollowUps(
      @Param("status") FollowUpStatus status,
      @Param("currentDate") LocalDateTime currentDate
  );

  long countByStatusAndDueDateBetween(FollowUpStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
