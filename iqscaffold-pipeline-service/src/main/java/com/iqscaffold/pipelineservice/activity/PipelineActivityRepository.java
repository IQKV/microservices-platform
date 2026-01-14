package com.iqscaffold.pipelineservice.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineActivityRepository extends JpaRepository<PipelineActivity, Long> {

  Page<PipelineActivity> findByLeadId(Long leadId, Pageable pageable);

  List<PipelineActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);

  Page<PipelineActivity> findByActivityType(ActivityType activityType, Pageable pageable);

  long countByLeadId(Long leadId);
}
