package com.iqscaffold.pipelineservice.followup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of FollowUpEntityGraphService providing optimized data fetching
 * using entity graphs for different use case scenarios.
 */
@Service
@Transactional(readOnly = true)
public class FollowUpEntityGraphServiceImpl implements FollowUpEntityGraphService {

  private final FollowUpRepository followUpRepository;

  public FollowUpEntityGraphServiceImpl(final FollowUpRepository followUpRepository) {
    this.followUpRepository = followUpRepository;
  }

  @Override
  public Optional<FollowUp> getBasicFollowUp(final Long id) {
    return followUpRepository.findBasicById(id);
  }

  @Override
  public Page<FollowUp> getBasicFollowUpsByLead(final Long leadId, final Pageable pageable) {
    return followUpRepository.findBasicByLeadId(leadId, pageable);
  }

  @Override
  public List<FollowUp> getAllBasicFollowUpsByLead(final Long leadId) {
    return followUpRepository.findBasicByLeadId(leadId);
  }

  @Override
  public Page<FollowUp> getBasicFollowUpsByStatus(final FollowUpStatus status, final Pageable pageable) {
    return followUpRepository.findBasicByStatus(status, pageable);
  }

  @Override
  public List<FollowUp> getBasicFollowUpsByStatusAndDateRange(final FollowUpStatus status, final LocalDateTime startDate, final LocalDateTime endDate) {
    return followUpRepository.findBasicByStatusAndDueDateBetween(status, startDate, endDate);
  }

  @Override
  public List<FollowUp> getBasicOverdueFollowUps(final FollowUpStatus status, final LocalDateTime currentDate) {
    return followUpRepository.findBasicOverdueFollowUps(status, currentDate);
  }
}