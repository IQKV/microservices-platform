package com.iqscaffold.pipelineservice.followup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for optimized follow-up data fetching using entity graphs.
 * Provides methods for different fetching strategies based on use case requirements.
 */
public interface FollowUpEntityGraphService {

  /**
   * Get basic follow-up using entity graph optimization.
   * Optimized for scenarios where only basic follow-up data is needed.
   *
   * @param id the follow-up ID
   * @return basic follow-up, or empty if not found
   */
  Optional<FollowUp> getBasicFollowUp(Long id);

  /**
   * Get basic follow-ups for a lead using entity graph optimization.
   * Perfect for lead detail views showing follow-up tasks.
   *
   * @param leadId   the lead ID
   * @param pageable pagination information
   * @return page of basic follow-ups
   */
  Page<FollowUp> getBasicFollowUpsByLead(Long leadId, Pageable pageable);

  /**
   * Get all basic follow-ups for a lead using entity graph optimization.
   * Useful for complete follow-up lists and reporting.
   *
   * @param leadId the lead ID
   * @return list of basic follow-ups
   */
  List<FollowUp> getAllBasicFollowUpsByLead(Long leadId);

  /**
   * Get basic follow-ups by status using entity graph optimization.
   * Ideal for status-based follow-up management views.
   *
   * @param status   the follow-up status
   * @param pageable pagination information
   * @return page of basic follow-ups with the specified status
   */
  Page<FollowUp> getBasicFollowUpsByStatus(FollowUpStatus status, Pageable pageable);

  /**
   * Get basic follow-ups by status and date range using entity graph optimization.
   * Perfect for time-based follow-up reporting and scheduling.
   *
   * @param status    the follow-up status
   * @param startDate the start date
   * @param endDate   the end date
   * @return list of basic follow-ups within the date range
   */
  List<FollowUp> getBasicFollowUpsByStatusAndDateRange(FollowUpStatus status, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get basic overdue follow-ups using entity graph optimization.
   * Essential for overdue task management and notifications.
   *
   * @param status      the follow-up status (typically PENDING)
   * @param currentDate the current date to compare against
   * @return list of basic overdue follow-ups
   */
  List<FollowUp> getBasicOverdueFollowUps(FollowUpStatus status, LocalDateTime currentDate);
}
