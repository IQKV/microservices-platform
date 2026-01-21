package com.iqscaffold.pipelineservice.activity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PipelineActivityRepository extends JpaRepository<PipelineActivity, Long> {

  Page<PipelineActivity> findByLeadId(Long leadId, Pageable pageable);

  @EntityGraph("PipelineActivity.basic")
  Page<PipelineActivity> findBasicByLeadId(Long leadId, Pageable pageable);

  List<PipelineActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);

  @EntityGraph("PipelineActivity.basic")
  List<PipelineActivity> findBasicByLeadIdOrderByCreatedAtDesc(Long leadId);

  @EntityGraph("PipelineActivity.basic")
  Optional<PipelineActivity> findBasicById(Long id);

  Page<PipelineActivity> findByActivityType(ActivityType activityType, Pageable pageable);

  @EntityGraph("PipelineActivity.basic")
  Page<PipelineActivity> findBasicByActivityType(ActivityType activityType, Pageable pageable);

  long countByLeadId(Long leadId);
}
