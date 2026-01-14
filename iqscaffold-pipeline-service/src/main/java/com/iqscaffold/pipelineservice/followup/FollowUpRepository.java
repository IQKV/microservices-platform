package com.iqscaffold.pipelineservice.followup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

  Page<FollowUp> findByLeadId(Long leadId, Pageable pageable);

  List<FollowUp> findByLeadId(Long leadId);

  Page<FollowUp> findByStatus(FollowUpStatus status, Pageable pageable);

  @Query("SELECT f FROM FollowUp f WHERE f.status = :status AND f.dueDate BETWEEN :startDate AND :endDate")
  List<FollowUp> findByStatusAndDueDateBetween(
      @Param("status") FollowUpStatus status,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT f FROM FollowUp f WHERE f.status = :status AND f.dueDate < :currentDate")
  List<FollowUp> findOverdueFollowUps(
      @Param("status") FollowUpStatus status,
      @Param("currentDate") LocalDateTime currentDate
  );

  long countByStatusAndDueDateBetween(FollowUpStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
