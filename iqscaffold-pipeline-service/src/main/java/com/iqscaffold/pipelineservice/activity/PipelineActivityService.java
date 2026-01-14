package com.iqscaffold.pipelineservice.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PipelineActivityService {

  PipelineActivity logActivity(PipelineActivity activity);

  Optional<PipelineActivity> getActivityById(Long id);

  Page<PipelineActivity> getAllActivities(Pageable pageable);

  Page<PipelineActivity> getActivitiesByLeadId(Long leadId, Pageable pageable);

  List<PipelineActivity> getActivitiesByLeadId(Long leadId);

  Page<PipelineActivity> getActivitiesByType(ActivityType activityType, Pageable pageable);

  long countByLeadId(Long leadId);
}
