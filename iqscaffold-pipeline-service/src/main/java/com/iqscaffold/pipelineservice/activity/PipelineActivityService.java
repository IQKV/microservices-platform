package com.iqscaffold.pipelineservice.activity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PipelineActivityService {

  PipelineActivity logActivity(PipelineActivity activity);

  Optional<PipelineActivity> getActivityById(Long id);

  Page<PipelineActivity> getAllActivities(Pageable pageable);

  Page<PipelineActivity> getActivitiesByLeadId(Long leadId, Pageable pageable);

  List<PipelineActivity> getActivitiesByLeadId(Long leadId);

  Page<PipelineActivity> getActivitiesByType(ActivityType activityType, Pageable pageable);

  long countByLeadId(Long leadId);
}
