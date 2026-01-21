package com.iqscaffold.pipelineservice.activity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for optimized pipeline activity data fetching using entity graphs.
 * Provides methods for different fetching strategies based on use case requirements.
 */
public interface PipelineActivityEntityGraphService {

  /**
   * Get basic pipeline activity using entity graph optimization.
   * Optimized for scenarios where only basic activity data is needed.
   *
   * @param id the activity ID
   * @return basic pipeline activity, or empty if not found
   */
  Optional<PipelineActivity> getBasicPipelineActivity(Long id);

  /**
   * Get basic pipeline activities for a lead using entity graph optimization.
   * Perfect for lead detail views showing activity history.
   *
   * @param leadId the lead ID
   * @param pageable pagination information
   * @return page of basic pipeline activities
   */
  Page<PipelineActivity> getBasicPipelineActivitiesByLead(Long leadId, Pageable pageable);

  /**
   * Get all basic pipeline activities for a lead ordered by creation date using entity graph optimization.
   * Ideal for complete activity timelines and audit trails.
   *
   * @param leadId the lead ID
   * @return list of basic pipeline activities ordered by creation date descending
   */
  List<PipelineActivity> getAllBasicPipelineActivitiesByLead(Long leadId);

  /**
   * Get basic pipeline activities by type using entity graph optimization.
   * Useful for activity type filtering and reporting.
   *
   * @param activityType the activity type
   * @param pageable pagination information
   * @return page of basic pipeline activities of the specified type
   */
  Page<PipelineActivity> getBasicPipelineActivitiesByType(ActivityType activityType, Pageable pageable);
}