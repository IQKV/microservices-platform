package com.iqscaffold.pipelineservice.activity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PipelineActivityEntityGraphService providing optimized data fetching
 * using entity graphs for different use case scenarios.
 */
@Service
@Transactional(readOnly = true)
public class PipelineActivityEntityGraphServiceImpl implements PipelineActivityEntityGraphService {

  private final PipelineActivityRepository pipelineActivityRepository;

  public PipelineActivityEntityGraphServiceImpl(final PipelineActivityRepository pipelineActivityRepository) {
    this.pipelineActivityRepository = pipelineActivityRepository;
  }

  @Override
  public Optional<PipelineActivity> getBasicPipelineActivity(final Long id) {
    return pipelineActivityRepository.findBasicById(id);
  }

  @Override
  public Page<PipelineActivity> getBasicPipelineActivitiesByLead(final Long leadId, final Pageable pageable) {
    return pipelineActivityRepository.findBasicByLeadId(leadId, pageable);
  }

  @Override
  public List<PipelineActivity> getAllBasicPipelineActivitiesByLead(final Long leadId) {
    return pipelineActivityRepository.findBasicByLeadIdOrderByCreatedAtDesc(leadId);
  }

  @Override
  public Page<PipelineActivity> getBasicPipelineActivitiesByType(final ActivityType activityType, final Pageable pageable) {
    return pipelineActivityRepository.findBasicByActivityType(activityType, pageable);
  }
}